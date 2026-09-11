/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : ProfileService
 * PURPOSE    : Manages user profile data, visibility rules, media uploads,
 *              and profile updates for the current authenticated user.
 *
 * This service protects private information while allowing public user discovery.
 * It also uploads avatar/banner assets to Cloudinary.
 */
package com.M198.Majorproject.service.profile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.dto.PublicUserProfileResponse;
import com.M198.Majorproject.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.dto.UserProfileResponse;
import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.M198.Majorproject.repository.identity.UserRepository;
import com.cloudinary.Cloudinary;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final Cloudinary cloudinary;

    public UserProfileResponse getMyProfile(Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        User user = activeUser(userId);
        UserProfile profile = profile(userId);
        return toOwnerResponse(user, profile);
    }

    public List<PublicUserProfileResponse> getPublicProfiles() {
        return profileRepository.findAllByProfileVisibility(ProfileVisibility.PUBLIC).stream()
                .filter(profile -> profile.getDeletedAt() == null)
                .map(this::toPublicResponse)
                .toList();
    }

    public PublicUserProfileResponse getUserProfile(String userId, Authentication authentication) {
        activeUser(userId);
        UserProfile profile = profile(userId);
        String authenticatedUserId = authenticatedUserId(authentication);
        if (!userId.equals(authenticatedUserId) && profile.getProfileVisibility() != ProfileVisibility.PUBLIC) {
            throw new ProfileNotFoundException();
        }
        return toPublicResponse(profile);
    }

    public UserProfileResponse updateMyProfile(Authentication authentication, UpdateUserProfileRequest request) {
        return updateMyProfile(authentication, request, null, null);
    }

    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            UpdateUserProfileRequest request,
            MultipartFile avatarFile,
            MultipartFile bannerFile) {
        String userId = authenticatedUserId(authentication);
        User user = activeUser(userId);
        UserProfile profile = profile(userId);
        applyUpdate(profile, request);
        applyMediaUpdate(profile, avatarFile, bannerFile);
        try {
            UserProfile saved = profileRepository.save(profile);
            return toOwnerResponse(user, saved);
        } catch (DuplicateKeyException exception) {
            throw new HandleConflictException();
        }
    }

    private void applyMediaUpdate(UserProfile profile, MultipartFile avatarFile, MultipartFile bannerFile) {
        if (hasContent(avatarFile)) {
            profile.setAvatarUrl(upload(avatarFile, "user_avatars"));
        }
        if (hasContent(bannerFile)) {
            profile.setBannerUrl(upload(bannerFile, "user_banners"));
        }
    }

    private boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private String upload(MultipartFile file, String folder) {
        try {
            return upload(file.getBytes(), file.getContentType(), folder);
        } catch (ProfileStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ProfileStorageException("Could not read uploaded profile asset", exception);
        }
    }

    private String mediaValue(String value, String folder) {
        if (!value.startsWith("data:")) {
            return value;
        }
        int separator = value.indexOf(',');
        if (separator < 0 || !value.substring(0, separator).contains(";base64")) {
            throw new ProfileStorageException("Invalid profile image data");
        }
        String metadata = value.substring(5, separator);
        String contentType = metadata.substring(0, metadata.indexOf(';'));
        if (!contentType.startsWith("image/")) {
            throw new ProfileStorageException("Profile image must be an image file");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(separator + 1));
            return upload(bytes, contentType, folder);
        } catch (IllegalArgumentException exception) {
            throw new ProfileStorageException("Invalid profile image data", exception);
        }
    }

    private String upload(byte[] bytes, String contentType, String folder) {
        try {
            Map<String, Object> options = new java.util.HashMap<>();
            options.put("folder", folder);
            options.put("resource_type", "image");
            if (contentType != null && !contentType.isBlank()) {
                options.put("context", "content_type=" + contentType);
            }
            Map<?, ?> result = cloudinary.uploader().upload(bytes, options);
            Object secureUrl = result.get("secure_url");
            if (!(secureUrl instanceof String url) || url.isBlank()) {
                throw new ProfileStorageException("Cloudinary did not return a secure asset URL");
            }
            return url;
        } catch (ProfileStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ProfileStorageException("Could not upload profile asset", exception);
        }
    }

    private void applyUpdate(UserProfile profile, UpdateUserProfileRequest request) {
        if (request.getProfileVisibility() == ProfileVisibility.COURSE_MEMBERS) {
            throw new IllegalArgumentException("COURSE_MEMBERS visibility is not available yet");
        }
        if (request.getHandle() != null) {
            String newHandle = request.getHandle().trim();
            String currentHandle = profile.getHandle() != null ? profile.getHandle().trim() : "";
            if (!newHandle.equalsIgnoreCase(currentHandle)) {
                if (!newHandle.isEmpty() && profileRepository.findByHandle(newHandle)
                        .filter(existing -> !existing.getUserId().equals(profile.getUserId()))
                        .isPresent()) {
                    throw new IllegalArgumentException("Handle is already taken");
                }
                Instant fourteenDaysAgo = Instant.now().minus(Duration.ofDays(14));
                List<Instant> timestamps = profile.getHandleUpdatedTimestamps();
                if (timestamps == null) {
                    timestamps = new java.util.ArrayList<>();
                    profile.setHandleUpdatedTimestamps(timestamps);
                }
                List<Instant> recentUpdates = timestamps.stream()
                        .filter(ts -> ts != null && ts.isAfter(fourteenDaysAgo))
                        .toList();
                if (recentUpdates.size() >= 2) {
                    throw new IllegalArgumentException("You can only change your handle twice within a 14-day period.");
                }
                timestamps.add(Instant.now());
                profile.setHandle(newHandle.isEmpty() ? null : newHandle);
            }
        }
        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getHeadline() != null) {
            profile.setHeadline(request.getHeadline().trim());
        }
        if (request.getAbout() != null) {
            profile.setAbout(request.getAbout().trim());
        }
        if (request.getAvatarUrl() != null) {
            String avatar = request.getAvatarUrl().trim();
            profile.setAvatarUrl(avatar.isEmpty() ? null : mediaValue(avatar, "user_avatars"));
        }
        if (request.getBannerUrl() != null) {
            String banner = request.getBannerUrl().trim();
            profile.setBannerUrl(banner.isEmpty() ? null : mediaValue(banner, "user_banners"));
        }
        if (request.getCity() != null) {
            profile.setCity(request.getCity().trim());
        }
        if (request.getCountry() != null) {
            profile.setCountry(request.getCountry().trim());
        }
        if (request.getGradeLevel() != null) {
            profile.setGradeLevel(request.getGradeLevel().trim());
        }
        if (request.getProfileVisibility() != null) {
            profile.setProfileVisibility(request.getProfileVisibility());
        }
        if (request.getLinks() != null) {
            profile.setLinks(request.getLinks().stream()
                    .filter(link -> link != null && !link.isBlank())
                    .map(String::trim)
                    .toList());
        }
    }

    private User activeUser(String userId) {
        return userRepository.findByIdAndActiveTrueAndStatus(userId, AccountStatus.ACTIVE)
                .orElseThrow(ProfileNotFoundException::new);
    }

    private UserProfile profile(String userId) {
        return profileRepository.findByUserId(userId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(ProfileNotFoundException::new);
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new ProfileNotFoundException();
        }
        return authentication.getName();
    }

    private UserProfileResponse toOwnerResponse(User user, UserProfile profile) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(profile.getUserId());
        response.setEmail(user.getEmail());
        response.setAccountType(user.getAccountType());
        copyProfileFields(profile, response);
        return response;
    }

    private PublicUserProfileResponse toPublicResponse(UserProfile profile) {
        PublicUserProfileResponse response = new PublicUserProfileResponse();
        response.setId(profile.getUserId());
        copyProfileFields(profile, response);
        return response;
    }

    private void copyProfileFields(UserProfile profile, UserProfileResponse response) {
        response.setHandle(profile.getHandle());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setDisplayName(profile.getDisplayName());
        response.setAvatarUrl(profile.getAvatarUrl());
        response.setBannerUrl(profile.getBannerUrl());
        response.setHeadline(profile.getHeadline());
        response.setAbout(profile.getAbout());
        response.setCity(profile.getCity());
        response.setCountry(profile.getCountry());
        response.setProfileVisibility(profile.getProfileVisibility());
        response.setGradeLevel(profile.getGradeLevel());
        response.setLinks(profile.getLinks() != null ? new java.util.ArrayList<>(profile.getLinks()) : java.util.Collections.emptyList());
    }

    private void copyProfileFields(UserProfile profile, PublicUserProfileResponse response) {
        response.setHandle(profile.getHandle());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setDisplayName(profile.getDisplayName());
        response.setAvatarUrl(profile.getAvatarUrl());
        response.setBannerUrl(profile.getBannerUrl());
        response.setHeadline(profile.getHeadline());
        response.setAbout(profile.getAbout());
        response.setCity(profile.getCity());
        response.setCountry(profile.getCountry());
        response.setProfileVisibility(profile.getProfileVisibility());
        response.setGradeLevel(profile.getGradeLevel());
        response.setLinks(profile.getLinks() != null ? new java.util.ArrayList<>(profile.getLinks()) : java.util.Collections.emptyList());
    }

    public static class ProfileNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class HandleConflictException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class ProfileStorageException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ProfileStorageException(String message) {
            super(message);
        }

        public ProfileStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
