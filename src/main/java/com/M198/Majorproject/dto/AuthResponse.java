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
