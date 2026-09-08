package com.M198.Majorproject.dto;

import jakarta.validation.constraints.Size;

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
    private CourseVisibility visibility;
    private Boolean enrollmentEnabled;
}
