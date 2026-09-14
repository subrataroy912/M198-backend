/**
 * CREATED BY : SUBRATA ROY
 * CONTROLLER : ProfileController
 * PURPOSE    : Exposes endpoints for viewing and updating the current user's profile and public profile data.
 *
 * This controller sits in front of the profile service and ensures account owners can access
 * their own information, while visibility rules still protect private data from unrelated users.
 */
package com.M198.Majorproject.profile.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.profile.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.profile.dto.UserProfileResponse;
import com.M198.Majorproject.profile.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/users")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping
	public java.util.List<PublicUserProfileResponse> getPublicProfiles() {
		return profileService.getPublicProfiles();
	}

	@GetMapping("/me")
	public UserProfileResponse getMyProfile(Authentication authentication) {
		return profileService.getMyProfile(authentication);
	}

	@GetMapping("/{userId}")
	public PublicUserProfileResponse getUserProfile(
			@PathVariable String userId,
			Authentication authentication) {
		return profileService.getUserProfile(userId, authentication);
	}

	@PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
	public UserProfileResponse updateProfile(
			@Valid @RequestBody UpdateUserProfileRequest request,
			Authentication authentication) {
		return profileService.updateMyProfile(authentication, request);
	}

	@PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserProfileResponse updateProfileWithAssets(
			@Valid @RequestPart(value = "profile", required = false) UpdateUserProfileRequest request,
			@RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
			@RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
			Authentication authentication) {
		UpdateUserProfileRequest effectiveRequest = request != null ? request : new UpdateUserProfileRequest();
		return profileService.updateMyProfile(authentication, effectiveRequest, avatarFile, bannerFile);
	}

	@PostMapping("/me/unlock-creator")
	public UserProfileResponse unlockCreator(Authentication authentication) {
		return profileService.unlockCreator(authentication);
	}

}
