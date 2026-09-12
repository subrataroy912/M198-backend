package com.M198.Majorproject.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.M198.Majorproject.dto.AuthResponse;
import com.M198.Majorproject.dto.LoginRequest;
import com.M198.Majorproject.dto.RegisterUserRequest;
import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.entity.identity.OAuthProvider;
import com.M198.Majorproject.entity.identity.RefreshToken;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.entity.identity.UserOAuth;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.repository.identity.RefreshTokenRepository;
import com.M198.Majorproject.repository.identity.UserOAuthRepository;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.M198.Majorproject.repository.identity.UserRepository;
import com.M198.Majorproject.security.JwtService;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private final UserOAuthRepository oauthRepository = mock(UserOAuthRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final AuthService authService = new AuthService(
            userRepository,
            profileRepository,
            oauthRepository,
            passwordEncoder,
            authenticationManager,
            new JwtService("test-secret-that-is-long-enough-32", java.time.Duration.ofMinutes(5), java.time.Duration.ofDays(1)),
            refreshTokenRepository);

    @Test
    void publicRegistrationRejectsAdministratorRoleBeforePersistence() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setAccountType(AccountType.ADMIN);

        var exception = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        org.junit.jupiter.api.Assertions.assertEquals(
                "Public registration requires a teacher or student account", exception.getMessage());
        verifyNoInteractions(userRepository, profileRepository, refreshTokenRepository);
    }

    @Test
    void refreshCookieUsesSecureHttpOnlySameSiteAndRootPath() {
        ReflectionTestUtils.setField(authService, "secureCookies", true);
        ReflectionTestUtils.setField(authService, "cookieSameSite", "Lax");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authService.setRefreshCookie(response, "refresh-token");

        String setCookie = response.getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("HttpOnly"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("Secure"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("SameSite=Lax"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("Path=/"));
    }

    @Test
    void authenticateOAuthAutoLinksToExistingUserWhenEmailVerified() {
        User existingUser = User.builder()
                .id("user-1")
                .email("alice@example.com")
                .passwordHash("local-hash")
                .accountType(AccountType.STUDENT)
                .status(AccountStatus.ACTIVE)
                .active(true)
                .verified(true)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId("user-1")
                .displayName("Alice Smith")
                .build();

        when(oauthRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "g-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(oauthRepository.findByUserIdAndProvider("user-1", OAuthProvider.GOOGLE))
                .thenReturn(Optional.empty());
        when(profileRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(profile));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthResponse result = authService.authenticateOAuth(
                OAuthProvider.GOOGLE, "g-123", "alice@example.com", true, "Alice Smith", "https://avatar.url");

        assertNotNull(result);
        assertEquals("user-1", result.getUserId());
        assertEquals("alice@example.com", result.getEmail());
        verify(oauthRepository).save(any(UserOAuth.class));
    }

    @Test
    void authenticateOAuthLinksMultipleProvidersToSameUser() {
        User existingUser = User.builder()
                .id("user-1")
                .email("alice@example.com")
                .accountType(AccountType.STUDENT)
                .status(AccountStatus.ACTIVE)
                .active(true)
                .verified(true)
                .build();
        UserProfile profile = UserProfile.builder()
                .userId("user-1")
                .displayName("Alice")
                .build();

        when(oauthRepository.findByProviderAndProviderUserId(OAuthProvider.GITHUB, "gh-456"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(oauthRepository.findByUserIdAndProvider("user-1", OAuthProvider.GITHUB))
                .thenReturn(Optional.empty());
        when(profileRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(profile));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthResponse result = authService.authenticateOAuth(
                OAuthProvider.GITHUB, "gh-456", "alice@example.com", true, "Alice", null);

        assertNotNull(result);
        assertEquals("user-1", result.getUserId());
        verify(oauthRepository).save(any(UserOAuth.class));
    }

    @Test
    void authenticateOAuthRejectsUnverifiedEmail() {
        var exception = assertThrows(OAuth2AuthenticationException.class, () ->
                authService.authenticateOAuth(
                        OAuthProvider.GITHUB, "gh-456", "alice@example.com", false, "Alice", null));

        assertEquals("unverified_email", exception.getError().getErrorCode());
        verifyNoInteractions(userRepository, profileRepository);
    }

    @Test
    void loginRejectsOAuthUserWithoutPasswordWithClearMessage() {
        User oAuthUser = User.builder()
                .id("user-oauth")
                .email("social@example.com")
                .passwordHash(null)
                .accountType(AccountType.STUDENT)
                .status(AccountStatus.ACTIVE)
                .active(true)
                .build();

        when(userRepository.findByEmail("social@example.com"))
                .thenReturn(Optional.of(oAuthUser));
        when(oauthRepository.findAllByUserId("user-oauth"))
                .thenReturn(List.of(UserOAuth.builder().provider(OAuthProvider.GOOGLE).build()));

        LoginRequest request = new LoginRequest();
        request.setEmail("social@example.com");
        request.setPassword("any-password");

        var exception = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Google"));
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void registerRejectsExistingOAuthUserWithDescriptiveConflictMessage() {
        User oAuthUser = User.builder()
                .id("user-oauth")
                .email("social@example.com")
                .passwordHash(null)
                .accountType(AccountType.STUDENT)
                .status(AccountStatus.ACTIVE)
                .active(true)
                .build();

        when(userRepository.findByEmail("social@example.com"))
                .thenReturn(Optional.of(oAuthUser));
        when(oauthRepository.findAllByUserId("user-oauth"))
                .thenReturn(List.of(UserOAuth.builder().provider(OAuthProvider.GITHUB).build()));

        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("social@example.com");
        request.setPassword("password123");
        request.setFirstName("Social");
        request.setLastName("User");
        request.setAccountType(AccountType.STUDENT);

        var exception = assertThrows(DuplicateKeyException.class, () -> authService.register(request));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("GitHub"));
    }
}
