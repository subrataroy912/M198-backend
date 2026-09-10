/**
 * CREATED BY : SUBRATA ROY
 * DTO        : AuthResponse
 * PURPOSE    : Returns the issued authentication data after login, registration, or refresh.
 *
 * This response includes identity details and token values needed by the frontend to establish
 * a valid authenticated session with the backend.
 */
package com.M198.Majorproject.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@ Getter 

    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class AuthResponse {

        // Security Tokens
        private String accessToken;
        private String refreshToken;

        private String userId;
        private String email;
        private String displayName;
        private String avatarUrl;
    }
