package com.M198.Majorproject.core.course.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.M198.Majorproject.user.profile.entity.UserProfile;

/** Read-only profile boundary used when rendering course owners and rosters. */
public interface CourseProfilePort {
    Optional<UserProfile> findByUserId(String userId);
    List<UserProfile> findAllByUserIdIn(Collection<String> userIds);
}
