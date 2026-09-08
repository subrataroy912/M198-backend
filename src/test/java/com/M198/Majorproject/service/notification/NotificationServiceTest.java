package com.M198.Majorproject.service.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.entity.notification.NotificationSettings;
import com.M198.Majorproject.repository.notification.NotificationRepository;
import com.M198.Majorproject.repository.notification.NotificationSettingsRepository;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationSettingsRepository settingsRepository = mock(NotificationSettingsRepository.class);
    private final NotificationService notificationService = new NotificationService(
            notificationRepository, settingsRepository);
    private final Authentication authentication = mock(Authentication.class);

    @Test
    void settingsReturnsExistingDocumentWithoutWriting() {
        NotificationSettings settings = NotificationSettings.builder().userId("user-1").build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user-1");
        when(settingsRepository.findByUserId("user-1")).thenReturn(Optional.of(settings));

        assertTrue(notificationService.settings(authentication).isInAppEnabled());
    }

    @Test
    void settingsRecoversWhenAnotherRequestWinsUniqueInsert() {
        NotificationSettings settings = NotificationSettings.builder().userId("user-1").build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user-1");
        when(settingsRepository.findByUserId("user-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(settings));
        when(settingsRepository.save(any(NotificationSettings.class)))
                .thenThrow(new DuplicateKeyException("user_id already exists"));

        assertTrue(notificationService.settings(authentication).isInAppEnabled());
        verify(settingsRepository, times(2)).findByUserId("user-1");
    }
}
