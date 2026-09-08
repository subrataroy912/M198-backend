package com.M198.Majorproject.dto;

import java.time.Instant;

import com.M198.Majorproject.entity.coursework.CourseworkStatus;
import com.M198.Majorproject.entity.coursework.CourseworkType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseworkResponse {

    private String id;
    private String courseId;
    private String creatorId;
    private CourseworkType type;
    private String title;
    private String description;
    private CourseworkStatus status;
    private Instant publishedAt;
    private Instant dueAt;
    private Integer maximumPoints;
    private Instant createdAt;
    private Instant updatedAt;
}
