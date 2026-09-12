package com.M198.Majorproject.dto;

import jakarta.validation.constraints.Size;

import com.M198.Majorproject.entity.course.CourseAccessType;
import com.M198.Majorproject.entity.course.CourseVisibility;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseRequest {
    @Size(max = 120)
    private String title;
    @Size(max = 80)
    private String section;
    @Size(max = 80)
    private String subject;
    @Size(max = 2000)
    private String description;
    @Size(max = 2048)
    private String coverUrl;
    @Size(max = 2048)
    private String logoUrl;
    private CourseAccessType accessType;
    private CourseVisibility visibility;
    private Boolean enrollmentEnabled;
    @Size(max = 64)
    private String theme;
}
