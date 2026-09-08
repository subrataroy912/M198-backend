package com.M198.Majorproject.service.profile;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import com.M198.Majorproject.dto.PublicUserProfileResponse;
import com.M198.Majorproject.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.dto.UserProfileResponse;
import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.M198.Majorproject.repository.identity.UserRepository;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public ProfileService(UserRepository userRepository, UserProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    public UserProfileResponse getMyProfile(Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        User user = activeUser(userId);
        UserProfile profile = profile(userId);
        return toOwnerResponse(user, profile);
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
        String userId = authenticatedUserId(authentication);
        User user = activeUser(userId);
        UserProfile profile = profile(userId);
        applyUpdate(profile, request);
        try {
            return toOwnerResponse(user, profileRepository.save(profile));
        } catch (DuplicateKeyException exception) {
            throw new HandleConflictException();
        }
    }

    private void applyUpdate(UserProfile profile, UpdateUserProfileRequest request) {
        if (request.getProfileVisibility() == ProfileVisibility.COURSE_MEMBERS) {
            throw new IllegalArgumentException("COURSE_MEMBERS visibility is not available yet");
        }
        if (request.getHandle() != null) {
            String handle = request.getHandle().trim();
            if (!handle.isEmpty() && profileRepository.findByHandle(handle)
                    .filter(existing -> !existing.getUserId().equals(profile.getUserId()))
                    .isPresent()) {
                throw new IllegalArgumentException("Handle is already taken");
            }
            profile.setHandle(handle.isEmpty() ? null : handle);
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
            profile.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getBannerUrl() != null) {
            profile.setBannerUrl(request.getBannerUrl().trim());
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
    }

    public static class ProfileNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class HandleConflictException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
