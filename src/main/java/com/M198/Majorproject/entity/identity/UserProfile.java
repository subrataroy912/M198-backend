package com.M198.Majorproject.entity.identity;

import java.time.Instant;

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
@Document(collection = "user_profiles")
public class UserProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    @Field("display_name")
    private String displayName;

    private String headline;

    private String about;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("banner_url")
    private String bannerUrl;

    private String city;

    private String country;

    @Field("profile_visibility")
    @Builder.Default
    private ProfileVisibility profileVisibility = ProfileVisibility.PRIVATE;

    @Field("grade_level")
    private String gradeLevel;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Field("deleted_at")
    private Instant deletedAt;
}
