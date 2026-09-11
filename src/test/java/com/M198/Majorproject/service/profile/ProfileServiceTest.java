package com.M198.Majorproject.service.profile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.entity.identity.ProfileLink;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.M198.Majorproject.repository.identity.UserRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

class ProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository profileRepository = mock(UserProfileRepository.class);
    private final Cloudinary cloudinary = mock(Cloudinary.class);
    private final Uploader uploader = mock(Uploader.class);
        private final ProfileService profileService = new ProfileService(userRepository, profileRepository, cloudinary);
    private final Authentication authentication = mock(Authentication.class);
    private User user;
    private UserProfile profile;

    @BeforeEach
    @SuppressWarnings("unused")
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
        when(cloudinary.uploader()).thenReturn(uploader);
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

    @Test
    void emptyAssetUrlClearsAssetAndNullAssetUrlPreservesExistingAsset() {
        profile.setAvatarUrl("https://existing.example/avatar.jpg");
        profile.setBannerUrl("https://existing.example/banner.jpg");
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("");
        request.setBannerUrl(null);

        profileService.updateMyProfile(authentication, request);

        assertEquals(null, profile.getAvatarUrl());
        assertEquals("https://existing.example/banner.jpg", profile.getBannerUrl());
    }

    @Test
    void emptyUploadedFileUsesTheAssetUrlClearRule() {
        profile.setAvatarUrl("https://existing.example/avatar.jpg");
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("");

        profileService.updateMyProfile(
                authentication,
                request,
                new MockMultipartFile("avatarFile", new byte[0]),
                null);

        assertEquals(null, profile.getAvatarUrl());
        verifyNoInteractions(uploader);
    }

    @Test
    void nonEmptyAssetFilesUploadToTheirDedicatedFoldersAndOverrideUrlFields() throws Exception {
        when(uploader.upload(any(byte[].class), argThat(options -> "user_avatars".equals(options.get("folder")))))
                .thenReturn(java.util.Map.of("secure_url", "https://cdn.example/avatar.jpg"));
        when(uploader.upload(any(byte[].class), argThat(options -> "user_banners".equals(options.get("folder")))))
                .thenReturn(java.util.Map.of("secure_url", "https://cdn.example/banner.jpg"));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("");
        request.setBannerUrl("");

        var response = profileService.updateMyProfile(
                authentication,
                request,
                new MockMultipartFile("avatarFile", "avatar.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("bannerFile", "banner.jpg", "image/jpeg", new byte[]{2}));

        assertEquals("https://cdn.example/avatar.jpg", response.getAvatarUrl());
        assertEquals("https://cdn.example/banner.jpg", response.getBannerUrl());
        verify(uploader).upload(any(byte[].class), argThat(options -> "user_avatars".equals(options.get("folder"))));
        verify(uploader).upload(any(byte[].class), argThat(options -> "user_banners".equals(options.get("folder"))));
    }

    @Test
    void handleUpdateEnforcesFourteenDayRateLimit() {
        UpdateUserProfileRequest request1 = new UpdateUserProfileRequest();
        request1.setHandle("handle_one");
        profileService.updateMyProfile(authentication, request1);
        assertEquals("handle_one", profile.getHandle());

        UpdateUserProfileRequest request2 = new UpdateUserProfileRequest();
        request2.setHandle("handle_two");
        profileService.updateMyProfile(authentication, request2);
        assertEquals("handle_two", profile.getHandle());

        UpdateUserProfileRequest request3 = new UpdateUserProfileRequest();
        request3.setHandle("handle_three");
        var ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateMyProfile(authentication, request3));
        assertEquals("You can only change your handle twice within a 14-day period.", ex.getMessage());
    }

    @Test
    void updatingProfileSavesCustomLinks() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setLinks(java.util.List.of(
                ProfileLink.builder().name("GitHub").url("https://github.com/test").build(),
                ProfileLink.builder().name("LinkedIn").url("https://linkedin.com/in/test").build()));

        var response = profileService.updateMyProfile(authentication, request);

        assertEquals(2, response.getLinks().size());
        assertEquals("GitHub", response.getLinks().get(0).getName());
        assertEquals("https://github.com/test", response.getLinks().get(0).getUrl());
    }

    @Test
    void updatingProfileSavesPersonalInformation() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setPhone("+1234567890");
        request.setGender("Female");
        request.setDateOfBirth("2002-05-15");
        request.setAddress("123 Main Street");
        request.setCity("Siliguri");
        request.setCountry("India");

        var response = profileService.updateMyProfile(authentication, request);

        assertEquals("+1234567890", response.getPhone());
        assertEquals("Female", response.getGender());
        assertEquals("2002-05-15", response.getDateOfBirth());
        assertEquals("123 Main Street", response.getAddress());
        assertEquals("Siliguri", response.getCity());
        assertEquals("India", response.getCountry());
    }

    @Test
    void getPublicProfilesReturnsOnlyPublicProfiles() {
        UserProfile publicProfile = UserProfile.builder()
                .userId("user-2")
                .displayName("Public User")
                .profileVisibility(ProfileVisibility.PUBLIC)
                .build();
        when(profileRepository.findAllByProfileVisibility(ProfileVisibility.PUBLIC))
                .thenReturn(java.util.List.of(publicProfile));

        var publicProfiles = profileService.getPublicProfiles();

        assertEquals(1, publicProfiles.size());
        assertEquals("Public User", publicProfiles.get(0).getName());
    }

    @Test
    void unlockCreatorSetsCanCreateCoursesOnUserAndProfile() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = profileService.unlockCreator(authentication);

        org.junit.jupiter.api.Assertions.assertTrue(response.isCanCreateCourses());
        assertEquals(AccountType.STUDENT, response.getAccountType());
        verify(userRepository).save(argThat(User::isCanCreateCourses));
        verify(profileRepository).save(argThat(UserProfile::isCanCreateCourses));
    }
}
