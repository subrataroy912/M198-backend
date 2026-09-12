package com.M198.Majorproject.entity.explore;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.M198.Majorproject.entity.course.CourseAccessType;
import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;

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
@Document(collection = "course_discovery")
@CompoundIndex(name = "public_course_rank", def = "{'visibility': 1, 'status': 1, 'popularity_score': -1, 'last_activity_at': -1}")
@CompoundIndex(name = "subject_course_rank", def = "{'subject': 1, 'visibility': 1, 'status': 1, 'popularity_score': -1}")
public class CourseDiscovery {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("course_id")
    private String courseId;

    private String title;
    private String subject;
    private List<String> tags;
    private String coverUrl;
    private String logoUrl;

    @Builder.Default
    private CourseVisibility visibility = CourseVisibility.PRIVATE;

    @Builder.Default
    private CourseAccessType accessType = CourseAccessType.OPEN;

    @Builder.Default
    private CourseStatus status = CourseStatus.ACTIVE;

    @Field("enrollment_count")
    @Builder.Default
    private long enrollmentCount = 0;

    @Field("popularity_score")
    @Builder.Default
    private double popularityScore = 0;

    @Field("last_activity_at")
    private Instant lastActivityAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
