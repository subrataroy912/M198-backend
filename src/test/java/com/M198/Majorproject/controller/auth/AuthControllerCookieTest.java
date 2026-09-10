package com.M198.Majorproject.controller.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.ResponseEntity;

import com.M198.Majorproject.dto.AuthResponse;
import com.M198.Majorproject.service.auth.AuthService;

import jakarta.servlet.http.Cookie;

class AuthControllerCookieTest {

    @Test
    void refreshUsesCookieTokenWhenRequestBodyIsMissing() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "cookie-refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthResponse issued = AuthResponse.builder()
                .accessToken("new-access-token")
                .userId("user-1")
                .email("student@example.com")
                .displayName("Student User")
                .build();

        when(authService.refresh(eq("cookie-refresh-token"))).thenReturn(issued);

        ResponseEntity<AuthResponse> result = controller.refresh(request, response);

        assertEquals(200, result.getStatusCode().value());
        assertEquals("new-access-token", result.getBody().getAccessToken());
        verify(authService).setRefreshCookie(response, null);
        verify(authService).refresh(eq("cookie-refresh-token"));
    }
}
