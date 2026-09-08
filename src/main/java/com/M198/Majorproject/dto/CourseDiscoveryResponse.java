package com.M198.Majorproject.dto;

import java.time.Instant;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseDiscoveryResponse {
    private String courseId;
    private String title;
    private String subject;
    private List<String> tags;
    private long enrollmentCount;
    private double popularityScore;
    private Instant lastActivityAt;
}
