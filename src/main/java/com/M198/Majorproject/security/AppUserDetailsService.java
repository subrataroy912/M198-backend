/**
 * CREATED BY : SUBRATA ROY
 * SECURITY   : AppUserDetailsService
 * PURPOSE    : Loads authenticated user details into Spring Security for JWT-based access control.
 *
 * This service translates the application user model into Spring Security's UserDetails,
 * so protected endpoints can validate the authenticated caller correctly.
 */
package com.M198.Majorproject.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.repository.identity.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return toUserDetails(userRepository.findByEmailAndActiveTrueAndStatus(email, AccountStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found")));
    }

    public UserDetails loadById(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByIdAndActiveTrueAndStatus(userId, AccountStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDetails(user, userId);
    }

    private UserDetails toUserDetails(User user) {
        return toUserDetails(user, user.getEmail());
    }

    private UserDetails toUserDetails(User user, String username) {
        java.util.List<String> roles = new java.util.ArrayList<>();
        roles.add(user.getAccountType().name());
        if (user.isCanCreateCourses()) {
            roles.add("CREATOR");
        }

        var builder = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(user.getPasswordHash() == null ? "" : user.getPasswordHash())
                .roles(roles.toArray(new String[0]));

        if (user.getPasswordHash() == null) {
            builder.credentialsExpired(true);
        }

        return builder.build();
    }
}
