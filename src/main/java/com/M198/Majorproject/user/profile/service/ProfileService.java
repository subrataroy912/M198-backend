/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : ProfileService
 * PURPOSE    : Orchestrates user profile operations including lookup, visibility rules,
 *              updates, creator unlock, and public discovery.
 */
package com.M198.Majorproject.user.profile.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;
import com.M198.Majorproject.user.identity.repository.UserRepository;
import com.M198.Majorproject.user.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.user.profile.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.user.profile.dto.UserProfileResponse;
import com.M198.Majorproject.user.profile.exception.HandleConflictException;
import com.M198.Majorproject.user.profile.exception.ProfileNotFoundException;
import com.M198.Majorproject.user.profile.mapper.ProfileMapper;
import com.M198.Majorproject.user.profile.security.AuthenticatedUserResolver;
import com.M198.Majorproject.user.profile.security.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final CourseRepository courseRepository;
    private final CourseMembershipRepository courseMembershipRepository;
    private final ProfileMapper profileMapper;
    private final AuthenticatedUserResolver userResolver;
    private final ProfilePatcher profilePatcher;
    private final HandleChangePolicy handleChangePolicy;
    private final MediaStorageService mediaStorageService;

    public UserProfileResponse getMyProfile(Authentication authentication)
    {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        UserProfileResponse response = profileMapper.toOwnerResponse(context.user(), context.profile());
        enrichOwnerProfile(response, context.user(), context.profile());
        return response;
    }

    public List<PublicUserProfileResponse> getPublicProfiles()
    {
        List<UserProfile> profiles = profileRepository.findAllByProfileVisibilityAndDeletedAtIsNull(ProfileVisibility.PUBLIC);
        return profiles.stream()
                .map(this::mapAndEnrichPublicProfile)
                .toList();
    }

    public Page<PublicUserProfileResponse> getPublicProfiles(
            String query,
            Pageable pageable)
    {
        Page<UserProfile> profilesPage;
        if (query != null && !query.trim().isBlank()) {
            profilesPage = profileRepository.searchPublicProfiles(ProfileVisibility.PUBLIC, query.trim(), pageable);
        } else {
            profilesPage = profileRepository.findAllByProfileVisibilityAndDeletedAtIsNull(ProfileVisibility.PUBLIC, pageable);
        }
        return profilesPage.map(this::mapAndEnrichPublicProfile);
    }

    public PublicUserProfileResponse getUserProfile(
            String identifier,
            Authentication authentication)
    {
        UserContext context = userResolver.resolveUserByIdentifier(identifier);
        String authenticatedUserId = userResolver.resolveAuthenticatedUserIdSafe(authentication);
        if (!context.profile().getUserId().equals(authenticatedUserId)
                && context.profile().getProfileVisibility() != ProfileVisibility.PUBLIC) {
            throw new ProfileNotFoundException();
        }
        PublicUserProfileResponse response = profileMapper.toPublicResponse(context.profile());
        if (context.user().isCanCreateCourses()) {
            response.setCanCreateCourses(true);
        }
        enrichPublicProfile(response, context.user(), context.profile());
        return response;
    }

    public UserProfileResponse unlockCreator(
            Authentication authentication)
    {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        User user = context.user();
        UserProfile profile = context.profile();

        if (user.isCanCreateCourses() && profile.isCanCreateCourses()) {
            log.debug("User {} has already unlocked creator privileges", user.getId());
            return getMyProfile(authentication);
        }

        user.setCanCreateCourses(true);
        userRepository.save(user);

        profile.setCanCreateCourses(true);
        UserProfile saved = profileRepository.save(profile);
        log.info("User {} successfully unlocked creator privileges", user.getId());

        UserProfileResponse response = profileMapper.toOwnerResponse(user, saved);
        enrichOwnerProfile(response, user, saved);
        return response;
    }

    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            UpdateUserProfileRequest request)
    {
        return updateMyProfile(authentication, request, null, null);
    }

    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            UpdateUserProfileRequest request,
            MultipartFile avatarFile,
            MultipartFile bannerFile)
    {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        User user = context.user();
        UserProfile profile = context.profile();

        profilePatcher.patch(profile, request);
        handleChangePolicy.validateAndApplyHandleChange(profile, request != null ? request.getHandle() : null);
        applyMediaFiles(profile, avatarFile, bannerFile);

        try {
            UserProfile saved = profileRepository.save(profile);
            UserProfileResponse response = profileMapper.toOwnerResponse(user, saved);
            enrichOwnerProfile(response, user, saved);
            return response;
        } catch (DuplicateKeyException exception) {
            throw new HandleConflictException();
        }
    }

    private void applyMediaFiles(
            UserProfile profile,
            MultipartFile avatarFile,
            MultipartFile bannerFile)
    {
        if (mediaStorageService.hasContent(avatarFile)) {
            String oldAvatar = profile.getAvatarUrl();
            String newAvatar = mediaStorageService.uploadImage(avatarFile, "user_avatars");
            profile.setAvatarUrl(newAvatar);
            if (oldAvatar != null && !oldAvatar.equals(newAvatar)) {
                mediaStorageService.deleteImage(oldAvatar);
            }
        }
        if (mediaStorageService.hasContent(bannerFile)) {
            String oldBanner = profile.getBannerUrl();
            String newBanner = mediaStorageService.uploadImage(bannerFile, "user_banners");
            profile.setBannerUrl(newBanner);
            if (oldBanner != null && !oldBanner.equals(newBanner)) {
                mediaStorageService.deleteImage(oldBanner);
            }
        }
    }

    private PublicUserProfileResponse mapAndEnrichPublicProfile(UserProfile profile)
    {
        PublicUserProfileResponse response = profileMapper.toPublicResponse(profile);
        User user = userRepository.findById(profile.getUserId()).orElse(null);
        if (user != null && user.isCanCreateCourses()) {
            response.setCanCreateCourses(true);
        }
        enrichPublicProfile(response, user, profile);
        return response;
    }

    private void enrichOwnerProfile(
            UserProfileResponse response,
            User user,
            UserProfile profile)
    {
        if (response == null || user == null) {
            return;
        }
        long created = courseRepository.countByOwnerId(user.getId());
        long enrolled = courseMembershipRepository.countByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE);
        response.setCoursesCreatedCount(created);
        response.setCoursesEnrolledCount(enrolled);

        List<String> badges = new ArrayList<>();
        if (response.isAdmin()) {
            badges.add("ADMIN");
        }
        if (response.isCanCreateCourses()) {
            badges.add("CREATOR");
        }
        if (created > 0) {
            badges.add("INSTRUCTOR");
        }
        if (enrolled > 0) {
            badges.add("STUDENT");
        }
        response.setBadges(badges);
    }

    private void enrichPublicProfile(
            PublicUserProfileResponse response,
            User user,
            UserProfile profile)
    {
        if (response == null) {
            return;
        }
        String userId = profile != null ? profile.getUserId() : (user != null ? user.getId() : null);
        if (userId == null) {
            return;
        }
        long created = courseRepository.countByOwnerIdAndStatus(userId, CourseStatus.ACTIVE);
        long enrolled = courseMembershipRepository.countByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
        response.setCoursesCreatedCount(created);
        response.setCoursesEnrolledCount(enrolled);

        List<String> badges = new ArrayList<>();
        if ((user != null && user.isAdmin()) || (profile != null && profile.isAdmin())) {
            badges.add("ADMIN");
        }
        if (response.isCanCreateCourses()) {
            badges.add("CREATOR");
        }
        if (created > 0) {
            badges.add("INSTRUCTOR");
        }
        if (enrolled > 0) {
            badges.add("STUDENT");
        }
        response.setBadges(badges);
    }

}
