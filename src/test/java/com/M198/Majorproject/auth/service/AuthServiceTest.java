package com.M198.Majorproject.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

import com.M198.Majorproject.auth.dto.AuthResponse;
import com.M198.Majorproject.auth.dto.LoginRequest;
import com.M198.Majorproject.auth.dto.RegisterUserRequest;
import com.M198.Majorproject.identity.entity.AccountStatus;
import com.M198.Majorproject.identity.entity.OAuthProvider;
import com.M198.Majorproject.identity.entity.RefreshToken;
import com.M198.Majorproject.identity.entity.User;
import com.M198.Majorproject.identity.entity.UserOAuth;
import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.identity.repository.RefreshTokenRepository;
import com.M198.Majorproject.identity.repository.UserOAuthRepository;
import com.M198.Majorproject.identity.repository.UserProfileRepository;
import com.M198.Majorproject.identity.repository.UserRepository;
import com.M198.Majorproject.common.security.JwtService;

class AuthServiceTest {

        private final UserRepository userRepository = mock(UserRepository.class);
        private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
        private final UserOAuthRepository oauthRepository = mock(UserOAuthRepository.class);
        private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        private final JwtService jwtService = new JwtService("test-secret-that-is-long-enough-32",
                        java.time.Duration.ofMinutes(5), java.time.Duration.ofDays(1));
        private final AuthService authService = new AuthService(
                        userRepository,
                        profileRepository,
                        oauthRepository,
                        passwordEncoder,
                        authenticationManager,
                        jwtService,
                        refreshTokenRepository);

        @Test
        void registrationPersistsUserAndProfileWithDefaultPrivileges() {
                RegisterUserRequest request = new RegisterUserRequest();
                request.setEmail("newuser@example.com");
                request.setPassword("password123");
                request.setFirstName("New");
                request.setLastName("User");

                when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
                when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
                when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                        User u = inv.getArgument(0);
                        u.setId("new-user-id");
                        return u;
                });
                when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

                AuthResponse response = authService.register(request);

                assertNotNull(response);
                assertEquals("new-user-id", response.getUserId());
                verify(userRepository).save(argThat(user -> !user.isAdmin() && !user.isCanCreateCourses()));
                verify(profileRepository).save(argThat(profile -> !profile.isAdmin() && !profile.isCanCreateCourses()));
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
                                OAuthProvider.GOOGLE, "g-123", "alice@example.com", true, "Alice Smith",
                                "https://avatar.url");

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
                var exception = assertThrows(OAuth2AuthenticationException.class, () -> authService.authenticateOAuth(
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

                var exception = assertThrows(DuplicateKeyException.class, () -> authService.register(request));
                org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("GitHub"));
        }

        @Test
        void refreshDeletesOldTokenDocumentAndIssuesNewOne() {
                User user = User.builder()
                                .id("user-1")
                                .email("user@example.com")
                                .status(AccountStatus.ACTIVE)
                                .active(true)
                                .build();
                when(userRepository.findByIdAndActiveTrueAndStatus("user-1", AccountStatus.ACTIVE))
                                .thenReturn(Optional.of(user));

                String rawToken = jwtService.createRefreshToken("user-1");
                String tokenHash = ReflectionTestUtils.invokeMethod(authService, "hash", rawToken);

                RefreshToken storedToken = RefreshToken.builder()
                                .tokenHash(tokenHash)
                                .userId("user-1")
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();

                when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash))
                                .thenReturn(Optional.of(storedToken));
                when(refreshTokenRepository.revokeIfActive(eq(tokenHash), any(Instant.class)))
                                .thenReturn(1L);

                AuthResponse response = authService.refresh(rawToken);

                assertNotNull(response);
                assertEquals("user-1", response.getUserId());
                verify(refreshTokenRepository).deleteByTokenHash(tokenHash);
                verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        void logoutDeletesTokenDocumentRatherThanRevoking() {
                String rawToken = jwtService.createRefreshToken("user-1");
                String tokenHash = ReflectionTestUtils.invokeMethod(authService, "hash", rawToken);

                RefreshToken storedToken = RefreshToken.builder()
                                .tokenHash(tokenHash)
                                .userId("user-1")
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();

                when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash))
                                .thenReturn(Optional.of(storedToken));

                authService.logout("user-1", rawToken);

                verify(refreshTokenRepository).deleteByTokenHash(tokenHash);
        }

        @Test
        void refreshReuseOfAlreadyRotatedTokenTriggersFullSessionRevocation() {
                String rawToken = jwtService.createRefreshToken("user-1");
                String tokenHash = ReflectionTestUtils.invokeMethod(authService, "hash", rawToken);

                // Token is NOT found in repository (meaning already used and deleted)
                when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash))
                                .thenReturn(Optional.empty());

                assertThrows(org.springframework.security.core.AuthenticationException.class,
                                () -> authService.refresh(rawToken));

                verify(refreshTokenRepository).deleteAllByUserId("user-1");
        }

        @Test
        void exceedingConcurrentSessionCapPrunesOldestToken() {
                User user = User.builder()
                                .id("user-1")
                                .email("user@example.com")
                                .status(AccountStatus.ACTIVE)
                                .active(true)
                                .build();

                RefreshToken t1 = RefreshToken.builder().id("token-1").tokenHash("h1").userId("user-1").build();
                RefreshToken t2 = RefreshToken.builder().id("token-2").tokenHash("h2").userId("user-1").build();
                RefreshToken t3 = RefreshToken.builder().id("token-3").tokenHash("h3").userId("user-1").build();
                RefreshToken t4 = RefreshToken.builder().id("token-4").tokenHash("h4").userId("user-1").build();
                RefreshToken t5 = RefreshToken.builder().id("token-5").tokenHash("h5").userId("user-1").build();

                when(refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc("user-1"))
                                .thenReturn(List.of(t1, t2, t3, t4, t5));

                ReflectionTestUtils.invokeMethod(authService, "issueTokens", user);

                verify(refreshTokenRepository).deleteById("token-1");
                verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        void issueTokensPreservesCanCreateCoursesAndAdminFlagsFromUserAndProfile() {
                User creatorUser = User.builder()
                                .id("creator-1")
                                .email("creator@example.com")
                                .status(AccountStatus.ACTIVE)
                                .active(true)
                                .canCreateCourses(true)
                                .isAdmin(false)
                                .build();

                UserProfile profile = UserProfile.builder()
                                .userId("creator-1")
                                .displayName("Creator Name")
                                .avatarUrl("https://example.com/avatar.png")
                                .canCreateCourses(true)
                                .isAdmin(false)
                                .build();

                when(profileRepository.findByUserId("creator-1")).thenReturn(Optional.of(profile));

                AuthResponse response = ReflectionTestUtils.invokeMethod(authService, "issueTokens", creatorUser);

                assertNotNull(response);
                assertEquals("creator-1", response.getUserId());
                assertEquals("creator@example.com", response.getEmail());
                assertEquals("Creator Name", response.getDisplayName());
                assertEquals("https://example.com/avatar.png", response.getAvatarUrl());
                org.junit.jupiter.api.Assertions.assertTrue(response.isCanCreateCourses());
                org.junit.jupiter.api.Assertions.assertFalse(response.isAdmin());
        }

        @Test
        void issueTokensPreservesAdminFlagWhenProfileOrUserIsAdmin() {
                User adminUser = User.builder()
                                .id("admin-1")
                                .email("admin@example.com")
                                .status(AccountStatus.ACTIVE)
                                .active(true)
                                .canCreateCourses(false)
                                .isAdmin(true)
                                .build();

                when(profileRepository.findByUserId("admin-1")).thenReturn(Optional.empty());

                AuthResponse response = ReflectionTestUtils.invokeMethod(authService, "issueTokens", adminUser);

                assertNotNull(response);
                org.junit.jupiter.api.Assertions.assertTrue(response.isAdmin());
                org.junit.jupiter.api.Assertions.assertFalse(response.isCanCreateCourses());
        }
}
