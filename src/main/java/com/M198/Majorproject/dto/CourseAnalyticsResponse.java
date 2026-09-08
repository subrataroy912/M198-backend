package com.M198.Majorproject.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseAnalyticsResponse {
    private String courseId;
    private long studentCount;
    private long courseworkCount;
    private long submissionCount;
    private long turnedInCount;
    private long missingCount;
    private long gradedCount;
    private BigDecimal averageScore;
    private BigDecimal onTimeSubmissionRate;
    private BigDecimal missingSubmissionRate;
    private Instant generatedAt;
}
