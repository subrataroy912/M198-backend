package com.M198.Majorproject.controller.analytics;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.M198.Majorproject.controller.auth.AuthExceptionHandler;
import com.M198.Majorproject.service.analytics.AnalyticsService;

@ExtendWith(MockitoExtension.class)
class AnalyticsApiControllerTest {

    @Mock
    private AnalyticsService service;

    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyticsApiController(service))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void unauthorizedAnalyticsReturnsForbidden() throws Exception {
        when(service.summary(eq("course-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class)))
                .thenThrow(new AnalyticsService.AnalyticsAccessException());

        mockMvc.perform(get("/v1/analytics/courses/course-1/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingAnalyticsReturnsNotFound() throws Exception {
        when(service.summary(eq("course-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class)))
                .thenThrow(new AnalyticsService.AnalyticsNotFoundException());

        mockMvc.perform(get("/v1/analytics/courses/course-1/summary"))
                .andExpect(status().isNotFound());
    }
}
