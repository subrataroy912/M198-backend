package com.M198.Majorproject.core.course.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import com.M198.Majorproject.core.course.entity.CourseworkAttachment;
import com.M198.Majorproject.core.course.entity.CourseworkStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseworkRequest {

    @Size(max = 160)
    private String title;

    @Size(max = 10000)
    private String description;

    private Instant dueAt;

    @Min(0)
    private Integer maximumPoints;

    private CourseworkStatus status;

    private Boolean pinned;

    private List<CourseworkAttachment> attachments;
}
