package com.M198.Majorproject.user.profile.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.exception.HandleRateLimitExceededException;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;

class HandleChangePolicyTest {

    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private Instant baseTime;
    private Clock fixedClock;
    private HandleChangePolicy policy;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2026-09-01T12:00:00Z");
        fixedClock = Clock.fixed(baseTime, ZoneId.of("UTC"));
        policy = new HandleChangePolicy(profileRepository, fixedClock);
        profile = UserProfile.builder()
                .userId("u-1")
                .handle("initial_handle")
                .handleUpdatedTimestamps(new ArrayList<>())
                .build();
    }

    @Test
    void nullOrUnchangedHandleDoesNotChangeProfile() {
        policy.validateAndApplyHandleChange(profile, null);
        assertEquals("initial_handle", profile.getHandle());

        policy.validateAndApplyHandleChange(profile, "initial_handle");
        assertEquals("initial_handle", profile.getHandle());

        policy.validateAndApplyHandleChange(profile, "INITIAL_HANDLE");
        assertEquals("initial_handle", profile.getHandle());
    }

    @Test
    void duplicateHandleByAnotherUserThrowsException() {
        UserProfile anotherUser = UserProfile.builder().userId("u-2").handle("taken_handle").build();
        when(profileRepository.findByHandle("taken_handle")).thenReturn(Optional.of(anotherUser));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> policy.validateAndApplyHandleChange(profile, "taken_handle"));
        assertEquals("Handle is already taken", ex.getMessage());
    }

    @Test
    void firstTimeHandleSetupDoesNotConsumeQuota() {
        UserProfile newProfile = UserProfile.builder()
                .userId("u-new")
                .handle(null)
                .handleUpdatedTimestamps(new ArrayList<>())
                .build();

        policy.validateAndApplyHandleChange(newProfile, "brand_new_handle");
        assertEquals("brand_new_handle", newProfile.getHandle());
        assertEquals(0, newProfile.getHandleUpdatedTimestamps().size());
        assertEquals(3, policy.getRemainingChanges(newProfile));
        assertNull(policy.getNextAllowedChangeAt(newProfile));
    }

    @Test
    void enforceThreeChangesPerFourteenDays() {
        assertEquals(3, policy.getRemainingChanges(profile));

        policy.validateAndApplyHandleChange(profile, "handle_one");
        assertEquals("handle_one", profile.getHandle());
        assertEquals(2, policy.getRemainingChanges(profile));

        policy.validateAndApplyHandleChange(profile, "handle_two");
        assertEquals("handle_two", profile.getHandle());
        assertEquals(1, policy.getRemainingChanges(profile));

        policy.validateAndApplyHandleChange(profile, "handle_three");
        assertEquals("handle_three", profile.getHandle());
        assertEquals(0, policy.getRemainingChanges(profile));
        assertNotNull(policy.getNextAllowedChangeAt(profile));

        var ex = assertThrows(HandleRateLimitExceededException.class,
                () -> policy.validateAndApplyHandleChange(profile, "handle_four"));
        assertEquals("You can only change your handle 3 times within a 14-day period.", ex.getMessage());
        assertEquals(baseTime.plus(Duration.ofDays(14)), ex.getResetsAt());
    }

    @Test
    void updatesOlderThanFourteenDaysDoNotBlockNewChanges() {
        // Add 3 timestamps older than 14 days
        profile.getHandleUpdatedTimestamps().add(baseTime.minus(Duration.ofDays(16)));
        profile.getHandleUpdatedTimestamps().add(baseTime.minus(Duration.ofDays(15)));
        profile.getHandleUpdatedTimestamps().add(baseTime.minus(Duration.ofDays(14).plusSeconds(1)));

        // Should allow 3 fresh changes
        policy.validateAndApplyHandleChange(profile, "new_handle_1");
        assertEquals("new_handle_1", profile.getHandle());

        policy.validateAndApplyHandleChange(profile, "new_handle_2");
        assertEquals("new_handle_2", profile.getHandle());

        policy.validateAndApplyHandleChange(profile, "new_handle_3");
        assertEquals("new_handle_3", profile.getHandle());

        // 4th within the window should fail
        assertThrows(HandleRateLimitExceededException.class,
                () -> policy.validateAndApplyHandleChange(profile, "new_handle_4"));
    }

    @Test
    void normalizesLowercaseAndStripsLeadingAt() {
        policy.validateAndApplyHandleChange(profile, "@Cool_User");
        assertEquals("cool_user", profile.getHandle());
    }

    @Test
    void rejectsInvalidHandleFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateAndApplyHandleChange(profile, "ab")); // too short
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateAndApplyHandleChange(profile, "user-name!")); // invalid characters
    }
}
