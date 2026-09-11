package com.M198.Majorproject.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherGradebookResponse {
    private String id;
    private String studentId;
    private String studentName;
    private String avatar;
    private String avatarUrl;
    private String average;
    private BigDecimal averageScore;
    private long missingCount;
    private long submittedCount;
}
