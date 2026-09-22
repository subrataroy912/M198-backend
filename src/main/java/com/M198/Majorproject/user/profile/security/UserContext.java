package com.M198.Majorproject.user.profile.security;

import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.profile.entity.UserProfile;

/**
 * Record holding both the active User and their UserProfile.
 */
public record UserContext(User user, UserProfile profile) {
}
