/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : UserProfileRepository
 * PURPOSE    : Provides MongoDB access for user profile lookups and uniqueness checks.
 *
 * This repository supports profile ownership, handle validation, and profile visibility lookups
 * for authenticated users and public profile screens.
 */
package com.M198.Majorproject.repository.identity;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.identity.UserProfile;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByHandle(String handle);
}
