package com.M198.Majorproject.profile.security;

import com.M198.Majorproject.identity.entity.User;
import com.M198.Majorproject.identity.entity.UserProfile;

/**
 * Record holding both the active User and their UserProfile.
 */
public record UserContext(User user, UserProfile profile) {
}
