package com.M198.Majorproject.entity.comment;

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
@Document(collection = "comments")
@CompoundIndex(name = "target_visibility_created", def = "{'target_type': 1, 'target_id': 1, 'visibility': 1, 'created_at': -1}")
@CompoundIndex(name = "course_target", def = "{'course_id': 1, 'target_type': 1, 'target_id': 1}")
public class Comment {

    @Id
    private String id;

    @Indexed
    @Field("course_id")
    private String courseId;

    @Indexed
    @Field("author_id")
    private String authorId;

    @Indexed
    @Field("target_type")
    private CommentTargetType targetType;

    @Indexed
    @Field("target_id")
    private String targetId;

    @Builder.Default
    private CommentVisibility visibility = CommentVisibility.PUBLIC;

    private String body;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Field("edited_at")
    private Instant editedAt;

    @Field("deleted_at")
    private Instant deletedAt;
}
