package com.M198.Majorproject.notification.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.M198.Majorproject.dto.PageResponse;
import com.M198.Majorproject.notification.dto.NotificationResponse;
import com.M198.Majorproject.notification.dto.NotificationSettingsRequest;
import com.M198.Majorproject.notification.dto.NotificationSettingsResponse;
import com.M198.Majorproject.notification.service.NotificationService;

@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication a) {
        return PageResponse.from(service.list(a, unreadOnly, page, size));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse read(@PathVariable String notificationId, Authentication a) {
        return service.markRead(notificationId, a);
    }

    @GetMapping("/settings")
    public NotificationSettingsResponse settings(Authentication a) {
        return service.settings(a);
    }

    @PatchMapping("/settings")
    public NotificationSettingsResponse update(@Valid @RequestBody NotificationSettingsRequest request, Authentication a) {
        return service.updateSettings(a, request);
    }
}
