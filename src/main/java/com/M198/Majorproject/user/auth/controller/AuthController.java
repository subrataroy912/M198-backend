/**
 * CREATED BY : SUBRATA ROY
 * CREATED AT : 09/04/2026 (M-D-Y)
 */
package com.M198.Majorproject.user.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.user.auth.dto.AuthResponse;
import com.M198.Majorproject.user.auth.dto.CompleteOnboardingRequest;
import com.M198.Majorproject.user.auth.dto.LoginRequest;
import com.M198.Majorproject.user.auth.dto.RegisterUserRequest;
import com.M198.Majorproject.user.auth.exception.RefreshTokenException;
import com.M198.Majorproject.user.auth.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterUserRequest request,
            HttpServletResponse response,
            CsrfToken csrfToken) {
        exposeCsrfToken(response, csrfToken);
        AuthResponse authResponse = authService.register(request);
        authService.setRefreshCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/onboarding/complete")
    public ResponseEntity<AuthResponse> completeOnboarding(
            @Valid @RequestBody CompleteOnboardingRequest request,
            Authentication authentication,
            HttpServletResponse response,
            CsrfToken csrfToken) {
        exposeCsrfToken(response, csrfToken);
        String pendingUserId = authentication.getName();
        AuthResponse authResponse = authService.completeOnboarding(pendingUserId, request);
        authService.setRefreshCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/onboarding/cancel")
    public ResponseEntity<Void> cancelOnboarding(
            Authentication authentication,
            HttpServletResponse response) {
        if (authentication != null && authentication.isAuthenticated()) {
            authService.cancelOnboarding(authentication.getName());
        }
        authService.clearRefreshCookie(response);
        authService.clearCsrfCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response,
            CsrfToken csrfToken) {
        exposeCsrfToken(response, csrfToken);
        AuthResponse authResponse = authService.login(request);
        authService.setRefreshCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request, body);
        AuthResponse authResponse = authService.refresh(refreshToken);
        authService.setRefreshCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = null;
        try {
            refreshToken = resolveRefreshToken(request, body);
        } catch (RefreshTokenException ignored) {
            // Logout is idempotent — missing token is fine
        }
        if (refreshToken != null && authentication != null && authentication.isAuthenticated()) {
            authService.logout(authentication.getName(), refreshToken);
        }
        authService.clearRefreshCookie(response);
        authService.clearCsrfCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void exposeCsrfToken(HttpServletResponse response, CsrfToken csrfToken) {
        if (csrfToken != null) {
            String token = csrfToken.getToken();
            if (token != null) {
                response.setHeader(csrfToken.getHeaderName(), token);
            }
        } else {
            authService.setCsrfCookie(response);
        }
    }

    private String resolveRefreshToken(
            HttpServletRequest request,
            Map<String, String> body) {

        String refreshToken = body != null
                ? body.get("refreshToken")
                : null;

        if (refreshToken != null && !refreshToken.isBlank()) {
            return refreshToken;
        }

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthService.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        throw new RefreshTokenException("Refresh token is required");
    }
}
