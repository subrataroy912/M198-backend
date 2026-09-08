package com.M198.Majorproject.entity.course;

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
@Document(collection = "course_memberships")
@CompoundIndex(name = "course_user_unique", def = "{'course_id': 1, 'user_id': 1}", unique = true)
@CompoundIndex(name = "course_status", def = "{'course_id': 1, 'status': 1}")
@CompoundIndex(name = "user_status", def = "{'user_id': 1, 'status': 1}")
public class CourseMembership {

    @Id
    private String id;

    @Indexed
    @Field("course_id")
    private String courseId;

    @Indexed
    @Field("user_id")
    private String userId;

    @Builder.Default
    private MembershipRole role = MembershipRole.STUDENT;

    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Field("joined_at")
    private Instant joinedAt;

    @Field("removed_at")
    private Instant removedAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
