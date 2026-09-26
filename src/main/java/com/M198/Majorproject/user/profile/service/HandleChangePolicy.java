package com.M198.Majorproject.user.profile.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.exception.HandleRateLimitExceededException;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;

@Component
public class HandleChangePolicy {

    public static final int MAX_UPDATES_IN_WINDOW = 3;
    public static final Duration WINDOW_DURATION = Duration.ofDays(14);

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
     * First-time handle creation (when handle is null/empty) is free and does not consume from the 3-change quota.
     *
     * @param profile the user profile being modified
     * @param rawNewHandle the requested new handle
     */
    public void validateAndApplyHandleChange(UserProfile profile, String rawNewHandle) {
        if (rawNewHandle == null) {
            return;
        }

        String normalized = rawNewHandle.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        String newHandle = normalized.toLowerCase();

        String currentHandle = profile.getHandle() != null ? profile.getHandle().trim().toLowerCase() : "";

        if (newHandle.equals(currentHandle)) {
            return;
        }

        if (!newHandle.isEmpty() && (newHandle.length() < 3 || newHandle.length() > 30 || !newHandle.matches("^[a-z0-9_]+$"))) {
            throw new IllegalArgumentException("Handle must be between 3 and 30 characters and contain only letters, numbers, or underscores");
        }

        if (!newHandle.isEmpty() && profileRepository.findByHandle(newHandle)
                .filter(existing -> !existing.getUserId().equals(profile.getUserId()))
                .isPresent()) {
            throw new IllegalArgumentException("Handle is already taken");
        }

        boolean isFirstTime = currentHandle.isEmpty();

        List<Instant> timestamps = profile.getHandleUpdatedTimestamps();
        if (timestamps == null) {
            timestamps = new ArrayList<>();
            profile.setHandleUpdatedTimestamps(timestamps);
        }

        if (!isFirstTime) {
            List<Instant> recentUpdates = getRecentUpdates(profile);
            if (recentUpdates.size() >= MAX_UPDATES_IN_WINDOW) {
                Instant resetsAt = recentUpdates.get(0).plus(WINDOW_DURATION);
                throw new HandleRateLimitExceededException(
                        "You can only change your handle 3 times within a 14-day period.",
                        resetsAt
                );
            }
            timestamps.add(Instant.now(clock));
        }

        profile.setHandle(newHandle.isEmpty() ? null : newHandle);
    }

    public List<Instant> getRecentUpdates(UserProfile profile) {
        if (profile == null || profile.getHandleUpdatedTimestamps() == null) {
            return List.of();
        }
        Instant windowStart = Instant.now(clock).minus(WINDOW_DURATION);
        return profile.getHandleUpdatedTimestamps().stream()
                .filter(ts -> ts != null && ts.isAfter(windowStart))
                .sorted()
                .toList();
    }

    public int getRemainingChanges(UserProfile profile) {
        List<Instant> recent = getRecentUpdates(profile);
        return Math.max(0, MAX_UPDATES_IN_WINDOW - recent.size());
    }

    public Instant getNextAllowedChangeAt(UserProfile profile) {
        List<Instant> recent = getRecentUpdates(profile);
        if (recent.size() < MAX_UPDATES_IN_WINDOW) {
            return null;
        }
        return recent.get(0).plus(WINDOW_DURATION);
    }
}
