package com.M198.Majorproject.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.identity.entity.OAuthProvider;
import com.M198.Majorproject.identity.entity.UserOAuth;

public interface UserOAuthRepository extends MongoRepository<UserOAuth, String> {

    List<UserOAuth> findAllByUserId(String userId);

    Optional<UserOAuth> findByUserIdAndProvider(String userId, OAuthProvider provider);

    Optional<UserOAuth> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
