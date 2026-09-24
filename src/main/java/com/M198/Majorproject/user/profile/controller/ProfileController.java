/**
 * CREATED BY : SUBRATA ROY
 * CONTROLLER : ProfileController
 */
package com.M198.Majorproject.user.profile.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.user.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.user.profile.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.user.profile.dto.UserProfileResponse;
import com.M198.Majorproject.user.profile.service.ProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/users")
public class ProfileController {

	private final ProfileService profileService;

	@GetMapping
	public Page<PublicUserProfileResponse> getPublicProfiles(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size) {

		int safeSize = Math.min(Math.max(1, size), 100);

		Sort defaultSort = Sort.by(Sort.Direction.DESC, "createdAt");

		return profileService.getPublicProfiles(query, PageRequest.of(page, safeSize, defaultSort));
	}

	@GetMapping("/me")
	public UserProfileResponse getMyProfile(Authentication authentication) {
		return profileService.getMyProfile(authentication);
	}

	@GetMapping("/by-handle/{handle}")
	public PublicUserProfileResponse getUserByHandle(
			@PathVariable String handle,
			Authentication authentication) {
		return profileService.getUserProfile(handle, authentication);
	}

	@GetMapping("/{identifier}")
	public PublicUserProfileResponse getUserProfile(
			@PathVariable String identifier,
			Authentication authentication) {
		return profileService.getUserProfile(identifier, authentication);
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
