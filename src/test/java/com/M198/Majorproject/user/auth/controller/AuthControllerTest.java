package com.M198.Majorproject.user.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import com.M198.Majorproject.user.auth.dto.AuthResponse;
import com.M198.Majorproject.user.auth.dto.LoginRequest;
import com.M198.Majorproject.user.auth.dto.RegisterUserRequest;
import com.M198.Majorproject.user.auth.service.AuthService;

class AuthControllerTest {

    private AuthService authService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        controller = new AuthController(authService);
    }

    private LoginRequest createLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        return request;
    }

    private RegisterUserRequest createRegisterRequest() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("securePassword123");
        request.setFirstName("New");
        request.setLastName("User");
        return request;
    }

    @Test
    void loginWithCsrfTokenCallsGetToken() {
        LoginRequest request = createLoginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken csrfToken = mock(CsrfToken.class);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponse> result = controller.login(request, response, csrfToken);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("access-token-123", result.getBody().getAccessToken());

        verify(csrfToken).getToken();
        verify(authService).setRefreshCookie(eq(response), eq("refresh-token-123"));
    }

    @Test
    void loginWithNullCsrfTokenDoesNotThrowAndSetsCsrfCookie() {
        LoginRequest request = createLoginRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Pass null for csrfToken — should not throw NullPointerException
        ResponseEntity<AuthResponse> result = controller.login(request, response, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(authService).setCsrfCookie(eq(response));
        verify(authService).setRefreshCookie(eq(response), eq("refresh-token-123"));
    }

    @Test
    void registerWithCsrfTokenCallsGetToken() {
        RegisterUserRequest request = createRegisterRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken csrfToken = mock(CsrfToken.class);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token-new")
                .refreshToken("refresh-token-new")
                .build();

        when(authService.register(any(RegisterUserRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponse> result = controller.register(request, response, csrfToken);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(csrfToken).getToken();
        verify(authService).setRefreshCookie(eq(response), eq("refresh-token-new"));
    }

    @Test
    void registerWithNullCsrfTokenDoesNotThrowAndSetsCsrfCookie() {
        RegisterUserRequest request = createRegisterRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token-new")
                .refreshToken("refresh-token-new")
                .build();

        when(authService.register(any(RegisterUserRequest.class))).thenReturn(authResponse);

        // Pass null for csrfToken — should not throw NullPointerException
        ResponseEntity<AuthResponse> result = controller.register(request, response, null);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(authService).setCsrfCookie(eq(response));
        verify(authService).setRefreshCookie(eq(response), eq("refresh-token-new"));
    }
}
