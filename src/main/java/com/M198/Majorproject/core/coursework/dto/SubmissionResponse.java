package com.M198.Majorproject.core.coursework.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.M198.Majorproject.core.coursework.entity.SubmissionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionResponse {

    private String id;
    private String courseworkId;
    private String courseId;
    private String studentId;
    private SubmissionStatus status;
    private String answerText;
    private Instant submittedAt;
    private Instant returnedAt;
    private boolean late;
    private BigDecimal score;
    private String graderId;
    private String feedback;
    private Instant gradedAt;
}
