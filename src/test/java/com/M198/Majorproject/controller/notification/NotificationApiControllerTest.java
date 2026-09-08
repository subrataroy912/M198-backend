package com.M198.Majorproject.controller.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.M198.Majorproject.controller.auth.AuthExceptionHandler;
import com.M198.Majorproject.dto.NotificationResponse;
import com.M198.Majorproject.service.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationApiControllerTest {

    @Mock
    private NotificationService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationApiController(service))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
        void notificationListReturnsForbiddenWithoutAuthentication() throws Exception {
        when(service.list(null, false, 0, 20)).thenThrow(new NotificationService.NotificationAccessException());

        mockMvc.perform(get("/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void notificationListReturnsPaginationEnvelope() throws Exception {
        when(service.list(org.mockito.ArgumentMatchers.nullable(Authentication.class), eq(true), eq(1), eq(5)))
                .thenReturn(new PageImpl<>(List.of(new NotificationResponse())));

        mockMvc.perform(get("/v1/notifications?unreadOnly=true&page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content").isArray())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.page").value(0));
    }

    @Test
    void missingNotificationReturnsNotFound() throws Exception {
        when(service.markRead(eq("missing"), org.mockito.ArgumentMatchers.nullable(Authentication.class)))
                .thenThrow(new NotificationService.NotificationNotFoundException());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/v1/notifications/missing/read"))
                .andExpect(status().isNotFound());
    }
}
