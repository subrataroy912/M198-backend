package com.M198.Majorproject.service.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.M198.Majorproject.dto.RegisterUserRequest;
import com.M198.Majorproject.entity.identity.AccountType;
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
}