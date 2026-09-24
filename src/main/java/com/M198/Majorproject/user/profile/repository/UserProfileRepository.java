/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : UserProfileRepository
 * PURPOSE    : Provides MongoDB access for user profile lookups and uniqueness checks.
 */
package com.M198.Majorproject.user.profile.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.profile.entity.UserProfile;

@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

        Optional<UserProfile> findByUserId(String userId);

        Optional<UserProfile> findByUserIdAndDeletedAtIsNull(String userId);

        Optional<UserProfile> findByHandle(String handle);

        Optional<UserProfile> findByHandleIgnoreCase(String handle);

        List<UserProfile> findAllByUserIdIn(Collection<String> userIds);

        List<UserProfile> findAllByUserIdInAndDeletedAtIsNull(Collection<String> userIds);

        List<UserProfile> findAllByProfileVisibility(ProfileVisibility profileVisibility);

        List<UserProfile> findAllByProfileVisibilityAndDeletedAtIsNull(ProfileVisibility profileVisibility);

        Page<UserProfile> findAllByProfileVisibilityAndDeletedAtIsNull(
                        ProfileVisibility profileVisibility,
                        Pageable pageable);

        @Query("{ 'profile_visibility': ?0, 'deleted_at': null, $or: [ "
                        + "{ 'handle': { $regex: ?1, $options: 'i' } }, "
                        + "{ 'display_name': { $regex: ?1, $options: 'i' } }, "
                        + "{ 'first_name': { $regex: ?1, $options: 'i' } }, "
                        + "{ 'last_name': { $regex: ?1, $options: 'i' } }, "
                        + "{ 'headline': { $regex: ?1, $options: 'i' } } "
                        + "] }")
        Page<UserProfile> searchPublicProfiles(
                        ProfileVisibility visibility,
                        String searchRegex,
                        Pageable pageable);
}