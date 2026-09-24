/**
 * CREATED BY : SUBRATA ROY
 * ENTITY     : Course
 */
package com.M198.Majorproject.core.course.entity;

import java.time.Instant;
import java.util.List;

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
@Document(collection = "courses")
@CompoundIndex(name = "course_owner_status", def = "{'owner_id': 1, 'status': 1}")
public class Course {

    @Id
    private String id;

    @Indexed
    @Field("owner_id")
    private String ownerId;

    private String title;
    private String section;
    private String subject;
    private String description;
    private String coverUrl;
    private String logoUrl;
    private String theme;

    private List<String> tags;

    @Field("links")
    @Builder.Default
    private List<SpaceLink> links = new java.util.ArrayList<>();

    @Field("access_type")
    @Builder.Default
    private CourseAccessType accessType = CourseAccessType.PUBLIC;

    @Builder.Default
    private CourseStatus status = CourseStatus.ACTIVE;

    @Field("enrollment_enabled")
    @Builder.Default
    private Boolean enrollmentEnabled = true;

    public boolean isEnrollmentEnabled() {
        return enrollmentEnabled == null || Boolean.TRUE.equals(enrollmentEnabled);
    }

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Field("archived_at")
    private Instant archivedAt;

    public CourseAccessType getResolvedAccessType() {
        return this.accessType != null ? this.accessType : CourseAccessType.PUBLIC;
    }

    public CourseVisibility getVisibility() {
        return getResolvedAccessType() == CourseAccessType.PUBLIC
                ? CourseVisibility.PUBLIC
                : CourseVisibility.PRIVATE;
    }
}
