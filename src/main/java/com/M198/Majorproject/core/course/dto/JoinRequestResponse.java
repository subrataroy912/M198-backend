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
public class JoinRequestResponse {
    private String id;
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String email;
    private Instant requestedAt;
}
