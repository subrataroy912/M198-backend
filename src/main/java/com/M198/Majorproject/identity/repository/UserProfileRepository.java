/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : UserProfileRepository
 * PURPOSE    : Provides MongoDB access for user profile lookups and uniqueness checks.
 *
 * This repository supports profile ownership, handle validation, and profile visibility lookups
 * for authenticated users and public profile screens.
 */
package com.M198.Majorproject.identity.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.identity.entity.ProfileVisibility;
import com.M198.Majorproject.identity.entity.UserProfile;
import org.springframework.data.mongodb.repository.Query;


public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByUserIdAndDeletedAtIsNull(String userId);

    Optional<UserProfile> findByHandleNormalizedAndDeletedAtIsNull(
            String handleNormalized
    );

    List<UserProfile> findAllByUserIdInAndDeletedAtIsNull(
            Collection<String> userIds
    );

    List<UserProfile> findAllByProfileVisibilityAndDeletedAtIsNull(
            ProfileVisibility profileVisibility
    );

    Page<UserProfile> findAllByProfileVisibilityAndDeletedAtIsNull(
            ProfileVisibility profileVisibility,
            Pageable pageable
    );

    @Query("""
        {
          'profile_visibility': ?0,
          'deleted_at': null,
          '$or': [
            { 'handle_normalized': { '$regex': ?1 } },
            { 'display_name': { '$regex': ?1, '$options': 'i' } },
            { 'first_name': { '$regex': ?1, '$options': 'i' } },
            { 'last_name': { '$regex': ?1, '$options': 'i' } },
            { 'headline': { '$regex': ?1, '$options': 'i' } }
          ]
        }
        """)
    Page<UserProfile> searchPublicProfiles(
            ProfileVisibility visibility,
            String searchRegex,
            Pageable pageable
    );

    UserProfile findByUserId(String userId);
}