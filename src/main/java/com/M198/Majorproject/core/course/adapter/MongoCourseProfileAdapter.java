package com.M198.Majorproject.core.course.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MongoCourseProfileAdapter implements CourseProfilePort {
    private final UserProfileRepository repository;

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public List<UserProfile> findAllByUserIdIn(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUserIdInAndDeletedAtIsNull(userIds);
    }
}
