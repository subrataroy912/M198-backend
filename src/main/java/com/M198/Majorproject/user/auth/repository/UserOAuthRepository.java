package com.M198.Majorproject.user.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.user.auth.entity.OAuthProvider;
import com.M198.Majorproject.user.auth.entity.UserOAuth;

public interface UserOAuthRepository extends MongoRepository<UserOAuth, String> {

    List<UserOAuth> findAllByUserId(String userId);

    Optional<UserOAuth> findByUserIdAndProvider(String userId, OAuthProvider provider);

    Optional<UserOAuth> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
