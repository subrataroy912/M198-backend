package com.M198.Majorproject.user.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.user.profile.dto.UpdateUserHandleRequest;
import com.M198.Majorproject.user.profile.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.user.profile.dto.UserProfileResponse;
import com.M198.Majorproject.user.profile.service.ProfileService;

class ProfileControllerTest {

    private ProfileService profileService;
    private ProfileController controller;
    private Authentication authentication;
    private UpdateUserHandleRequest lastHandleRequest;
    private UpdateUserProfileRequest lastProfileRequest;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(null, null, null, null, null, null, null, null, null, null) {
            @Override
            public UserProfileResponse updateMyHandle(Authentication auth, UpdateUserHandleRequest request) {
                lastHandleRequest = request;
                UserProfileResponse response = new UserProfileResponse();
                response.setHandle(request.getHandle());
                response.setHandleChangesRemaining(2);
                return response;
            }

            @Override
            public UserProfileResponse updateMyProfile(Authentication auth, UpdateUserProfileRequest request) {
                lastProfileRequest = request;
                UserProfileResponse response = new UserProfileResponse();
                response.setFirstName(request.getFirstName());
                return response;
            }
        };
        controller = new ProfileController(profileService);
        authentication = mock(Authentication.class);
    }

    @Test
    void updateHandleDelegatesToProfileService() {
        UpdateUserHandleRequest request = new UpdateUserHandleRequest("brand_new_handle");

        UserProfileResponse response = controller.updateHandle(request, authentication);

        assertEquals("brand_new_handle", response.getHandle());
        assertEquals(2, response.getHandleChangesRemaining());
        assertEquals("brand_new_handle", lastHandleRequest.getHandle());
    }

    @Test
    void updateProfileDelegatesToProfileService() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFirstName("Jane");

        UserProfileResponse response = controller.updateProfile(request, authentication);

        assertEquals("Jane", response.getFirstName());
        assertEquals("Jane", lastProfileRequest.getFirstName());
    }
}
