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
import com.M198.Majorproject.dto.LogoutRequest;
import com.M198.Majorproject.dto.RegisterUserRequest;
import com.M198.Majorproject.dto.RefreshTokenRequest;
import com.M198.Majorproject.service.auth.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			Authentication authentication,
			@Valid @RequestBody LogoutRequest request) {
		authService.logout(authentication.getName(), request);
		return ResponseEntity.noContent().build();
	}
}
