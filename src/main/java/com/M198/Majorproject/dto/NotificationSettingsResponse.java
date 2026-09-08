package com.M198.Majorproject.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSettingsResponse {
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean inAppEnabled;
    private Instant updatedAt;
}
