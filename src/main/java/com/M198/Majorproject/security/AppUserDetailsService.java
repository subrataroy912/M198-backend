package com.M198.Majorproject.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.User;
import com.M198.Majorproject.repository.identity.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
        var builder = org.springframework.security.core.userdetails.User
            .withUsername(username)
            .password(user.getPasswordHash() == null ? "" : user.getPasswordHash())
            .roles(user.getAccountType().name());
        if (user.getPasswordHash() == null) {
            builder.credentialsExpired(true);
        }
        return builder.build();
    }
}