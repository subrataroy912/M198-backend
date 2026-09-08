package com.M198.Majorproject.dto;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.M198.Majorproject.entity.coursework.CourseworkType;

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
}
