package com.M198.Majorproject.entity.course;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
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
@Document(collection = "enrollment_codes")
@CompoundIndex(name = "course_code_unique", def = "{'course_id': 1, 'code': 1}", unique = true)
@CompoundIndex(name = "course_active", def = "{'course_id': 1, 'active': 1}")
public class EnrollmentCode {

    @Id
    private String id;

    @Indexed
    @Field("course_id")
    private String courseId;

    @Indexed(unique = true)
    private String code;

    @Builder.Default
    private boolean active = true;

    @Field("created_by")
    private String createdBy;

    @Field("expires_at")
    private Instant expiresAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("deactivated_at")
    private Instant deactivatedAt;
}
