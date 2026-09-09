/**
 * CREATED BY : SUBRATA ROY
 * DTO        : AuthResponse
 * PURPOSE    : Returns the issued authentication data after login, registration, or refresh.
 *
 * This response includes identity details and token values needed by the frontend to establish
 * a valid authenticated session with the backend.
 */
package com.M198.Majorproject.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class AuthResponse {
    // Security Tokens
    private String accessToken;
    private String refreshToken;
    

    private String userId;
    private String email;
    private String displayName;
    private String avatarUrl;
}
