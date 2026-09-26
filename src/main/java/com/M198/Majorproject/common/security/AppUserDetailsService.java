/**
 * CREATED BY : SUBRATA ROY
 * SECURITY   : AppUserDetailsService
 * PURPOSE    : Loads authenticated user details into Spring Security for JWT-based access control.
 *
 * This service translates the application user model into Spring Security's UserDetails,
 * so protected endpoints can validate the authenticated caller correctly.
 */
package com.M198.Majorproject.common.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import com.M198.Majorproject.user.auth.entity.PendingRegistration;
import com.M198.Majorproject.user.auth.repository.PendingRegistrationRepository;
import com.M198.Majorproject.user.identity.entity.AccountStatus;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return toUserDetails(userRepository.findByEmailAndActiveTrueAndStatus(email, AccountStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found")));
    }

    public UserDetails loadById(String userId) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByIdAndActiveTrueAndStatus(userId, AccountStatus.ACTIVE);
        if (userOpt.isPresent()) {
            return toUserDetails(userOpt.get(), userId);
        }

        Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findById(userId);
        if (pendingOpt.isPresent()) {
            return toPendingUserDetails(pendingOpt.get());
        }

        throw new UsernameNotFoundException("User not found");
    }

    private UserDetails toPendingUserDetails(PendingRegistration pending) {
        return org.springframework.security.core.userdetails.User
                .withUsername(pending.getId())
                .password(pending.getPasswordHash() != null ? pending.getPasswordHash() : "{noop}__PENDING__")
                .roles("ONBOARDING", "USER")
                .build();
    }

    private UserDetails toUserDetails(User user) {
        return toUserDetails(user, user.getEmail());
    }

    private UserDetails toUserDetails(User user, String username) {
        java.util.List<String> roles = new java.util.ArrayList<>();
        roles.add("USER");
        if (user.isAdmin()) {
            roles.add("ADMIN");
        }
        if (user.isCanCreateCourses()) {
            roles.add("CREATOR");
        }

        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        var builder = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(hasPassword ? user.getPasswordHash() : "{noop}__NO_PASSWORD_SET_USE_SOCIAL_LOGIN__")
                .roles(roles.toArray(new String[0]));

        if (!hasPassword) {
            builder.credentialsExpired(true);
            builder.accountLocked(true);
        }

        return builder.build();
    }
}
