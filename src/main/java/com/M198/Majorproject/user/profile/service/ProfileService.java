/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : ProfileService
 * PURPOSE    : Orchestrates user profile operations including lookup, visibility rules,
 *              updates, creator unlock, and public discovery.
 */
package com.M198.Majorproject.user.profile.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.user.profile.port.ProfileCoursePort;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.identity.entity.AccountStatus;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;
import com.M198.Majorproject.user.identity.repository.UserRepository;
import com.M198.Majorproject.user.auth.repository.RefreshTokenRepository;
import com.M198.Majorproject.user.profile.dto.AvatarMediaResponse;
import com.M198.Majorproject.user.profile.dto.BannerMediaResponse;
import com.M198.Majorproject.user.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.user.profile.dto.UpdateCreatorProfileRequest;
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

    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024; // 2MB
    private static final long MAX_BANNER_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ProfileCoursePort profileCoursePort;
    private final ProfileMapper profileMapper;
    private final AuthenticatedUserResolver userResolver;
    private final ProfilePatcher profilePatcher;
    private final HandleChangePolicy handleChangePolicy;
    private final MediaStorageService mediaStorageService;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserProfileResponse getMyProfile(Authentication authentication) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        UserProfileResponse response = profileMapper.toOwnerResponse(context.user(), context.profile());
        enrichOwnerProfile(response, context.user(), context.profile());
        return response;
    }

    public PublicUserProfileResponse getUserProfile(
            String identifier,
            Authentication authentication) {
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
            Authentication authentication) {
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
            UpdateUserProfileRequest request) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        User user = context.user();
        UserProfile profile = context.profile();

        profilePatcher.patch(profile, request);
        handleChangePolicy.validateAndApplyHandleChange(profile, request != null ? request.getHandle() : null);
        profile.setProfileCompleted(true);

        try {
            UserProfile saved = profileRepository.save(profile);
            UserProfileResponse response = profileMapper.toOwnerResponse(user, saved);
            enrichOwnerProfile(response, user, saved);
            return response;
        } catch (DuplicateKeyException exception) {
            throw new HandleConflictException();
        }
    }

    public AvatarMediaResponse uploadAvatar(Authentication authentication, MultipartFile file) {
        validateImage(file, MAX_AVATAR_SIZE, "Avatar");
        UserContext context = userResolver.resolveCurrentUser(authentication);
        UserProfile profile = context.profile();

        String oldAvatar = profile.getAvatarUrl();
        String newAvatar = mediaStorageService.uploadImage(file, "user_avatars");
        profile.setAvatarUrl(newAvatar);

        if (oldAvatar != null && !oldAvatar.equals(newAvatar)) {
            mediaStorageService.deleteImage(oldAvatar);
        }

        UserProfile saved = profileRepository.save(profile);
        return AvatarMediaResponse.builder()
                .avatarUrl(saved.getAvatarUrl())
                .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt() : Instant.now())
                .build();
    }

    public AvatarMediaResponse deleteAvatar(Authentication authentication) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        UserProfile profile = context.profile();

        String oldAvatar = profile.getAvatarUrl();
        if (oldAvatar != null && !oldAvatar.isBlank()) {
            mediaStorageService.deleteImage(oldAvatar);
            profile.setAvatarUrl(null);
            UserProfile saved = profileRepository.save(profile);
            return AvatarMediaResponse.builder()
                    .avatarUrl(null)
                    .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt() : Instant.now())
                    .build();
        }

        return AvatarMediaResponse.builder()
                .avatarUrl(null)
                .updatedAt(profile.getUpdatedAt() != null ? profile.getUpdatedAt() : Instant.now())
                .build();
    }

    public BannerMediaResponse uploadBanner(Authentication authentication, MultipartFile file) {
        validateImage(file, MAX_BANNER_SIZE, "Banner");
        UserContext context = userResolver.resolveCurrentUser(authentication);
        UserProfile profile = context.profile();

        String oldBanner = profile.getBannerUrl();
        String newBanner = mediaStorageService.uploadImage(file, "user_banners");
        profile.setBannerUrl(newBanner);

        if (oldBanner != null && !oldBanner.equals(newBanner)) {
            mediaStorageService.deleteImage(oldBanner);
        }

        UserProfile saved = profileRepository.save(profile);
        return BannerMediaResponse.builder()
                .bannerUrl(saved.getBannerUrl())
                .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt() : Instant.now())
                .build();
    }

    public BannerMediaResponse deleteBanner(Authentication authentication) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        UserProfile profile = context.profile();

        String oldBanner = profile.getBannerUrl();
        if (oldBanner != null && !oldBanner.isBlank()) {
            mediaStorageService.deleteImage(oldBanner);
            profile.setBannerUrl(null);
            UserProfile saved = profileRepository.save(profile);
            return BannerMediaResponse.builder()
                    .bannerUrl(null)
                    .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt() : Instant.now())
                    .build();
        }

        return BannerMediaResponse.builder()
                .bannerUrl(null)
                .updatedAt(profile.getUpdatedAt() != null ? profile.getUpdatedAt() : Instant.now())
                .build();
    }

    public UserProfileResponse updateCreatorProfile(Authentication authentication, UpdateCreatorProfileRequest request) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        User user = context.user();
        UserProfile profile = context.profile();

        if (!user.isCanCreateCourses() && !profile.isCanCreateCourses()) {
            throw new AccessDeniedException("Creator privileges required to update creator profile");
        }

        if (request != null && request.getTags() != null) {
            profile.setTags(new ArrayList<>(request.getTags()));
        }

        UserProfile saved = profileRepository.save(profile);
        UserProfileResponse response = profileMapper.toOwnerResponse(user, saved);
        enrichOwnerProfile(response, user, saved);
        return response;
    }

    public void deleteMyAccount(Authentication authentication) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        User user = context.user();
        UserProfile profile = context.profile();

        Instant now = Instant.now();
        user.setActive(false);
        user.setStatus(AccountStatus.DELETED);
        user.setDeletedAt(now);
        userRepository.save(user);

        profile.setDeletedAt(now);
        profileRepository.save(profile);

        refreshTokenRepository.deleteAllByUserId(user.getId());
        log.info("User {} account has been deleted and sessions revoked", user.getId());
    }

    private void validateImage(MultipartFile file, long maxBytes, String assetName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(assetName + " file cannot be empty");
        }
        if (file.getSize() > maxBytes) {
            long maxMb = maxBytes / (1024 * 1024);
            throw new IllegalArgumentException(assetName + " file size exceeds maximum limit of " + maxMb + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file format for " + assetName + ". Only JPEG, PNG, and WebP are allowed.");
        }
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.M198.Majorproject.common.presence.UserPresenceService userPresenceService;

    private void enrichOwnerProfile(
            UserProfileResponse response,
            User user,
            UserProfile profile) {
        if (response == null || user == null) {
            return;
        }
        long created = profileCoursePort.countCreatedCourses(user.getId());
        long enrolled = profileCoursePort.countActiveEnrolledCourses(user.getId());
        response.setCoursesCreatedCount(created);
        response.setCoursesEnrolledCount(enrolled);

        boolean isOnline = userPresenceService != null ? userPresenceService.isOnline(user.getId()) : true;
        java.time.Instant fallbackLastActive = profile != null && profile.getLastActiveAt() != null
                ? profile.getLastActiveAt()
                : (profile != null ? profile.getUpdatedAt() : user.getUpdatedAt());
        response.setOnline(isOnline);
        response.setLastActiveAt(userPresenceService != null
                ? userPresenceService.getLastActiveAt(user.getId(), fallbackLastActive)
                : fallbackLastActive);

        List<String> badges = new ArrayList<>();
        if (response.isAdmin()) {
            badges.add("ADMIN");
        }
        if (response.isCanCreateCourses()) {
            badges.add("CREATOR");
        }
        response.setBadges(badges);
    }

    private void enrichPublicProfile(
            PublicUserProfileResponse response,
            User user,
            UserProfile profile) {
        if (response == null) {
            return;
        }
        String userId = profile != null ? profile.getUserId() : (user != null ? user.getId() : null);
        if (userId == null) {
            return;
        }
        long created = profileCoursePort.countActiveCreatedCourses(userId);
        long enrolled = profileCoursePort.countActiveEnrolledCourses(userId);
        response.setCoursesCreatedCount(created);
        response.setCoursesEnrolledCount(enrolled);

        boolean isOnline = userPresenceService != null && userPresenceService.isOnline(userId);
        java.time.Instant fallbackLastActive = profile != null && profile.getLastActiveAt() != null
                ? profile.getLastActiveAt()
                : (profile != null ? profile.getUpdatedAt() : (user != null ? user.getUpdatedAt() : null));
        response.setOnline(isOnline);
        response.setLastActiveAt(userPresenceService != null
                ? userPresenceService.getLastActiveAt(userId, fallbackLastActive)
                : fallbackLastActive);

        List<String> badges = new ArrayList<>();
        if ((user != null && user.isAdmin()) || (profile != null && profile.isAdmin())) {
            badges.add("ADMIN");
        }
        if (response.isCanCreateCourses()) {
            badges.add("CREATOR");
        }
        response.setBadges(badges);
    }

}
