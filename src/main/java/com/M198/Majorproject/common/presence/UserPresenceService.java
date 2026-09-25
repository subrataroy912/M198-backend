package com.M198.Majorproject.common.presence;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.user.profile.entity.UserProfile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {

    /**
     * Grace window so page refreshes, route transitions, and periodic heartbeats
     * keep the user smoothly marked Online without flickering.
     */
    private static final long ONLINE_GRACE_SECONDS = 65L;
    private static final long DB_PERSIST_THROTTLE_SECONDS = 90L;

    private final MongoTemplate mongoTemplate;

    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userToSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionToSpaces = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastActiveAtMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastPersistedAtMap = new ConcurrentHashMap<>();

    public record DisconnectInfo(String userId, Set<String> spaceIds, boolean stillOnline) {}

    public boolean markUserActive(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        boolean wasOnline = isOnline(userId);
        Instant now = Instant.now();
        lastActiveAtMap.put(userId, now);
        persistLastActiveIfNeeded(userId, now, false);
        return !wasOnline;
    }

    public boolean registerWebSocketSession(String sessionId, String userId) {
        if (sessionId == null || userId == null || userId.isBlank()) {
            return false;
        }
        boolean wasOnline = isOnline(userId);
        sessionToUser.put(sessionId, userId);
        userToSessions
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        Instant now = Instant.now();
        lastActiveAtMap.put(userId, now);
        persistLastActiveIfNeeded(userId, now, false);
        return !wasOnline;
    }

    public void registerSpaceSubscription(String sessionId, String userId, String spaceId) {
        if (sessionId == null || spaceId == null || spaceId.isBlank()) {
            return;
        }
        if (userId != null && !userId.isBlank()) {
            registerWebSocketSession(sessionId, userId);
        }
        sessionToSpaces
                .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(spaceId);
    }

    public DisconnectInfo unregisterWebSocketSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        String userId = sessionToUser.remove(sessionId);
        Set<String> spaces = sessionToSpaces.remove(sessionId);
        if (spaces == null) {
            spaces = Collections.emptySet();
        } else {
            spaces = new HashSet<>(spaces);
        }

        if (userId == null) {
            return null;
        }

        Set<String> sessions = userToSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userToSessions.remove(userId);
            }
        }

        Instant now = Instant.now();
        lastActiveAtMap.put(userId, now);
        persistLastActiveIfNeeded(userId, now, true);

        boolean hasRemainingSessions = hasActiveSession(userId);
        return new DisconnectInfo(userId, spaces, hasRemainingSessions);
    }

    public boolean hasActiveSession(String userId) {
        if (userId == null) {
            return false;
        }
        Set<String> sessions = userToSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public boolean isOnline(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        if (hasActiveSession(userId)) {
            return true;
        }
        Instant lastActive = lastActiveAtMap.get(userId);
        if (lastActive == null) {
            return false;
        }
        return Duration.between(lastActive, Instant.now()).getSeconds() <= ONLINE_GRACE_SECONDS;
    }

    public Instant getLastActiveAt(String userId, Instant fallback) {
        if (userId == null) {
            return fallback;
        }
        Instant inMemory = lastActiveAtMap.get(userId);
        if (inMemory != null) {
            return inMemory;
        }
        return fallback;
    }

    public Set<String> getOnlineUserIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptySet();
        }
        return userIds.stream()
                .filter(this::isOnline)
                .collect(Collectors.toSet());
    }

    private void persistLastActiveIfNeeded(String userId, Instant now, boolean force) {
        try {
            Instant lastPersisted = lastPersistedAtMap.get(userId);
            if (!force && lastPersisted != null
                    && Duration.between(lastPersisted, now).getSeconds() < DB_PERSIST_THROTTLE_SECONDS) {
                return;
            }
            lastPersistedAtMap.put(userId, now);
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("user_id").is(userId)),
                    new Update().set("last_active_at", now),
                    UserProfile.class);
        } catch (Exception ex) {
            log.debug("Non-critical failure persisting last_active_at for user {}: {}", userId, ex.getMessage());
        }
    }
}
