package com.M198.Majorproject.core.course.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CourseAccessType {
    PUBLIC,
    PRIVATE,
    LINK_ONLY;

    @JsonCreator
    public static CourseAccessType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PUBLIC;
        }
        return switch (value.trim().toUpperCase()) {
            case "PUBLIC", "OPEN" -> PUBLIC;
            case "PRIVATE" -> PRIVATE;
            case "LINK_ONLY", "LINK", "CODE", "INVITE" -> LINK_ONLY;
            default -> PUBLIC;
        };
    }
}
