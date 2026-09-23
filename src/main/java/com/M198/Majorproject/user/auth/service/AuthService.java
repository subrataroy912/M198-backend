/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : AuthService
 * PURPOSE    : Handles account creation, credential-based login, JWT issuance,
 * refresh token management, OAuth linking, and session lifecycle.
 * <p>
 * This service is the core identity engine of the platform.
 * It manages user registration, token creation, login events, OAuth account setup,
 * and secure profile creation for newly registered users.
 */
package com.M198.Majorproject.user.auth.service;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.M198.Majorproject.user.auth.exception.AuthConflictException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
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

import com.M198.Majorproject.user.auth.dto.AuthResponse;
import com.M198.Majorproject.user.auth.dto.LoginRequest;
import com.M198.Majorproject.user.auth.dto.RegisterUserRequest;
import com.M198.Majorproject.user.identity.entity.AccountStatus;
import com.M198.Majorproject.user.auth.entity.OAuthProvider;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.auth.entity.RefreshToken;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.auth.entity.UserOAuth;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.auth.repository.RefreshTokenRepository;
import com.M198.Majorproject.user.auth.repository.UserOAuthRepository;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;
import com.M198.Majorproject.user.identity.repository.UserRepository;
import com.M198.Majorproject.common.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthService.class);

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

    @Transactional
    public AuthResponse register(RegisterUserRequest request) {
        String email = requireValidEmail(request.getEmail());
        String rawPassword = requireNonBlank(request.getPassword(), "Password is required");
        String firstName = requireNonBlank(request.getFirstName(), "First name is required");
        String lastName = requireNonBlank(request.getLastName(), "Last name is required");

        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (isSocialOnlyAccount(existingUser)) {
                String providers = getFormattedProviders(existingUser.getId());
                String message = providers.isBlank()
                        ? "An account with this email already exists via social login. Please sign in using your social account."
                        : "An account with this email already exists via " + providers + ". Please sign in with "
                                + providers + ".";
                throw new DuplicateKeyException(message);
            }
            throw new DuplicateKeyException("Email is already registered");
        });

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(AccountStatus.ACTIVE)
                .active(true)
                .verified(false)
                .isAdmin(false)
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Race condition protection (unique constraint on email)
            throw new AuthConflictException("Email is already registered");
        }

        // 4. Create profile
        profileRepository.save(UserProfile.builder()
                .userId(user.getId())
                .firstName(firstName)
                .lastName(lastName)
                .displayName(firstName + " " + lastName)
                .profileVisibility(ProfileVisibility.PRIVATE)
                .isAdmin(false)
                .canCreateCourses(false)
                .profileCompleted(false)
                .build());

        AuthResponse response = issueTokens(user);
        response.setNewUser(true);
        response.setProfileCompleted(false);
        return response;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword();

        if (email.isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Social-only account (no local password)
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            String providers = getFormattedProviders(user.getId());
            String message = providers.isBlank()
                    ? "This account does not have a local password set. Please sign in using social login."
                    : "This account was registered with " + providers + ". Please sign in with " + providers + ".";

            throw new BadCredentialsException(message);
        }

        // Authenticate (will throw BadCredentialsException on failure)
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, rawPassword));

        // Re-fetch with active + status check (or apply filters earlier)
        if (!user.isActive() || user.getStatus() != AccountStatus.ACTIVE) {
            throw new AuthenticationServiceException("Account is not active");
        }

        // Check if user was away for a long time (>= 3 days)
        Instant prevLogin = user.getLastLoginAt();
        boolean isLongTimeAway = prevLogin != null
                && prevLogin.isBefore(Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS));

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        AuthResponse response = issueTokens(user);
        response.setLongTimeAway(isLongTimeAway);
        return response;
    }

    public AuthResponse refresh(String refreshToken) {
        String userId = jwtService.parseAndValidate(refreshToken, "refresh").getSubject();
        String tokenHash = hash(refreshToken);
        Optional<RefreshToken> storedTokenOpt = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()));

        if (storedTokenOpt.isEmpty()) {
            logger.warn(
                    "Potential refresh token reuse or stolen token detected for userId={}. Revoking all active sessions.",
                    userId);
            refreshTokenRepository.deleteAllByUserId(userId);
            throw new AuthenticationServiceException("Invalid refresh token");
        }

        RefreshToken storedToken = storedTokenOpt.get();
        if (!storedToken.getUserId().equals(userId)) {
            logger.warn("Refresh token userId mismatch for userId={}. Revoking all active sessions.", userId);
            refreshTokenRepository.deleteAllByUserId(userId);
            throw new AuthenticationServiceException("Invalid refresh token");
        }

        if (refreshTokenRepository.revokeIfActive(storedToken.getTokenHash(), Instant.now()) != 1) {
            logger.warn("Concurrent refresh token race/reuse detected for userId={}. Revoking all active sessions.",
                    userId);
            refreshTokenRepository.deleteAllByUserId(userId);
            throw new AuthenticationServiceException("Invalid refresh token");
        }

        refreshTokenRepository.deleteByTokenHash(tokenHash);

        User user = userRepository.findByIdAndActiveTrueAndStatus(userId, AccountStatus.ACTIVE)
                .orElseThrow(() -> new AuthenticationServiceException("Invalid refresh token"));
        return issueTokens(user);
    }

    public void logout(String authenticatedUserId, String refreshToken) {
        String tokenUserId = jwtService.parseAndValidate(refreshToken, "refresh").getSubject();
        if (!authenticatedUserId.equals(tokenUserId)) {
            throw new AuthenticationServiceException("Invalid refresh token");
        }
        String tokenHash = hash(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash).ifPresent(storedToken -> {
            if (!authenticatedUserId.equals(storedToken.getUserId())) {
                throw new AuthenticationServiceException("Invalid refresh token");
            }
            refreshTokenRepository.deleteByTokenHash(tokenHash);
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

    public void setCsrfCookie(@NonNull HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", UUID.randomUUID().toString())
                .httpOnly(false)
                .sameSite(cookieSameSite)
                .secure(secureCookies)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearCsrfCookie(@NonNull HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .sameSite(cookieSameSite)
                .secure(secureCookies)
                .path("/")
                .maxAge(0)
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
        boolean isFreshOAuthRegistration = false;
        var existingLink = oauthRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (existingLink.isPresent()) {
            user = userRepository.findById(existingLink.get().getUserId())
                    .orElseThrow(() -> new AuthenticationServiceException("OAuth account is unavailable"));
            if (user.getDeletedAt() != null) {
                throw new AuthenticationServiceException("OAuth account is unavailable");
            }
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
                if (user.getDeletedAt() != null) {
                    throw new AuthenticationServiceException("OAuth account is unavailable");
                }
                if (!user.isActive() || user.getStatus() != AccountStatus.ACTIVE || !user.isVerified()) {
                    user.setActive(true);
                    user.setStatus(AccountStatus.ACTIVE);
                    user.setVerified(true);
                    userRepository.save(user);
                }
            } else {
                user = createOAuthUser(normalizedEmail, displayName, avatarUrl);
                isFreshOAuthRegistration = true;
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

        Instant prevLogin = user.getLastLoginAt();
        boolean isLongTimeAway = prevLogin != null
                && prevLogin.isBefore(Instant.now().minus(3, java.time.temporal.ChronoUnit.DAYS));

        ensureProfileExists(user, displayName, avatarUrl);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        AuthResponse response = issueTokens(user);
        if (isFreshOAuthRegistration) {
            response.setNewUser(true);
            response.setProfileCompleted(false);
        }
        response.setLongTimeAway(isLongTimeAway);
        return response;
    }

    private User createOAuthUser(String email, String displayName, String avatarUrl) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.save(User.builder()
                .email(normalizedEmail)
                .status(AccountStatus.ACTIVE)
                .active(true)
                .verified(true)
                .isAdmin(false)
                .build());
        String[] names = splitDisplayName(displayName, normalizedEmail);
        profileRepository.save(UserProfile.builder()
                .userId(user.getId())
                .firstName(names[0])
                .lastName(names[1])
                .displayName(displayName == null || displayName.isBlank() ? names[0] : displayName)
                .avatarUrl(avatarUrl)
                .profileVisibility(ProfileVisibility.PRIVATE)
                .isAdmin(false)
                .canCreateCourses(false)
                .profileCompleted(false)
                .build());
        return user;
    }

    private void ensureProfileExists(User user, String displayName, String avatarUrl) {
        var existingProfile = profileRepository.findByUserId(user.getId());
        if (existingProfile.filter(profile -> profile.getDeletedAt() == null).isPresent()) {
            UserProfile profile = existingProfile.get();
            if ((profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) && avatarUrl != null
                    && !avatarUrl.isBlank()) {
                profile.setAvatarUrl(avatarUrl);
                profileRepository.save(profile);
            }
            return;
        }
        if (existingProfile.isPresent()) {
            UserProfile profile = existingProfile.get();
            profile.setDeletedAt(null);
            if ((profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) && avatarUrl != null
                    && !avatarUrl.isBlank()) {
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
                .isAdmin(user.isAdmin())
                .canCreateCourses(user.isCanCreateCourses())
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

        enforceConcurrentSessionCap(user.getId());

        String refreshToken = jwtService.createRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hash(refreshToken))
                .userId(user.getId())
                .expiresAt(jwtService.refreshTokenExpiresAt())
                .build());
        boolean isProfileDone = profile != null
                && (profile.isProfileCompleted() || (profile.getHandle() != null && !profile.getHandle().isBlank()));

        return AuthResponse.builder()
                .accessToken(jwtService.createAccessToken(user.getId()))
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(profile == null ? null : profile.getDisplayName())
                .avatarUrl(profile == null ? null : profile.getAvatarUrl())
                .canCreateCourses(user.isCanCreateCourses() || (profile != null && profile.isCanCreateCourses()))
                .isAdmin(user.isAdmin() || (profile != null && profile.isAdmin()))
                .isNewUser(false)
                .profileCompleted(isProfileDone)
                .build();
    }

    private void enforceConcurrentSessionCap(String userId) {
        int maxConcurrentSessions = 5;
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
        if (activeTokens != null && activeTokens.size() >= maxConcurrentSessions) {
            int toPrune = activeTokens.size() - maxConcurrentSessions + 1;
            for (int i = 0; i < toPrune && i < activeTokens.size(); i++) {
                RefreshToken oldest = activeTokens.get(i);
                if (oldest.getId() != null) {
                    refreshTokenRepository.deleteById(oldest.getId());
                } else if (oldest.getTokenHash() != null) {
                    refreshTokenRepository.deleteByTokenHash(oldest.getTokenHash());
                }
            }
        }
    }

    private @NonNull String normalizeEmail(@NonNull String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private @NonNull String hash(@NonNull String token) {
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
        @Serial
        private static final long serialVersionUID = 1L;

        AuthenticationServiceException(String message) {
            super(message);
        }
    }

    private String requireValidEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        // Optional: add basic format check
        // if (!EmailValidator.getInstance().isValid(normalized)) {
        // throw new IllegalArgumentException("Invalid email format");
        // }
        return normalized;
    }

    private String requireNonBlank(String value, String message) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private boolean isSocialOnlyAccount(User user) {
        return user.getPasswordHash() == null || user.getPasswordHash().isBlank();
    }

    private String getFormattedProviders(String userId) {
        if (userId == null || oauthRepository == null) {
            return "";
        }
        return oauthRepository.findAllByUserId(userId).stream()
                .map(o -> formatProviderName(o.getProvider()))
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
