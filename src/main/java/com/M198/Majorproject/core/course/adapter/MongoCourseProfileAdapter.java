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

    public Optional<UserProfile> findByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public List<UserProfile> findAllByUserIdIn(Collection<String> userIds) {
        return repository.findAllByUserIdIn(userIds);
    }
}
