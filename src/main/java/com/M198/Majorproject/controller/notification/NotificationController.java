/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
	 * ==========================================
	 * API Functions inside the Notification Controller:
	 * ==========================================
	 * 1. getMyNotifications   - Fetches notifications for the logged-in user
	 * 2. markAsRead            - Marks a notification as read
	 * 3. updateAlertSettings   - Updates email, push, or in-app alert settings
 */

/*
 * =================================================================================
 * NOTIFICATION CONTROLLER ENDPOINT DOCUMENTATION
 * =================================================================================
 *
 * 1. getMyNotifications
 *    - Route: GET /v1/notifications
 *    - Role Allowed: Authenticated users.
 *    - Request: Optional page, size, unreadOnly, and sort query parameters.
 *    - How it works: Uses the authenticated user ID to fetch only that user's
 *      notifications, ordered by creation time.
 *    - Response: Returns notification ID, type, message, related resource, read
 *      status, and creation time in a paginated response.
 *    - Why it's used: Populates the user's activity or notification feed.
 *
 * 2. markAsRead
 *    - Route: PATCH /v1/notifications/{notificationId}/read
 *    - Role Allowed: The notification owner only.
 *    - Request: Notification ID path parameter.
 *    - How it works: Confirms ownership and changes the notification read status
 *      without allowing the caller to modify its message or recipient.
 *    - Response: Returns the updated notification or no content.
 *    - Why it's used: Removes an alert from the user's unread count.
 *
 * 3. updateAlertSettings
 *    - Route: PATCH /v1/notifications/settings
 *    - Role Allowed: Authenticated users.
 *    - Request Body: Email, push, or in-app preferences for supported event types.
 *    - How it works: Validates supported settings and updates preferences belonging
 *      to the authenticated account.
 *    - Response: Returns the saved notification settings.
 *    - Why it's used: Lets users control which events generate alerts.
 */
package com.M198.Majorproject.controller.notification;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

}
