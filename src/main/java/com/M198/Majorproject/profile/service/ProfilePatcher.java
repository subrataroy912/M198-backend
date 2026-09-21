package com.M198.Majorproject.profile.service;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.M198.Majorproject.identity.entity.ProfileLink;
import com.M198.Majorproject.identity.entity.ProfileVisibility;
import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.profile.dto.UpdateUserProfileRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfilePatcher {

    private final MediaStorageService mediaStorageService;

    public void patch(UserProfile profile, UpdateUserProfileRequest request) {
        if (request == null) {
            return;
        }

        if (request.getProfileVisibility() == ProfileVisibility.COURSE_MEMBERS) {
            throw new IllegalArgumentException("COURSE_MEMBERS visibility is not available yet");
        }

        patchScalar(request.getFirstName(), profile::setFirstName);
        patchScalar(request.getLastName(), profile::setLastName);
        patchScalar(request.getDisplayName(), profile::setDisplayName);
        patchScalar(request.getHeadline(), profile::setHeadline);
        patchScalar(request.getAbout(), profile::setAbout);
        patchScalar(request.getCity(), profile::setCity);
        patchScalar(request.getCountry(), profile::setCountry);
        patchScalar(request.getPhone(), profile::setPhone);
        patchScalar(request.getGender(), profile::setGender);
        patchScalar(request.getDateOfBirth(), profile::setDateOfBirth);
        patchScalar(request.getAddress(), profile::setAddress);

        if (request.getProfileVisibility() != null) {
            profile.setProfileVisibility(request.getProfileVisibility());
        }

        if (request.getAvatarUrl() != null) {
            String avatar = request.getAvatarUrl().trim();
            profile.setAvatarUrl(avatar.isEmpty() ? null : mediaStorageService.uploadImage(avatar, "user_avatars"));
        }

        if (request.getBannerUrl() != null) {
            String banner = request.getBannerUrl().trim();
            profile.setBannerUrl(banner.isEmpty() ? null : mediaStorageService.uploadImage(banner, "user_banners"));
        }

        if (request.getLinks() != null) {
            profile.setLinks(mapLinks(request.getLinks()));
        }
    }

    private void patchScalar(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value.trim());
        }
    }

    private List<ProfileLink> mapLinks(List<ProfileLink> links) {
        return links.stream()
                .filter(link -> link != null && link.getUrl() != null && !link.getUrl().isBlank())
                .map(link -> ProfileLink.builder()
                        .name(link.getName() != null && !link.getName().isBlank() ? link.getName().trim() : null)
                        .url(link.getUrl().trim())
                        .build())
                .toList();
    }
}
