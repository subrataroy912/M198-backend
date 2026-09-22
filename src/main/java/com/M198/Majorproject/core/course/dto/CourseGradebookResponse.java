package com.M198.Majorproject.core.course.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseGradebookResponse {
    private String id;
    private String memberId;
    private String memberName;
    private String studentId;
    private String studentName;
    private String avatar;
    private String avatarUrl;
    private String average;
    private BigDecimal averageScore;
    private long missingCount;
    private long submittedCount;

    public void setMemberId(String memberId) {
        this.memberId = memberId;
        this.studentId = memberId;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
        this.studentName = memberName;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
        if (this.memberId == null) {
            this.memberId = studentId;
        }
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
        if (this.memberName == null) {
            this.memberName = studentName;
        }
    }
}
