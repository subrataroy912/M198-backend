package com.M198.Majorproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.M198.Majorproject.entity.course.CourseVisibility;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 80)
    private String section;

    @Size(max = 80)
    private String subject;

    @Size(max = 2000)
    private String description;

    private CourseVisibility visibility;

    private com.M198.Majorproject.entity.course.CourseAccessType accessType;

    @Size(max = 2048)
    private String coverUrl;

    @Size(max = 2048)
    private String logoUrl;
}
