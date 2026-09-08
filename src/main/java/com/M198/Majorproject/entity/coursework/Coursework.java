package com.M198.Majorproject.entity.coursework;

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
@Document(collection = "coursework")
@CompoundIndex(name = "course_status_published", def = "{'course_id': 1, 'status': 1, 'published_at': -1}")
@CompoundIndex(name = "course_type_status", def = "{'course_id': 1, 'type': 1, 'status': 1}")
public class Coursework {

    @Id
    private String id;

    @Indexed
    @Field("course_id")
    private String courseId;

    @Indexed
    @Field("creator_id")
    private String creatorId;

    @Builder.Default
    private CourseworkType type = CourseworkType.ANNOUNCEMENT;

    private String title;
    private String description;

    @Builder.Default
    private CourseworkStatus status = CourseworkStatus.DRAFT;

    @Field("published_at")
    private Instant publishedAt;

    @Field("due_at")
    private Instant dueAt;

    @Field("maximum_points")
    private Integer maximumPoints;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Field("archived_at")
    private Instant archivedAt;
}
