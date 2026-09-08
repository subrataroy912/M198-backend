package com.M198.Majorproject.entity.identity;

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
@Document(collection = "auth_refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("token_hash")
    private String tokenHash;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed(expireAfter = "0s")
    @Field("expires_at")
    private Instant expiresAt;

    @Field("revoked_at")
    private Instant revokedAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;
}