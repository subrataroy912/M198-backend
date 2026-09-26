package com.M198.Majorproject.user.auth.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
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
@Document(collection = "pending_registrations")
public class PendingRegistration {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Field("password_hash")
    private String passwordHash;

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    @Field("auth_type")
    @Builder.Default
    private String authType = "LOCAL";

    private OAuthProvider provider;

    @Field("provider_user_id")
    private String providerUserId;

    @Field("avatar_url")
    private String avatarUrl;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Indexed(expireAfter = "0s")
    @Field("expires_at")
    private Instant expiresAt;
}
