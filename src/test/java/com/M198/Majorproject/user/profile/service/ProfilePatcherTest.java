package com.M198.Majorproject.user.profile.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.M198.Majorproject.user.profile.entity.ProfileLink;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.dto.UpdateUserProfileRequest;

class ProfilePatcherTest {

    private final MediaStorageService mediaStorageService = mock(MediaStorageService.class);
    private ProfilePatcher patcher;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        patcher = new ProfilePatcher(mediaStorageService);
        profile = UserProfile.builder()
                .userId("u-1")
                .firstName("John")
                .lastName("Doe")
                .displayName("Original")
                .headline("Developer")
                .about("Original About")
                .city("Kolkata")
                .country("India")
                .phone("111")
                .gender("Male")
                .dateOfBirth("1990-01-01")
                .address("Old St")
                .profileVisibility(ProfileVisibility.PRIVATE)
                .avatarUrl("https://existing.com/avatar.png")
                .bannerUrl("https://existing.com/banner.png")
                .build();
    }

    @Test
    void patch_UpdatesProfileVisibility() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setProfileVisibility(ProfileVisibility.PUBLIC);

        patcher.patch(profile, request);
        assertEquals(ProfileVisibility.PUBLIC, profile.getProfileVisibility());
    }

    @Test
    void patch_UpdatesAndTrimsScalarFieldsWhilePreservingOmittedFields() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFirstName("  Jane  ");
        request.setLastName("  Smith  ");
        request.setDisplayName("  Jane Smith  ");
        request.setHeadline("  Lead Engineer  ");
        request.setAbout("  Updated Bio  ");
        request.setCity("  Bengaluru  ");
        request.setCountry("  India  ");
        request.setPhone("  999  ");
        request.setGender("  Female  ");
        request.setDateOfBirth("  1992-02-02  ");
        request.setAddress("  MG Road  ");
        request.setProfileVisibility(ProfileVisibility.PUBLIC);

        patcher.patch(profile, request);

        assertEquals("Jane", profile.getFirstName());
        assertEquals("Smith", profile.getLastName());
        assertEquals("Jane Smith", profile.getDisplayName());
        assertEquals("Lead Engineer", profile.getHeadline());
        assertEquals("Updated Bio", profile.getAbout());
        assertEquals("Bengaluru", profile.getCity());
        assertEquals("India", profile.getCountry());
        assertEquals("999", profile.getPhone());
        assertEquals("Female", profile.getGender());
        assertEquals("1992-02-02", profile.getDateOfBirth());
        assertEquals("MG Road", profile.getAddress());
        assertEquals(ProfileVisibility.PUBLIC, profile.getProfileVisibility());
    }

    @Test
    void patch_EmptyAssetUrlClearsToNull() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("   ");
        request.setBannerUrl("");

        patcher.patch(profile, request);

        assertNull(profile.getAvatarUrl());
        assertNull(profile.getBannerUrl());
    }

    @Test
    void patch_UploadsMediaWhenAssetUrlProvided() {
        when(mediaStorageService.uploadImage(eq("data:image/png;base64,abc"), eq("user_avatars")))
                .thenReturn("https://cloudinary.com/new_avatar.png");
        when(mediaStorageService.uploadImage(eq("https://cdn.com/new_banner.png"), eq("user_banners")))
                .thenReturn("https://cdn.com/new_banner.png");

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("data:image/png;base64,abc");
        request.setBannerUrl("https://cdn.com/new_banner.png");

        patcher.patch(profile, request);

        assertEquals("https://cloudinary.com/new_avatar.png", profile.getAvatarUrl());
        assertEquals("https://cdn.com/new_banner.png", profile.getBannerUrl());
    }

    @Test
    void patch_MapsLinksFilteringInvalidOnes() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setLinks(List.of(
                ProfileLink.builder().name(" Portfolio ").url(" https://me.dev ").build(),
                ProfileLink.builder().name("").url(" https://empty-name.dev ").build(),
                ProfileLink.builder().name("Bad").url("   ").build() // should be filtered out
        ));

        patcher.patch(profile, request);

        assertEquals(2, profile.getLinks().size());
        assertEquals("Portfolio", profile.getLinks().get(0).getName());
        assertEquals("https://me.dev", profile.getLinks().get(0).getUrl());
        assertNull(profile.getLinks().get(1).getName());
        assertEquals("https://empty-name.dev", profile.getLinks().get(1).getUrl());
    }
}
