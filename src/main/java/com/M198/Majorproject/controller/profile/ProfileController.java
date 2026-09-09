/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
	 * ==========================================
	 * API Functions inside the Profile Controller:
	 * ==========================================
	 * 1. getMyProfile      - Retrieves the logged-in user's profile and settings
	 * 2. getUserProfileById - Retrieves another user's public profile
	 * 3. updateProfileData - Updates the logged-in user's profile data
 */

/*
 * =================================================================================
 * PROFILE CONTROLLER ENDPOINT DOCUMENTATION
 * =================================================================================
 *
 * 1. getMyProfile
 *    - Route: GET /v1/users/me
 *    - Role Allowed: Authenticated users.
 *    - Request: The user ID is taken from the authenticated security context.
 *    - How it works: Loads the caller's profile and private account settings without
 *      accepting a user ID supplied by the client.
 *    - Response: Returns private profile data needed by the account owner, excluding
 *      password hashes and other security-sensitive fields.
 *    - Why it's used: Populates the current user's profile and account screens.
 *
 * 2. getUserProfileById
 *    - Route: GET /v1/users/{userId}
 *    - Role Allowed: Authenticated users, subject to profile visibility rules.
 *    - Request: Target user ID path parameter.
 *    - How it works: Fetches only public fields such as display name, avatar, and
 *      permitted contact information.
 *    - Response: Returns a public user profile.
 *    - Why it's used: Displays the identity of classmates, teachers, and authors.
 *
 * 3. updateProfileData
 *    - Route: PATCH /v1/users/me
 *    - Role Allowed: Authenticated users updating their own profile.
 *    - Request Body: Editable fields such as display name, avatar reference, and
 *      notification preferences. Email and role require separate verification rules.
 *    - How it works: Validates the changed fields, updates only the authenticated
 *      user's record, and prevents changes to protected account properties.
 *    - Response: Returns the updated safe profile representation.
 *    - Why it's used: Allows users to maintain personal details and preferences.
 */
/**
 * CREATED BY : SUBRATA ROY
 * CONTROLLER : ProfileController
 * PURPOSE    : Exposes endpoints for viewing and updating the current user's profile and public profile data.
 *
 * This controller sits in front of the profile service and ensures account owners can access
 * their own information, while visibility rules still protect private data from unrelated users.
 */
package com.M198.Majorproject.controller.profile;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.dto.PublicUserProfileResponse;
import com.M198.Majorproject.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.dto.UserProfileResponse;
import com.M198.Majorproject.service.profile.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/users")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
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
			@Valid @RequestPart("profile") UpdateUserProfileRequest request,
			@RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
			@RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile,
			Authentication authentication) {
		return profileService.updateMyProfile(authentication, request, avatarFile, bannerFile);
	}

}
