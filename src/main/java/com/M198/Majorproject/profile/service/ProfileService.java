/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : ProfileService
 * PURPOSE    : Orchestrates user profile operations including lookup, visibility rules,
 *              updates, creator unlock, and public discovery.
 */
package com.M198.Majorproject.profile.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.identity.entity.ProfileVisibility;
import com.M198.Majorproject.identity.entity.User;
import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.identity.repository.UserProfileRepository;
import com.M198.Majorproject.identity.repository.UserRepository;
import com.M198.Majorproject.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.profile.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.profile.dto.UserProfileResponse;
import com.M198.Majorproject.profile.mapper.ProfileMapper;
import com.M198.Majorproject.profile.security.AuthenticatedUserResolver;
import com.M198.Majorproject.profile.security.UserContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final AuthenticatedUserResolver userResolver;
    private final ProfilePatcher profilePatcher;
    private final HandleChangePolicy handleChangePolicy;
    private final MediaStorageService mediaStorageService;

    public UserProfileResponse getMyProfile(Authentication authentication) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        return profileMapper.toOwnerResponse(context.user(), context.profile());
    }

    public List<PublicUserProfileResponse> getPublicProfiles() {
        List<UserProfile> profiles = profileRepository.findAllByProfileVisibility(ProfileVisibility.PUBLIC).stream()
                .filter(profile -> profile.getDeletedAt() == null)
                .toList();
        return profileMapper.toPublicResponseList(profiles);
    }

    public PublicUserProfileResponse getUserProfile(String userId, Authentication authentication) {
        UserContext context = userResolver.resolveUser(userId);
        String authenticatedUserId = userResolver.authenticatedUserId(authentication);
        if (!userId.equals(authenticatedUserId)
                && context.profile().getProfileVisibility() != ProfileVisibility.PUBLIC) {
            throw new ProfileNotFoundException();
        }
        PublicUserProfileResponse response = profileMapper.toPublicResponse(context.profile());
        if (context.user().isCanCreateCourses()) {
            response.setCanCreateCourses(true);
        }
        return response;
    }

    public UserProfileResponse unlockCreator(Authentication authentication) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        User user = context.user();
        UserProfile profile = context.profile();

        user.setCanCreateCourses(true);
        userRepository.save(user);

        profile.setCanCreateCourses(true);
        UserProfile saved = profileRepository.save(profile);
        return profileMapper.toOwnerResponse(user, saved);
    }

    public UserProfileResponse updateMyProfile(Authentication authentication, UpdateUserProfileRequest request) {
        return updateMyProfile(authentication, request, null, null);
    }

    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            UpdateUserProfileRequest request,
            MultipartFile avatarFile,
            MultipartFile bannerFile) {
        UserContext context = userResolver.resolveCurrentUser(authentication);
        User user = context.user();
        UserProfile profile = context.profile();

        profilePatcher.patch(profile, request);
        handleChangePolicy.validateAndApplyHandleChange(profile, request != null ? request.getHandle() : null);
        applyMediaFiles(profile, avatarFile, bannerFile);

        try {
            UserProfile saved = profileRepository.save(profile);
            return profileMapper.toOwnerResponse(user, saved);
        } catch (DuplicateKeyException exception) {
            throw new HandleConflictException();
        }
    }

    private void applyMediaFiles(UserProfile profile, MultipartFile avatarFile, MultipartFile bannerFile) {
        if (mediaStorageService.hasContent(avatarFile)) {
            profile.setAvatarUrl(mediaStorageService.uploadImage(avatarFile, "user_avatars"));
        }
        if (mediaStorageService.hasContent(bannerFile)) {
            profile.setBannerUrl(mediaStorageService.uploadImage(bannerFile, "user_banners"));
        }
    }

    /**
     * @deprecated Use
     *             {@link com.M198.Majorproject.profile.exception.ProfileNotFoundException}
     *             directly.
     */
    @Deprecated
    public static class ProfileNotFoundException
            extends com.M198.Majorproject.profile.exception.ProfileNotFoundException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * @deprecated Use
     *             {@link com.M198.Majorproject.profile.exception.HandleConflictException}
     *             directly.
     */
    @Deprecated
    public static class HandleConflictException
            extends com.M198.Majorproject.profile.exception.HandleConflictException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * @deprecated Use
     *             {@link com.M198.Majorproject.profile.exception.ProfileStorageException}
     *             directly.
     */
    @Deprecated
    public static class ProfileStorageException
            extends com.M198.Majorproject.profile.exception.ProfileStorageException {
        private static final long serialVersionUID = 1L;

        public ProfileStorageException(String message) {
            super(message);
        }

        public ProfileStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
