package com.M198.Majorproject.core.course.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteTokenResponse {
    private String token;
    private String inviteUrl;
    private Instant expiresAt;
    private boolean expired;
}
