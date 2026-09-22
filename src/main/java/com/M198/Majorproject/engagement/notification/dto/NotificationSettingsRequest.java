package com.M198.Majorproject.engagement.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSettingsRequest {
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
}
