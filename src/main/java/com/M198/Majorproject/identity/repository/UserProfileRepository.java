/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : UserProfileRepository
 * PURPOSE    : Provides MongoDB access for user profile lookups and uniqueness checks.
 *
 * This repository supports profile ownership, handle validation, and profile visibility lookups
 * for authenticated users and public profile screens.
 */
package com.M198.Majorproject.identity.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.identity.entity.ProfileVisibility;
import com.M198.Majorproject.identity.entity.UserProfile;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByHandle(String handle);

    java.util.List<UserProfile> findAllByUserIdIn(java.util.Collection<String> userIds);

    java.util.List<UserProfile> findAllByProfileVisibility(ProfileVisibility profileVisibility);

    java.util.List<UserProfile> findAllByProfileVisibilityAndDeletedAtIsNull(ProfileVisibility profileVisibility);
}
