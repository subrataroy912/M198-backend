package com.M198.Majorproject.entity.submission;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@Document(collection = "submissions")
@CompoundIndex(name = "coursework_student_unique", def = "{'coursework_id': 1, 'student_id': 1}", unique = true)
@CompoundIndex(name = "coursework_status", def = "{'coursework_id': 1, 'status': 1}")
@CompoundIndex(name = "student_coursework", def = "{'student_id': 1, 'coursework_id': 1}")
public class Submission {

    @Id
    private String id;

    @Indexed
    @Field("coursework_id")
    private String courseworkId;

    @Indexed
    @Field("course_id")
    private String courseId;

    @Indexed
    @Field("student_id")
    private String studentId;

    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    @Field("answer_text")
    private String answerText;

    @Field("submitted_at")
    private Instant submittedAt;

    @Field("returned_at")
    private Instant returnedAt;

    @Builder.Default
    private boolean late = false;

    private BigDecimal score;

    @Field("grader_id")
    private String graderId;

    private String feedback;

    @Field("graded_at")
    private Instant gradedAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
