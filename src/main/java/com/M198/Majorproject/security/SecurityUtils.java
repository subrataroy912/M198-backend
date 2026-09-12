package com.M198.Majorproject.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

/**
 * Utility class providing common security operations such as extracting the
 * authenticated user ID and checking granted authorities.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent instantiation of utility class
    }

    /**
     * Extracts the authenticated user's ID (principal name) from the provided Authentication object.
     *
     * @param authentication the current Spring Security authentication
     * @return the authenticated user's ID
     * @throws AuthenticationCredentialsNotFoundException if authentication is missing or unauthenticated
     */
    public static String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AuthenticationCredentialsNotFoundException("User is not authenticated");
        }
        return authentication.getName();
    }

    /**
     * Checks if the given authentication contains the specified authority or role.
     *
     * @param authentication the current Spring Security authentication
     * @param authority the authority to check for (e.g., ROLE_ADMIN)
     * @return true if the authority is present, false otherwise
     */
    public static boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
