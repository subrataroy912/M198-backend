/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : AuthService
 * PURPOSE    : Handles account creation, credential-based login, JWT issuance,
 *              refresh token management, OAuth linking, and session lifecycle.
 *
 * This service is the core identity engine of the platform.
 * It manages user registration, token creation, login events, OAuth account setup,
 * and secure profile creation for newly registered users.
 */
package com.M198.Majorproject.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.dto.AuthResponse;
import com.M198.Majorproject.dto.LoginRequest;
import com.M198.Majorproject.dto.RegisterUserRequest;
import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.entity.identity.OAuthProvider;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import com.M198.Majorproject.entity.identity.RefreshToken;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.entity.identity.UserOAuth;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.repository.identity.RefreshTokenRepository;
import com.M198.Majorproject.repository.identity.UserOAuthRepository;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.M198.Majorproject.repository.identity.UserRepository;
import com.M198.Majorproject.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @org.springframework.beans.factory.annotation.Value("${app.cookies.secure:true}")
    private boolean secureCookies;

    @org.springframework.beans.factory.annotation.Value("${app.cookies.same-site:None}")
    private String cookieSameSite;

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserOAuthRepository oauthRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthResponse register(RegisterUserRequest request) {
        if (request.getAccountType() != AccountType.TEACHER && request.getAccountType() != AccountType.STUDENT) {
            throw new IllegalArgumentException("Public registration requires a teacher or student account");
        }
        String email = normalizeEmail(request.getEmail());
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getPasswordHash() == null || existingUser.getPasswordHash().isBlank()) {
                List<UserOAuth> oauths = oauthRepository.findAllByUserId(existingUser.getId());
                String providers = oauths.stream()
                        .map(o -> formatProviderName(o.getProvider()))
                        .distinct()
                        .collect(Collectors.joining(", "));
                String message = providers.isBlank()
                        ? "An account with this email already exists via social login. Please sign in using your social account."
                        : "An account with this email already exists via " + providers + ". Please sign in with " + providers + ".";
                throw new DuplicateKeyException(message);
            }
            throw new DuplicateKeyException("Email is already registered");
        }

        User user = userRepository.save(
                User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .accountType(request.getAccountType())
                        .status(AccountStatus.ACTIVE)
                        .active(true)
                        .verified(false)
                        .build());
        profileRepository.save(UserProfile.builder()
                .userId(user.getId())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .displayName((request.getFirstName().trim() + " " + request.getLastName().trim()).trim())
                .profileVisibility(ProfileVisibility.PRIVATE)
                .accountType(request.getAccountType())
                .canCreateCourses(false)
                .build());
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && (existingUser.getPasswordHash() == null || existingUser.getPasswordHash().isBlank())) {
            List<UserOAuth> oauths = oauthRepository.findAllByUserId(existingUser.getId());
            String providers = oauths.stream()
                    .map(o -> formatProviderName(o.getProvider()))
                    .distinct()
                    .collect(Collectors.joining(", "));
            String message = providers.isBlank()
                    ? "This account does not have a local password set. Please sign in using social login."
                    : "This account was registered with " + providers + ". Please sign in with " + providers + ".";
            throw new BadCredentialsException(message);
        }

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.getPassword()));
        User user = userRepository.findByEmailAndActiveTrueAndStatus(email, AccountStatus.ACTIVE)
                .orElseThrow(() -> new AuthenticationServiceException("Invalid credentials"));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        String userId = jwtService.parseAndValidate(refreshToken, "refresh").getSubject();
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(refreshToken))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new AuthenticationServiceException("Invalid refresh token"));
        if (!storedToken.getUserId().equals(userId)) {
            throw new AuthenticationServiceException("Invalid refresh token");
        }
        if (refreshTokenRepository.revokeIfActive(storedToken.getTokenHash(), Instant.now()) != 1) {
            throw new AuthenticationServiceException("Invalid refresh token");
        }
        User user = userRepository.findByIdAndActiveTrueAndStatus(userId, AccountStatus.ACTIVE)
                .orElseThrow(() -> new AuthenticationServiceException("Invalid refresh token"));
        return issueTokens(user);
    }

    public void logout(String authenticatedUserId, String refreshToken) {
        String tokenUserId = jwtService.parseAndValidate(refreshToken, "refresh").getSubject();
        if (!authenticatedUserId.equals(tokenUserId)) {
            throw new AuthenticationServiceException("Invalid refresh token");
        }
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(refreshToken)).ifPresent(storedToken -> {
            if (!authenticatedUserId.equals(storedToken.getUserId())) {
                throw new AuthenticationServiceException("Invalid refresh token");
            }
            storedToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(storedToken);
        });
    }

    public void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .sameSite(cookieSameSite) // 👈 Ensure this is set to "None" in your properties file!
                .secure(secureCookies) // 👈 Ensure this evaluates to true!
                .path("/") // 👈 Standardize path to root so all auth calls can see it
                .maxAge(java.time.Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .sameSite(cookieSameSite)
                .secure(secureCookies)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        ResponseCookie legacyCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .sameSite(cookieSameSite)
                .secure(secureCookies)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, legacyCookie.toString());
    }

    public void setCsrfCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", UUID.randomUUID().toString())
                .httpOnly(false)
                .sameSite(cookieSameSite)
                .secure(secureCookies)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public AuthResponse authenticateOAuth(
            OAuthProvider provider,
            String providerUserId,
            String email,
            boolean emailVerified,
            String displayName,
            String avatarUrl) {
        if (providerUserId == null || providerUserId.isBlank() || email == null || email.isBlank() || !emailVerified) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email"), "Provider email is missing or unverified");
        }

        String normalizedEmail = normalizeEmail(email);
        User user;
        var existingLink = oauthRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (existingLink.isPresent()) {
            user = userRepository.findById(existingLink.get().getUserId())
                    .orElseThrow(() -> new AuthenticationServiceException("OAuth account is unavailable"));
            if (!user.isActive() || user.getStatus() != AccountStatus.ACTIVE || !user.isVerified()) {
                user.setActive(true);
                user.setStatus(AccountStatus.ACTIVE);
                user.setVerified(true);
                userRepository.save(user);
            }
        } else {
            // Unified identity lookup: match account by verified email
            var existingUser = userRepository.findByEmail(normalizedEmail);
            if (existingUser.isPresent()) {
                user = existingUser.get();
                if (!user.isActive() || user.getStatus() != AccountStatus.ACTIVE || !user.isVerified()) {
                    user.setActive(true);
                    user.setStatus(AccountStatus.ACTIVE);
                    user.setVerified(true);
                    userRepository.save(user);
                }
            } else {
                user = createOAuthUser(normalizedEmail, displayName, avatarUrl);
            }

            if (oauthRepository.findByUserIdAndProvider(user.getId(), provider).isEmpty()) {
                try {
                    oauthRepository.save(UserOAuth.builder()
                            .userId(user.getId())
                            .provider(provider)
                            .providerUserId(providerUserId)
                            .build());
                } catch (DuplicateKeyException ignored) {
                    // Handled idempotently for concurrent requests
                }
            }
        }

        ensureProfileExists(user, displayName, avatarUrl);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    private User createOAuthUser(String email, String displayName, String avatarUrl) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.save(User.builder()
                .email(normalizedEmail)
                .accountType(AccountType.STUDENT)
                .status(AccountStatus.ACTIVE)
                .active(true)
                .verified(true)
                .build());
        String[] names = splitDisplayName(displayName, normalizedEmail);
        profileRepository.save(UserProfile.builder()
                .userId(user.getId())
                .firstName(names[0])
                .lastName(names[1])
                .displayName(displayName == null || displayName.isBlank() ? names[0] : displayName)
                .avatarUrl(avatarUrl)
                .profileVisibility(ProfileVisibility.PRIVATE)
                .accountType(AccountType.STUDENT)
                .canCreateCourses(false)
                .build());
        return user;
    }

    private void ensureProfileExists(User user, String displayName, String avatarUrl) {
        var existingProfile = profileRepository.findByUserId(user.getId());
        if (existingProfile.filter(profile -> profile.getDeletedAt() == null).isPresent()) {
            UserProfile profile = existingProfile.get();
            if ((profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) && avatarUrl != null && !avatarUrl.isBlank()) {
                profile.setAvatarUrl(avatarUrl);
                profileRepository.save(profile);
            }
            return;
        }
        if (existingProfile.isPresent()) {
            UserProfile profile = existingProfile.get();
            profile.setDeletedAt(null);
            if ((profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) && avatarUrl != null && !avatarUrl.isBlank()) {
                profile.setAvatarUrl(avatarUrl);
            }
            profileRepository.save(profile);
            return;
        }
        String[] names = splitDisplayName(displayName, user.getEmail());
        profileRepository.save(UserProfile.builder()
                .userId(user.getId())
                .firstName(names[0])
                .lastName(names[1])
                .displayName(displayName == null || displayName.isBlank() ? names[0] : displayName)
                .avatarUrl(avatarUrl)
                .profileVisibility(ProfileVisibility.PRIVATE)
                .build());
    }

    private String formatProviderName(OAuthProvider provider) {
        if (provider == null) {
            return "Social Login";
        }
        return switch (provider) {
            case GOOGLE -> "Google";
            case GITHUB -> "GitHub";
            case MICROSOFT -> "Microsoft";
        };
    }

    private AuthResponse issueTokens(User user) {
        UserProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        String refreshToken = jwtService.createRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hash(refreshToken))
                .userId(user.getId())
                .expiresAt(jwtService.refreshTokenExpiresAt())
                .build());
        return AuthResponse.builder()
                .accessToken(jwtService.createAccessToken(user.getId(), user.getAccountType().name()))
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(profile == null ? null : profile.getDisplayName())
                .avatarUrl(profile == null ? null : profile.getAvatarUrl())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String[] splitDisplayName(String displayName, String email) {
        String value = displayName == null || displayName.isBlank() ? email.substring(0, email.indexOf('@'))
                : displayName.trim();
        int separator = value.indexOf(' ');
        return separator < 0 ? new String[] { value, "" }
                : new String[] { value.substring(0, separator), value.substring(separator + 1).trim() };
    }

    private static class AuthenticationServiceException extends AuthenticationException {

        private static final long serialVersionUID = 1L;

        AuthenticationServiceException(String message) {
            super(message);
        }
    }
}
