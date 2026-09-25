package com.M198.Majorproject.user.profile.entity;

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

    @Indexed(unique = true, sparse = true)
    private String handle;

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

    private String phone;

    private String gender;

    @Field("date_of_birth")
    private String dateOfBirth;

    private String address;

    @Field("profile_visibility")
    @Builder.Default
    private ProfileVisibility profileVisibility = ProfileVisibility.PRIVATE;

    @Field("can_create_courses")
    @Builder.Default
    private boolean canCreateCourses = false;

    @Field("is_admin")
    @Builder.Default
    private boolean isAdmin = false;

    @Field("profile_completed")
    @Builder.Default
    private boolean profileCompleted = false;

    @Field("links")
    @Builder.Default
    private java.util.List<ProfileLink> links = new java.util.ArrayList<>();

    @Field("tags")
    @Builder.Default
    private java.util.List<String> tags = new java.util.ArrayList<>();

    @Field("handle_updated_timestamps")
    @Builder.Default
    private java.util.List<Instant> handleUpdatedTimestamps = new java.util.ArrayList<>();

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Field("last_active_at")
    private Instant lastActiveAt;

    @Field("deleted_at")
    private Instant deletedAt;
}
