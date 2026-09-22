package com.M198.Majorproject.user.profile.security;

import java.util.Optional;

import com.M198.Majorproject.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.M198.Majorproject.user.identity.entity.AccountStatus;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;
import com.M198.Majorproject.user.identity.repository.UserRepository;
import com.M198.Majorproject.user.profile.exception.ProfileNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public String authenticatedUserId(Authentication authentication)
    {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank())
        {
            throw new UnauthorizedException("User is not authenticated");
        }
        return authentication.getName();
    }

    public User activeUser(String userId) {
        return userRepository
                .findByIdAndActiveTrueAndStatus(userId, AccountStatus.ACTIVE)
                .orElseThrow(ProfileNotFoundException::new);
    }

    public UserProfile activeProfile(String userId)
    {
        return profileRepository.findByUserId(userId)
                .filter(profile -> profile.getDeletedAt() == null)
                .orElseThrow(ProfileNotFoundException::new);
    }

    public String resolveAuthenticatedUserIdSafe(Authentication authentication)
    {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }

    public UserContext resolveCurrentUser(Authentication authentication)
    {
        String userId = authenticatedUserId(authentication);
        User user = activeUser(userId);
        UserProfile profile = activeProfile(userId);
        return new UserContext(user, profile);
    }

    public UserContext resolveUser(String userId)
    {
        User user = activeUser(userId);
        UserProfile profile = activeProfile(userId);
        return new UserContext(user, profile);
    }

    public UserContext resolveUserByIdentifier(String identifier)
    {
        if (identifier == null || identifier.isBlank()) {
            throw new ProfileNotFoundException();
        }
        String clean = identifier.trim();
        if (clean.startsWith("@")) {
            return resolveByHandle(clean.substring(1));
        }

        // Try as userId first
        Optional<UserProfile> byUserId = profileRepository.findByUserId(clean)
                .filter(p -> p.getDeletedAt() == null);
        if (byUserId.isPresent()) {
            User user = activeUser(byUserId.get().getUserId());
            return new UserContext(user, byUserId.get());
        }

        // Try as handle
        return resolveByHandle(clean);
    }

    public UserContext resolveByHandle(String handle)
    {
        if (handle == null || handle.isBlank()) {
            throw new ProfileNotFoundException();
        }
        String cleanHandle = handle.trim().toLowerCase();
        UserProfile profile = profileRepository.findByHandleIgnoreCase(cleanHandle)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(ProfileNotFoundException::new);
        User user = activeUser(profile.getUserId());
        return new UserContext(user, profile);
    }
}
