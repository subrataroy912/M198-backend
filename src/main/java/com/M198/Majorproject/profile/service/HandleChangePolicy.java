package com.M198.Majorproject.profile.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.identity.repository.UserProfileRepository;

@Component
public class HandleChangePolicy {

    private static final int MAX_UPDATES_IN_WINDOW = 2;
    private static final Duration WINDOW_DURATION = Duration.ofDays(14);

    private final UserProfileRepository profileRepository;
    private final Clock clock;

    public HandleChangePolicy(UserProfileRepository profileRepository, Clock clock) {
        this.profileRepository = profileRepository;
        this.clock = clock;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public HandleChangePolicy(UserProfileRepository profileRepository) {
        this(profileRepository, Clock.systemUTC());
    }

    /**
     * Validates handle uniqueness and the 14-day rate limit policy, then records the change if allowed.
     *
     * @param profile the user profile being modified
     * @param rawNewHandle the requested new handle
     */
    public void validateAndApplyHandleChange(UserProfile profile, String rawNewHandle) {
        if (rawNewHandle == null) {
            return;
        }

        String newHandle = rawNewHandle.trim();
        String currentHandle = profile.getHandle() != null ? profile.getHandle().trim() : "";

        if (newHandle.equalsIgnoreCase(currentHandle)) {
            return;
        }

        if (!newHandle.isEmpty() && profileRepository.findByHandle(newHandle)
                .filter(existing -> !existing.getUserId().equals(profile.getUserId()))
                .isPresent()) {
            throw new IllegalArgumentException("Handle is already taken");
        }

        Instant windowStart = Instant.now(clock).minus(WINDOW_DURATION);
        List<Instant> timestamps = profile.getHandleUpdatedTimestamps();
        if (timestamps == null) {
            timestamps = new ArrayList<>();
            profile.setHandleUpdatedTimestamps(timestamps);
        }

        List<Instant> recentUpdates = timestamps.stream()
                .filter(ts -> ts != null && ts.isAfter(windowStart))
                .toList();

        if (recentUpdates.size() >= MAX_UPDATES_IN_WINDOW) {
            throw new IllegalArgumentException("You can only change your handle twice within a 14-day period.");
        }

        timestamps.add(Instant.now(clock));
        profile.setHandle(newHandle.isEmpty() ? null : newHandle);
    }
}
