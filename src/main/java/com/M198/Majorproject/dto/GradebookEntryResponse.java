package com.M198.Majorproject.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.M198.Majorproject.entity.submission.SubmissionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradebookEntryResponse {
    private String courseworkId;
    private String title;
    private Integer maximumPoints;
    private BigDecimal score;
    private SubmissionStatus status;
    private String feedback;
    private Instant dueAt;
    private Instant gradedAt;
}
