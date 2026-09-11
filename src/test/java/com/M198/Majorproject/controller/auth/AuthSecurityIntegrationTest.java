package com.M198.Majorproject.controller.auth;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationServiceException;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.M198.Majorproject.dto.AuthResponse;
import com.M198.Majorproject.service.auth.AuthService;

import jakarta.servlet.http.HttpServletResponse;

@SpringBootTest(properties = {
    "app.jwt.secret=test-secret-that-is-long-enough-32",
    "app.cookies.secure=true",
    "app.cookies.same-site=Lax"
})
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void refreshWithValidCookieReachesServiceWithoutCsrfHeader() throws Exception {
        AuthResponse response = authResponse();
        when(authService.refresh(eq("valid-refresh-token"))).thenReturn(response);
        doNothing().when(authService).setRefreshCookie(org.mockito.ArgumentMatchers.any(), eq("new-refresh-token"));

        mockMvc.perform(post("/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));

        verify(authService).refresh("valid-refresh-token");
    }

    @Test
    void refreshWithoutCookieFailsAuthentication() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Refresh token is required"));
    }

    @Test
    void refreshWithInvalidCookieFailsAuthentication() throws Exception {
        when(authService.refresh(eq("invalid-refresh-token")))
                .thenThrow(new AuthenticationServiceException("Invalid refresh token"));

        mockMvc.perform(post("/v1/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void normalAuthenticatedPostStillRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/v1/courses")
                .with(user("user-1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginRemainsCsrfExemptAndSetsHttpOnlyRefreshCookie() throws Exception {
        when(authService.login(org.mockito.ArgumentMatchers.any())).thenReturn(authResponse());
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE,
                    "refreshToken=new-refresh-token; Path=/; Max-Age=604800; Secure; HttpOnly; SameSite=Lax");
            return null;
        }).when(authService).setRefreshCookie(org.mockito.ArgumentMatchers.any(), eq("new-refresh-token"));

        mockMvc.perform(post("/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"student@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true));
    }

    @Test
    void logoutWithoutCsrfHeaderSucceedsForAuthenticatedUserAndClearsRefreshCookie() throws Exception {
        doNothing().when(authService).logout(eq("user-1"), eq("valid-refresh-token"));
        doAnswer(invocation -> {
            HttpServletResponse response = invocation.getArgument(0);
            response.addHeader(HttpHeaders.SET_COOKIE, "refreshToken=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Lax");
            return null;
        }).when(authService).clearRefreshCookie(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/v1/auth/logout")
                .with(user("user-1"))
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isNoContent());

        verify(authService).logout("user-1", "valid-refresh-token");
        verify(authService).clearRefreshCookie(org.mockito.ArgumentMatchers.any());
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .userId("user-1")
                .email("student@example.com")
                .displayName("Student User")
                .build();
    }
}
