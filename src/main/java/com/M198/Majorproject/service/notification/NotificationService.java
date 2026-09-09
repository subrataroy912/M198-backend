package com.M198.Majorproject.service.notification;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DuplicateKeyException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.NotificationResponse;
import com.M198.Majorproject.dto.NotificationSettingsRequest;
import com.M198.Majorproject.dto.NotificationSettingsResponse;
import com.M198.Majorproject.entity.notification.Notification;
import com.M198.Majorproject.entity.notification.NotificationSettings;
import com.M198.Majorproject.repository.notification.NotificationRepository;
import com.M198.Majorproject.repository.notification.NotificationSettingsRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;
    public NotificationService(NotificationRepository notificationRepository, NotificationSettingsRepository settingsRepository) { this.notificationRepository = notificationRepository; this.settingsRepository = settingsRepository; }
    public Page<NotificationResponse> list(Authentication a, boolean unreadOnly, int page, int size) { if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100"); String id = userId(a); PageRequest request = PageRequest.of(page, size); return (unreadOnly ? notificationRepository.findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc(id, request) : notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(id, request)).map(this::response); }
    public NotificationResponse markRead(String notificationId, Authentication a) { String id = userId(a); Notification n = notificationRepository.findById(notificationId).filter(value -> id.equals(value.getRecipientId())).orElseThrow(NotificationNotFoundException::new); n.setRead(true); n.setReadAt(Instant.now()); return response(notificationRepository.save(n)); }
    public NotificationSettingsResponse settings(Authentication a) {
        String id = userId(a);
        return settingsResponse(settingsRepository.findByUserId(id).orElseGet(() -> createSettingsRecoveringRace(id)));
    }
    public NotificationSettingsResponse updateSettings(Authentication a, NotificationSettingsRequest request) { String id = userId(a); NotificationSettings s = settingsRepository.findByUserId(id).orElseGet(() -> createSettingsRecoveringRace(id)); if (request.getEmailEnabled() != null) s.setEmailEnabled(request.getEmailEnabled()); if (request.getPushEnabled() != null) s.setPushEnabled(request.getPushEnabled()); if (request.getInAppEnabled() != null) s.setInAppEnabled(request.getInAppEnabled()); return settingsResponse(settingsRepository.save(s)); }
    private String userId(Authentication a) { if (a == null || !a.isAuthenticated() || a.getName() == null) throw new NotificationAccessException(); return a.getName(); }
    private NotificationSettings createSettingsRecoveringRace(String userId) {
        try {
            return settingsRepository.save(NotificationSettings.builder().userId(userId).build());
        } catch (DuplicateKeyException exception) {
            return settingsRepository.findByUserId(userId).orElseThrow(() -> exception);
        }
    }
    private NotificationResponse response(Notification n) { NotificationResponse r = new NotificationResponse(); r.setId(n.getId()); r.setType(n.getType()); r.setTitle(n.getTitle()); r.setMessage(n.getMessage()); r.setResourceType(n.getResourceType()); r.setResourceId(n.getResourceId()); r.setRead(n.isRead()); r.setReadAt(n.getReadAt()); r.setCreatedAt(n.getCreatedAt()); return r; }
    private NotificationSettingsResponse settingsResponse(NotificationSettings value) { NotificationSettingsResponse response = new NotificationSettingsResponse(); response.setEmailEnabled(value.isEmailEnabled()); response.setPushEnabled(value.isPushEnabled()); response.setInAppEnabled(value.isInAppEnabled()); response.setUpdatedAt(value.getUpdatedAt()); return response; }
    public static class NotificationNotFoundException extends RuntimeException { private static final long serialVersionUID = 1L; }
    public static class NotificationAccessException extends RuntimeException { private static final long serialVersionUID = 1L; }
}
