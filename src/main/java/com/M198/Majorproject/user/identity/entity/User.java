/**
 * CREATED BY : SUBRATA ROY
 * ENTITY     : User
 * PURPOSE    : Represents the core user account in the platform.
 *
 * This document stores the identity and account state of every user.
 * Authentication, role handling, account status, and OAuth linking are tied to this entity.
 */
package com.M198.Majorproject.user.identity.entity;

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
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Field("password_hash")
    private String passwordHash;

    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING;

    @Field("can_create_courses")
    @Builder.Default
    private boolean canCreateCourses = false;

    @Field("is_admin")
    @Builder.Default
    private boolean isAdmin = false;

    @Field("is_active")
    private boolean active;
    @Field("is_verified")
    private boolean verified;

    @Field("last_login_at")
    private Instant lastLoginAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("deleted_at")
    private Instant deletedAt;
    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
