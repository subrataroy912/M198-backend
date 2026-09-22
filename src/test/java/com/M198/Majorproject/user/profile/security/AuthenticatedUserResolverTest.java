package com.M198.Majorproject.user.profile.security;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.user.identity.entity.AccountStatus;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;
import com.M198.Majorproject.common.exception.UnauthorizedException;
import com.M198.Majorproject.user.identity.repository.UserRepository;
import com.M198.Majorproject.user.profile.exception.ProfileNotFoundException;

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
        assertThrows(UnauthorizedException.class, () -> resolver.authenticatedUserId(null));

        when(authentication.isAuthenticated()).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> resolver.authenticatedUserId(authentication));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(null);
        assertThrows(UnauthorizedException.class, () -> resolver.authenticatedUserId(authentication));
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

    @Test
    void resolveAuthenticatedUserIdSafe_ReturnsNullWhenUnauthenticated() {
        assertNull(resolver.resolveAuthenticatedUserIdSafe(null));

        when(authentication.isAuthenticated()).thenReturn(false);
        assertNull(resolver.resolveAuthenticatedUserIdSafe(authentication));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("anonymousUser");
        assertNull(resolver.resolveAuthenticatedUserIdSafe(authentication));

        when(authentication.getName()).thenReturn("u-1");
        assertEquals("u-1", resolver.resolveAuthenticatedUserIdSafe(authentication));
    }

    @Test
    void resolveUserByIdentifier_SupportsUserIdAndHandle() {
        User user = User.builder().id("u-1").active(true).status(AccountStatus.ACTIVE).build();
        UserProfile profile = UserProfile.builder().userId("u-1").handle("coder").build();

        when(userRepository.findByIdAndActiveTrueAndStatus("u-1", AccountStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId("u-1")).thenReturn(Optional.of(profile));
        when(profileRepository.findByHandleIgnoreCase("coder")).thenReturn(Optional.of(profile));

        // Lookup by userId
        UserContext byId = resolver.resolveUserByIdentifier("u-1");
        assertEquals("u-1", byId.user().getId());

        // Lookup by @handle
        UserContext byAtHandle = resolver.resolveUserByIdentifier("@coder");
        assertEquals("coder", byAtHandle.profile().getHandle());

        // Lookup by plain handle
        UserContext byHandle = resolver.resolveUserByIdentifier("coder");
        assertEquals("coder", byHandle.profile().getHandle());
    }
}
