package com.M198.Majorproject.profile.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.M198.Majorproject.identity.entity.AccountStatus;
import com.M198.Majorproject.identity.entity.User;
import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.identity.repository.UserProfileRepository;
import com.M198.Majorproject.identity.repository.UserRepository;
import com.M198.Majorproject.profile.exception.ProfileNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public String authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new ProfileNotFoundException();
        }
        return authentication.getName();
    }

    public User activeUser(String userId) {
        return userRepository.findByIdAndActiveTrueAndStatus(userId, AccountStatus.ACTIVE)
                .orElseThrow(ProfileNotFoundException::new);
    }

    public UserProfile activeProfile(String userId) {
        return profileRepository.findByUserId(userId)
                .filter(profile -> profile.getDeletedAt() == null)
                .orElseThrow(ProfileNotFoundException::new);
    }

    public UserContext resolveCurrentUser(Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        User user = activeUser(userId);
        UserProfile profile = activeProfile(userId);
        return new UserContext(user, profile);
    }

    public UserContext resolveUser(String userId) {
        User user = activeUser(userId);
        UserProfile profile = activeProfile(userId);
        return new UserContext(user, profile);
    }
}
