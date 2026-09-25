package com.M198.Majorproject.common.presence;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.message.service.SpaceChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final UserPresenceService userPresenceService;
    private final CourseMembershipRepository membershipRepository;
    private final SpaceChatService spaceChatService;

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        String userId = authentication.getName();
        boolean transitionedToOnline = userPresenceService.markUserActive(userId);
        if (transitionedToOnline) {
            List<CourseMembership> memberships = membershipRepository.findAllByUserIdAndStatus(
                    userId, MembershipStatus.ACTIVE);
            for (CourseMembership m : memberships) {
                if (m.getCourseId() != null) {
                    spaceChatService.broadcastSpacePresence(m.getCourseId(), userId, true);
                }
            }
        }
        Instant now = Instant.now();
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "online", true,
                "lastActiveAt", now.toString()));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Map<String, Object>>> getUsersPresence(
            @RequestParam(name = "ids", required = false) List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (String id : userIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            boolean online = userPresenceService.isOnline(id);
            Instant lastActive = userPresenceService.getLastActiveAt(id, null);
            Map<String, Object> entry = new HashMap<>();
            entry.put("userId", id);
            entry.put("online", online);
            if (lastActive != null) {
                entry.put("lastActiveAt", lastActive.toString());
            }
            result.put(id, entry);
        }
        return ResponseEntity.ok(result);
    }
}
