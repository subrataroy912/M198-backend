package com.M198.Majorproject.repository.identity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.identity.OAuthProvider;
import com.M198.Majorproject.entity.identity.UserOAuth;

public interface UserOAuthRepository extends MongoRepository<UserOAuth, String> {

    List<UserOAuth> findAllByUserId(String userId);

    Optional<UserOAuth> findByUserIdAndProvider(String userId, OAuthProvider provider);

    Optional<UserOAuth> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
}
