/**
 * CREATED BY : SUBRATA ROY
 * CONTROLLER : ProfileController
 */
package com.M198.Majorproject.user.profile.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.user.profile.dto.AvatarMediaResponse;
import com.M198.Majorproject.user.profile.dto.BannerMediaResponse;
import com.M198.Majorproject.user.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.user.profile.dto.UpdateCreatorProfileRequest;
import com.M198.Majorproject.user.profile.dto.UpdateUserHandleRequest;
import com.M198.Majorproject.user.profile.dto.UpdateUserProfileRequest;
import com.M198.Majorproject.user.profile.dto.UserProfileResponse;
import com.M198.Majorproject.user.profile.service.ProfileService;

import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/users")
public class ProfileController {

	private final ProfileService profileService;

	// ─────────────────────────────────────────────
	// Public profiles
	// ─────────────────────────────────────────────

	@GetMapping("/by-handle/{handle}")
	public PublicUserProfileResponse getUserByHandle(
			@PathVariable String handle,
			Authentication authentication) {

		return profileService.getUserProfile(handle, authentication);
	}

	@GetMapping("/{userId}")
	public PublicUserProfileResponse getUserById(
			@PathVariable String userId,
			Authentication authentication) {

		return profileService.getUserProfile(userId, authentication);
	}

	// ─────────────────────────────────────────────
	// Current user
	// ─────────────────────────────────────────────

	@GetMapping("/me")
	public UserProfileResponse getMyProfile(
			Authentication authentication) {

		return profileService.getMyProfile(authentication);
	}

	@PatchMapping("/me")
	public UserProfileResponse updateProfile(
			@Valid @RequestBody UpdateUserProfileRequest request,
			Authentication authentication) {

		return profileService.updateMyProfile(
				authentication,
				request);
	}

	@PatchMapping("/me/handle")
	public UserProfileResponse updateHandle(
			@Valid @RequestBody UpdateUserHandleRequest request,
			Authentication authentication) {

		return profileService.updateMyHandle(
				authentication,
				request);
	}

	// ─────────────────────────────────────────────
	// Avatar
	// ─────────────────────────────────────────────

	@PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AvatarMediaResponse uploadAvatar(
			@RequestPart("file") MultipartFile file,
			Authentication authentication) {

		return profileService.uploadAvatar(
				authentication,
				file);
	}

	@DeleteMapping("/me/avatar")
	public AvatarMediaResponse deleteAvatar(
			Authentication authentication) {

		return profileService.deleteAvatar(authentication);
	}

	// ─────────────────────────────────────────────
	// Banner
	// ─────────────────────────────────────────────

	@PutMapping(value = "/me/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public BannerMediaResponse uploadBanner(
			@RequestPart("file") MultipartFile file,
			Authentication authentication) {

		return profileService.uploadBanner(
				authentication,
				file);
	}

	@DeleteMapping("/me/banner")
	public BannerMediaResponse deleteBanner(
			Authentication authentication) {

		return profileService.deleteBanner(authentication);
	}

	// ─────────────────────────────────────────────
	// Creator
	// ─────────────────────────────────────────────

	@PatchMapping("/me/creator-profile")
	public UserProfileResponse updateCreatorProfile(
			@Valid @RequestBody UpdateCreatorProfileRequest request,
			Authentication authentication) {

		return profileService.updateCreatorProfile(
				authentication,
				request);
	}

	@PostMapping({"/me/creator/unlock", "/me/unlock-creator"})
	public UserProfileResponse unlockCreator(
			Authentication authentication) {

		return profileService.unlockCreator(authentication);
	}

	// ─────────────────────────────────────────────
	// Account
	// ─────────────────────────────────────────────

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMyAccount(
			Authentication authentication) {

		profileService.deleteMyAccount(authentication);
	}
}