package com.M198.Majorproject.core.course.dto;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.M198.Majorproject.core.course.entity.CourseworkStatus;
import com.M198.Majorproject.core.course.entity.CourseworkType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseworkRequest {

    @NotNull
    private CourseworkType type;

    @NotBlank
    @Size(max = 160)
    private String title;

    @Size(max = 10000)
    private String description;

    private Instant dueAt;

    @Min(0)
    private Integer maximumPoints;

    /**
     * Optional. When set to PUBLISHED the item is immediately visible to members
     * and publishedAt is recorded. Defaults to DRAFT when omitted or null.
     * ARCHIVED is not a valid value on creation.
     */
    private CourseworkStatus status;
}
