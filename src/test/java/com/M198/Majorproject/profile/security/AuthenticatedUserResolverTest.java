package com.M198.Majorproject.profile.security;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.identity.entity.AccountStatus;
import com.M198.Majorproject.identity.entity.User;
import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.identity.repository.UserProfileRepository;
import com.M198.Majorproject.identity.repository.UserRepository;
import com.M198.Majorproject.profile.exception.ProfileNotFoundException;

class AuthenticatedUserResolverTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private final Authentication authentication = mock(Authentication.class);
    private AuthenticatedUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthenticatedUserResolver(userRepository, profileRepository);
    }

    @Test
    void authenticatedUserId_ThrowsWhenUnauthenticated() {
        assertThrows(ProfileNotFoundException.class, () -> resolver.authenticatedUserId(null));

        when(authentication.isAuthenticated()).thenReturn(false);
        assertThrows(ProfileNotFoundException.class, () -> resolver.authenticatedUserId(authentication));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(null);
        assertThrows(ProfileNotFoundException.class, () -> resolver.authenticatedUserId(authentication));
    }

    @Test
    void resolveCurrentUser_Success() {
        User user = User.builder().id("u-1").active(true).status(AccountStatus.ACTIVE).build();
        UserProfile profile = UserProfile.builder().userId("u-1").displayName("Alice").build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("u-1");
        when(userRepository.findByIdAndActiveTrueAndStatus("u-1", AccountStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId("u-1")).thenReturn(Optional.of(profile));

        UserContext context = resolver.resolveCurrentUser(authentication);

        assertNotNull(context);
        assertEquals(user, context.user());
        assertEquals(profile, context.profile());
    }

    @Test
    void resolveCurrentUser_ThrowsWhenSoftDeleted() {
        User user = User.builder().id("u-1").active(true).status(AccountStatus.ACTIVE).build();
        UserProfile profile = UserProfile.builder().userId("u-1").deletedAt(Instant.now()).build();

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("u-1");
        when(userRepository.findByIdAndActiveTrueAndStatus("u-1", AccountStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId("u-1")).thenReturn(Optional.of(profile));

        assertThrows(ProfileNotFoundException.class, () -> resolver.resolveCurrentUser(authentication));
    }
}
