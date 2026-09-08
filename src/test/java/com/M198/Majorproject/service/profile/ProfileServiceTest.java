package com.M198.Majorproject.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.M198.Majorproject.repository.identity.UserRepository;

class ProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private final ProfileService profileService = new ProfileService(userRepository, profileRepository);
    private final Authentication authentication = mock(Authentication.class);
    private User user;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .accountType(AccountType.STUDENT)
                .status(AccountStatus.ACTIVE)
                .active(true)
                .build();
        profile = UserProfile.builder()
                .userId("user-1")
                .displayName("Original Name")
                .headline("Original headline")
                .profileVisibility(ProfileVisibility.PRIVATE)
                .build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user-1");
        when(userRepository.findByIdAndActiveTrueAndStatus("user-1", AccountStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(profileRepository.findByUserId("user-1")).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
    }

    @Test
    void ownerCanReadPrivateProfile() {
        var response = profileService.getMyProfile(authentication);

        assertEquals("user@example.com", response.getEmail());
        assertEquals("Original Name", response.getDisplayName());
    }

    @Test
    void nonOwnerCannotReadPrivateProfile() {
        Authentication otherAuthentication = mock(Authentication.class);
        when(otherAuthentication.isAuthenticated()).thenReturn(true);
        when(otherAuthentication.getName()).thenReturn("user-2");

        assertThrows(ProfileService.ProfileNotFoundException.class,
                () -> profileService.getUserProfile("user-1", otherAuthentication));
    }

    @Test
    void inactiveAccountCannotExposePublicProfile() {
        UserProfile publicProfile = UserProfile.builder()
                .userId("user-2")
                .profileVisibility(ProfileVisibility.PUBLIC)
                .build();
        when(profileRepository.findByUserId("user-2")).thenReturn(Optional.of(publicProfile));
        when(userRepository.findByIdAndActiveTrueAndStatus("user-2", AccountStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ProfileService.ProfileNotFoundException.class,
                () -> profileService.getUserProfile("user-2", authentication));
    }

    @Test
    void patchPreservesOmittedFieldsAndRejectsCourseMemberVisibility() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setDisplayName("Updated Name");

        var response = profileService.updateMyProfile(authentication, request);

        assertEquals("Updated Name", profile.getDisplayName());
        assertEquals("Original headline", profile.getHeadline());
        assertEquals(ProfileVisibility.PRIVATE, response.getProfileVisibility());

        request.setProfileVisibility(ProfileVisibility.COURSE_MEMBERS);
        assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile(authentication, request));
    }

    @Test
    void softDeletedProfileIsNotReadable() {
        profile.setDeletedAt(java.time.Instant.now());

        assertThrows(ProfileService.ProfileNotFoundException.class,
                () -> profileService.getMyProfile(authentication));
    }

    @Test
    void duplicateHandleIsRejected() {
        UserProfile existingProfile = UserProfile.builder()
                .userId("user-2")
                .handle("ada_lovelace")
                .build();
        when(profileRepository.findByHandle("ada_lovelace")).thenReturn(Optional.of(existingProfile));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setHandle("ada_lovelace");

        assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile(authentication, request));
    }
}
