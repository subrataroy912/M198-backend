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

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.AuthResponse;
import com.M198.Majorproject.dto.LoginRequest;
import com.M198.Majorproject.dto.LogoutRequest;
import com.M198.Majorproject.dto.RefreshTokenRequest;
import com.M198.Majorproject.dto.RegisterUserRequest;
import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.entity.identity.OAuthProvider;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.entity.identity.UserOAuth;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.entity.identity.RefreshToken;
import com.M198.Majorproject.repository.identity.UserOAuthRepository;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.M198.Majorproject.repository.identity.UserRepository;
import com.M198.Majorproject.repository.identity.RefreshTokenRepository;
import com.M198.Majorproject.security.JwtService;
import com.inngest.Inngest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserOAuthRepository oauthRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Inngest inngest;

    public AuthService(
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            UserOAuthRepository oauthRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            Inngest inngest) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.oauthRepository = oauthRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.inngest = inngest;
    }

    public AuthResponse register(RegisterUserRequest request) {
        if (request.getAccountType() != AccountType.TEACHER && request.getAccountType() != AccountType.STUDENT) {
            throw new IllegalArgumentException("Public registration requires a teacher or student account");
        }
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateKeyException("Email is already registered");
        }

        User user = userRepository.save(User.builder()
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
                .build());
        emitUserRegisteredEvent(user);
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.getPassword()));
        User user = userRepository.findByEmailAndActiveTrueAndStatus(email, AccountStatus.ACTIVE)
                .orElseThrow(() -> new AuthenticationServiceException("Invalid credentials"));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        emitUserLoggedInEvent(user);
        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
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

    public void logout(String authenticatedUserId, LogoutRequest request) {
        String token = request.getRefreshToken();
        String tokenUserId = jwtService.parseAndValidate(token, "refresh").getSubject();
        if (!authenticatedUserId.equals(tokenUserId)) {
            throw new AuthenticationServiceException("Invalid refresh token");
        }
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(token)).ifPresent(storedToken -> {
            if (!authenticatedUserId.equals(storedToken.getUserId())) {
                throw new AuthenticationServiceException("Invalid refresh token");
            }
            storedToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(storedToken);
        });
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

        User user;
        var existingLink = oauthRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (existingLink.isPresent()) {
            user = userRepository.findByIdAndActiveTrueAndStatus(existingLink.get().getUserId(), AccountStatus.ACTIVE)
                .orElseThrow(() -> new AuthenticationServiceException("OAuth account is unavailable"));
        } else {
            user = userRepository.findByEmailAndActiveTrueAndStatus(normalizeEmail(email), AccountStatus.ACTIVE)
                    .orElseGet(() -> createOAuthUserIfEmailIsAvailable(email, displayName, avatarUrl));
        }

        ensureProfileExists(user, displayName, avatarUrl);

        if (oauthRepository.findByUserIdAndProvider(user.getId(), provider).isEmpty()) {
            oauthRepository.save(UserOAuth.builder()
                    .userId(user.getId())
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .build());
            emitUserOAuthLinkedEvent(user, provider, providerUserId);
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        emitUserLoggedInEvent(user);
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
                .build());
        emitUserRegisteredEvent(user);
        return user;
    }

    private User createOAuthUserIfEmailIsAvailable(String email, String displayName, String avatarUrl) {
        if (userRepository.findByEmail(normalizeEmail(email)).isPresent()) {
            throw new AuthenticationServiceException("An account already exists for this email");
        }
        return createOAuthUser(email, displayName, avatarUrl);
    }

    private void emitUserRegisteredEvent(User user) {
        if (inngest == null) {
            return;
        }
        inngest.send(new com.inngest.InngestEvent("user-registered", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "accountType", user.getAccountType(),
                "createdAt", user.getCreatedAt() == null ? Instant.now() : user.getCreatedAt())));
    }

    private void emitUserLoggedInEvent(User user) {
        if (inngest == null) {
            return;
        }
        inngest.send(new com.inngest.InngestEvent("user-logged-in", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "accountType", user.getAccountType(),
                "lastLoginAt", user.getLastLoginAt() == null ? Instant.now() : user.getLastLoginAt())));
    }

    private void emitUserOAuthLinkedEvent(User user, OAuthProvider provider, String providerUserId) {
        if (inngest == null) {
            return;
        }
        inngest.send(new com.inngest.InngestEvent("user-oauth-linked", Map.of(
                "userId", user.getId(),
                "provider", provider,
                "providerUserId", providerUserId,
                "linkedAt", Instant.now())));
    }

    private void ensureProfileExists(User user, String displayName, String avatarUrl) {
        var existingProfile = profileRepository.findByUserId(user.getId());
        if (existingProfile.filter(profile -> profile.getDeletedAt() == null).isPresent()) {
            return;
        }
        if (existingProfile.isPresent()) {
            UserProfile profile = existingProfile.get();
            profile.setDeletedAt(null);
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
        String value = displayName == null || displayName.isBlank() ? email.substring(0, email.indexOf('@')) : displayName.trim();
        int separator = value.indexOf(' ');
        return separator < 0 ? new String[] { value, "" } : new String[] { value.substring(0, separator), value.substring(separator + 1).trim() };
    }

    private static class AuthenticationServiceException extends AuthenticationException {
        private static final long serialVersionUID = 1L;

        AuthenticationServiceException(String message) {
            super(message);
        }
    }
}