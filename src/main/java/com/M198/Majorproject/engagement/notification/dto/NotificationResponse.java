package com.M198.Majorproject.engagement.notification.dto;

import java.time.Instant;

import com.M198.Majorproject.engagement.notification.entity.NotificationResourceType;
import com.M198.Majorproject.engagement.notification.entity.NotificationType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationResourceType resourceType;
    private String resourceId;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;
}
