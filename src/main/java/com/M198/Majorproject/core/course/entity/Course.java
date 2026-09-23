/**
 * CREATED BY : SUBRATA ROY
 * ENTITY     : Course
 * PURPOSE    : Stores the core course and space model for user-created learning spaces and communities.
 *
 * This document holds the course owner, title, section, subject, status, visibility,
 * and enrollment settings needed for member access and space management.
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
@CompoundIndex(name = "course_visibility_status", def = "{'visibility': 1, 'status': 1}")
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

    @Field("space_type")
    @Builder.Default
    private SpaceType spaceType = SpaceType.ACADEMIC_CLASS;

    @Field("meeting_type")
    @Builder.Default
    private MeetingType meetingType = MeetingType.IN_PERSON;

    private String location;

    private List<String> tags;

    @Field("links")
    @Builder.Default
    private List<SpaceLink> links = new java.util.ArrayList<>();

    @Builder.Default
    private CourseVisibility visibility = CourseVisibility.PRIVATE;

    @Field("access_type")
    @Builder.Default
    private CourseAccessType accessType = CourseAccessType.PUBLIC;

    @Builder.Default
    private CourseStatus status = CourseStatus.ACTIVE;

    @Field("enrollment_enabled")
    @Builder.Default
    private boolean enrollmentEnabled = true;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Field("archived_at")
    private Instant archivedAt;
}
