package com.M198.Majorproject.entity.identity;

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
@Document(collection = "user_oauth")
@CompoundIndex(name = "user_provider_unique", def = "{'user_id': 1, 'provider': 1}", unique = true)
@CompoundIndex(name = "provider_identity_unique", def = "{'provider': 1, 'provider_user_id': 1}", unique = true)
public class UserOAuth {

    @Id
    private String id;

    @Indexed
    @Field("user_id")
    private String userId;

    private OAuthProvider provider;

    @Field("provider_user_id")
    private String providerUserId;

    @Field("access_token")
    private String accessToken;

    @Field("refresh_token")
    private String refreshToken;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;
}
