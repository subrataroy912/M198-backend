package com.M198.Majorproject.entity.analytics;

import java.time.Instant;
import java.math.BigDecimal;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "course_analytics_summaries")
public class CourseAnalyticsSummary {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("course_id")
    private String courseId;

    @Field("student_count")
    private long studentCount;

    @Field("coursework_count")
    private long courseworkCount;

    @Field("submission_count")
    private long submissionCount;

    @Field("turned_in_count")
    private long turnedInCount;

    @Field("missing_count")
    private long missingCount;

    @Field("graded_count")
    private long gradedCount;

    @Field("average_score")
    private BigDecimal averageScore;

    @Field("on_time_submission_rate")
    private BigDecimal onTimeSubmissionRate;

    @Field("missing_submission_rate")
    private BigDecimal missingSubmissionRate;

    @Field("generated_at")
    private Instant generatedAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
