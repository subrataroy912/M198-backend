package com.M198.Majorproject.core.course.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.M198.Majorproject.core.course.entity.CourseworkAttachment;
import com.M198.Majorproject.core.course.entity.CourseworkStatus;
import com.M198.Majorproject.core.course.entity.CourseworkType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseworkResponse {

    private String id;
    private String courseId;
    private String creatorId;
    private String creatorName;
    private String creatorAvatarUrl;
    private String creatorHandle;
    private CourseworkType type;
    private String title;
    private String description;
    private CourseworkStatus status;
    private boolean pinned;
    private List<CourseworkAttachment> attachments = new ArrayList<>();
    private Instant publishedAt;
    private Instant dueAt;
    private Integer maximumPoints;
    private String temporalStatus;
    private long submittedCount;
    private long totalCount;
    private Instant createdAt;
    private Instant updatedAt;

    public String getDueDate() {
        return dueAt != null ? dueAt.toString() : null;
    }

    public Integer getPointsPossible() {
        return maximumPoints;
    }
}
