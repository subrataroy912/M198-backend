package com.M198.Majorproject.core.course.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CourseAccessType {
    PUBLIC,
    PRIVATE,
    LINK_ONLY,
    @Deprecated OPEN,
    @Deprecated CODE,
    @Deprecated INVITE;

    @JsonCreator
    public static CourseAccessType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PUBLIC;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PUBLIC", "OPEN" -> PUBLIC;
            case "PRIVATE" -> PRIVATE;
            case "LINK_ONLY", "LINK" -> LINK_ONLY;
            case "INVITE" -> INVITE;
            case "CODE" -> CODE;
            default -> PUBLIC;
        };
    }
}
