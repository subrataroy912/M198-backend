package com.M198.Majorproject.dto;

import java.time.Instant;

import com.M198.Majorproject.entity.course.MembershipRole;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseMemberResponse {
    private String userId;
    private MembershipRole role;
    private Instant joinedAt;
    private String name;
    private String avatarUrl;
}
