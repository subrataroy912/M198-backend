package com.M198.Majorproject.discovery.notification.service;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DuplicateKeyException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.discovery.notification.dto.NotificationResponse;
import com.M198.Majorproject.discovery.notification.dto.NotificationSettingsRequest;
import com.M198.Majorproject.discovery.notification.dto.NotificationSettingsResponse;
import com.M198.Majorproject.discovery.notification.entity.Notification;
import com.M198.Majorproject.discovery.notification.entity.NotificationResourceType;
import com.M198.Majorproject.discovery.notification.entity.NotificationSettings;
import com.M198.Majorproject.discovery.notification.entity.NotificationType;
import com.M198.Majorproject.discovery.notification.repository.NotificationRepository;
import com.M198.Majorproject.discovery.notification.repository.NotificationSettingsRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public void sendNotification(
            String recipientId,
            NotificationType type,
            String title,
            String message,
            NotificationResourceType resourceType,
            String resourceId) {
        String resolvedCourseId = resourceType == NotificationResourceType.COURSE ? resourceId : null;
        sendNotification(recipientId, type, title, message, resourceType, resourceId, resolvedCourseId);
    }

    public void sendNotification(
            String recipientId,
            NotificationType type,
            String title,
            String message,
            NotificationResourceType resourceType,
            String resourceId,
            String courseId) {
        if (recipientId == null || recipientId.isBlank()) {
            return;
        }
        NotificationSettings userSettings = settingsRepository != null
                ? settingsRepository.findByUserId(recipientId).orElse(null)
                : null;
        if (userSettings != null && !userSettings.isInAppEnabled()) {
            return;
        }
        boolean pushEnabled = userSettings == null || userSettings.isPushEnabled();

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .courseId(courseId != null ? courseId : (resourceType == NotificationResourceType.COURSE ? resourceId : null))
                .read(false)
                .createdAt(Instant.now())
                .build();
        Notification saved = notificationRepository.save(notification);
        NotificationResponse dto = response(saved != null ? saved : notification);
        broadcastToUser(recipientId, java.util.Map.of(
                "eventType", "NOTIFICATION_CREATED",
                "notification", dto,
                "pushEnabled", pushEnabled));
    }

    public Page<NotificationResponse> list(Authentication a, boolean unreadOnly, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");

        }
        String id = userId(a);
        PageRequest request = PageRequest.of(page, size);
        return (unreadOnly ? notificationRepository.findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc(id, request) : notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(id, request)).map(this::response);
    }

    public NotificationResponse markRead(String notificationId, Authentication a) {
        String id = userId(a);
        Notification n = notificationRepository.findById(notificationId).filter(value -> id.equals(value.getRecipientId())).orElseThrow(NotificationNotFoundException::new);
        n.setRead(true);
        n.setReadAt(Instant.now());
        Notification saved = notificationRepository.save(n);
        NotificationResponse dto = response(saved != null ? saved : n);
        broadcastToUser(id, java.util.Map.of(
                "eventType", "NOTIFICATION_READ",
                "notificationId", dto.getId() != null ? dto.getId() : notificationId,
                "readAt", dto.getReadAt() != null ? dto.getReadAt().toString() : Instant.now().toString()));
        return dto;
    }

    public int markAllRead(Authentication a) {
        String id = userId(a);
        java.util.List<Notification> unread = notificationRepository.findAllByRecipientIdAndReadFalse(id);
        Instant now = Instant.now();
        if (unread != null && !unread.isEmpty()) {
            for (Notification n : unread) {
                n.setRead(true);
                n.setReadAt(now);
            }
            notificationRepository.saveAll(unread);
        }
        int updatedCount = unread != null ? unread.size() : 0;
        broadcastToUser(id, java.util.Map.of(
                "eventType", "NOTIFICATIONS_READ_ALL",
                "updatedCount", updatedCount,
                "readAt", now.toString()));
        return updatedCount;
    }

    private void broadcastToUser(String userId, Object payload) {
        if (messagingTemplate == null || userId == null || userId.isBlank()) {
            return;
        }
        try {
            messagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications", payload);
        } catch (Exception ignored) {
            // Best-effort real-time WebSocket push
        }
    }

    public NotificationSettingsResponse settings(Authentication a) {
        String id = userId(a);
        return settingsResponse(settingsRepository.findByUserId(id).orElseGet(() -> createSettingsRecoveringRace(id)));
    }

    public NotificationSettingsResponse updateSettings(Authentication a, NotificationSettingsRequest request) {
        String id = userId(a);
        NotificationSettings s = settingsRepository.findByUserId(id).orElseGet(() -> createSettingsRecoveringRace(id));
        if (request.getEmailEnabled() != null) {
            s.setEmailEnabled(request.getEmailEnabled());

        }
        if (request.getPushEnabled() != null) {
            s.setPushEnabled(request.getPushEnabled());

        }
        if (request.getInAppEnabled() != null) {
            s.setInAppEnabled(request.getInAppEnabled());

        }
        return settingsResponse(settingsRepository.save(s));
    }

    private String userId(Authentication a) {
        if (a == null || !a.isAuthenticated() || a.getName() == null) {
            throw new NotificationAccessException();

        }
        return a.getName();
    }

    private NotificationSettings createSettingsRecoveringRace(String userId) {
        try {
            return settingsRepository.save(NotificationSettings.builder().userId(userId).build());
        } catch (DuplicateKeyException exception) {
            return settingsRepository.findByUserId(userId).orElseThrow(() -> exception);
        }
    }

    private NotificationResponse response(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setResourceType(n.getResourceType());
        r.setResourceId(n.getResourceId());
        r.setCourseId(n.getCourseId() != null ? n.getCourseId()
                : (n.getResourceType() == NotificationResourceType.COURSE ? n.getResourceId() : null));
        r.setRead(n.isRead());
        r.setReadAt(n.getReadAt());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }

    private NotificationSettingsResponse settingsResponse(NotificationSettings value) {
        NotificationSettingsResponse response = new NotificationSettingsResponse();
        response.setEmailEnabled(value.isEmailEnabled());
        response.setPushEnabled(value.isPushEnabled());
        response.setInAppEnabled(value.isInAppEnabled());
        response.setUpdatedAt(value.getUpdatedAt());
        return response;
    }

    public static class NotificationNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class NotificationAccessException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
