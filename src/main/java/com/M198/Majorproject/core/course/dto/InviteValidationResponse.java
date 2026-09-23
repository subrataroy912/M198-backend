package com.M198.Majorproject.core.course.dto;

import java.time.Instant;

import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.SpaceType;

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
public class InviteValidationResponse {
    private String courseId;
    private String title;
    private String section;
    private String subject;
    private String description;
    private String coverUrl;
    private String logoUrl;
    private String theme;
    private SpaceType spaceType;
    private CourseAccessType accessType;
    private long memberCount;
    private String ownerName;
    private String ownerAvatarUrl;
    private Instant expiresAt;
    private boolean expired;
}
