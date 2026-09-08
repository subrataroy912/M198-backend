package com.M198.Majorproject.entity.analytics;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.M198.Majorproject.entity.submission.SubmissionStatus;

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
@Document(collection = "student_gradebook_entries")
@CompoundIndex(name = "course_student_coursework_unique", def = "{'course_id': 1, 'student_id': 1, 'coursework_id': 1}", unique = true)
@CompoundIndex(name = "course_student_due", def = "{'course_id': 1, 'student_id': 1, 'due_at': 1}")
public class StudentGradebookEntry {

    @Id
    private String id;

    @Field("course_id")
    private String courseId;

    @Field("student_id")
    private String studentId;

    @Field("coursework_id")
    private String courseworkId;

    private String title;

    @Field("maximum_points")
    private Integer maximumPoints;

    private BigDecimal score;

    private SubmissionStatus status;
    private String feedback;

    @Field("due_at")
    private Instant dueAt;

    @Field("graded_at")
    private Instant gradedAt;

    @Field("generated_at")
    private Instant generatedAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
