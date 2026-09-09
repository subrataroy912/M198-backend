package com.M198.Majorproject.dto;

import java.time.Instant;

import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseResponse {

    private String id;
    private String ownerId;
    private String title;
    private String section;
    private String subject;
    private String description;
    private String coverUrl;
    private CourseVisibility visibility;
    private CourseStatus status;
    private boolean enrollmentEnabled;
    private String enrollmentCode;
    private Instant createdAt;
    private Instant updatedAt;
}
