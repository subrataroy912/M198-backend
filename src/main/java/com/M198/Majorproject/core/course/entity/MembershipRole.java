package com.M198.Majorproject.core.course.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MembershipRole {
    OWNER,
    ADMIN,
    MEMBER;

    @JsonCreator
    public static MembershipRole fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEMBER;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "OWNER" -> OWNER;
            case "ADMIN", "TEACHER", "ASSISTANT", "TA", "INSTRUCTOR" -> ADMIN;
            case "MEMBER", "STUDENT", "LEARNER", "USER" -> MEMBER;
            default -> MEMBER;
        };
    }
}
