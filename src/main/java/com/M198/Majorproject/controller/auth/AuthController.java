/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
	 * ==========================================
	 * API Functions inside the Auth Controller:
	 * ==========================================
	 * 1. registerUser - Registers a new user account with a TEACHER or STUDENT role
	 * 2. loginUser    - Validates credentials and issues an access token
	 * 3. logoutUser   - Invalidates the current authenticated session
 */

/*
* =================================================================================
* AUTH CONTROLLER ENDPOINT DOCUMENTATION
* =================================================================================
*
* 1. registerUser
*    - Route: POST /v1/auth/register
*    - Role Allowed: Public; the caller must not already be authenticated.
*    - Request Body: Name, email, password, and account role (TEACHER or STUDENT).
*    - How it works: Validates the registration data, checks that the email is not
*      already registered, hashes the password, and stores the new user account.
*    - Response: Returns the created user's safe profile data. Never return the
*      password or its hash.
*    - Why it's used: Creates an account before the user joins or creates courses.
*
* 2. loginUser
*    - Route: POST /v1/auth/login
*    - Role Allowed: Public.
*    - Request Body: Registered email and password.
*    - How it works: Looks up the account, verifies the password hash, and issues
*      an access token containing the authenticated user's identity and role.
*    - Response: Returns an access token and the minimum user information required
*      by the client. Invalid credentials must produce the same generic error.
*    - Why it's used: Establishes the authenticated session for protected APIs.
*
* 3. logoutUser
*    - Route: POST /v1/auth/logout
*    - Role Allowed: Authenticated users.
*    - Request: The current access token, normally supplied in the Authorization
*      header. A refresh token may also need to be revoked if one is implemented.
*    - How it works: Invalidates the current session or adds the token to a
*      server-side blocklist until it expires.
*    - Response: Returns a successful empty response after the session is closed.
*    - Why it's used: Prevents a logged-in client from continuing to use its token.
*/
/**
 * CREATED BY : SUBRATA ROY
 * CONTROLLER : AuthController
 * PURPOSE    : Exposes public authentication endpoints for registration, login,
 *              refresh-token rotation, and logout.
 *
 * This is the first layer where client requests enter the app for identity-related work.
 * It validates incoming DTOs and delegates the actual business logic to AuthService.
 */
package com.M198.Majorproject.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.dto.AuthResponse;
import com.M198.Majorproject.dto.LoginRequest;
import com.M198.Majorproject.dto.RegisterUserRequest;
import com.M198.Majorproject.service.auth.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request,
            HttpServletResponse response, CsrfToken csrfToken) {
        csrfToken.getToken();
        AuthResponse authResponse = authService.register(request);
        authService.setRefreshCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response,
            CsrfToken csrfToken) {
        csrfToken.getToken();
        AuthResponse authResponse = authService.login(request);
        authService.setRefreshCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String refreshToken = resolveRefreshToken(request);
            AuthResponse authResponse = authService.refresh(refreshToken);
            authService.setRefreshCookie(response, authResponse.getRefreshToken());
            authResponse.setRefreshToken(null);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            // Return a clean 401 instead of letting a 400 bubble up
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        authService.logout(authentication.getName(), refreshToken);
        authService.clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String resolveRefreshToken(HttpServletRequest servletRequest) {
        if (servletRequest.getCookies() != null) {
            for (Cookie cookie : servletRequest.getCookies()) {
                if (AuthService.REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        throw new IllegalArgumentException("Refresh token is required");
    }
}
