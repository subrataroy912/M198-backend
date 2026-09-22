package com.M198.Majorproject.user.profile.adapter;

import org.springframework.stereotype.Component;

import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.user.profile.port.ProfileCoursePort;

@Component
class MongoProfileCourseAdapter implements ProfileCoursePort {

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository courseMembershipRepository;

    MongoProfileCourseAdapter(CourseRepository courseRepository, CourseMembershipRepository courseMembershipRepository) {
        this.courseRepository = courseRepository;
        this.courseMembershipRepository = courseMembershipRepository;
    }

    @Override
    public long countCreatedCourses(String userId) {
        return courseRepository.countByOwnerId(userId);
    }

    @Override
    public long countActiveCreatedCourses(String userId) {
        return courseRepository.countByOwnerIdAndStatus(userId, CourseStatus.ACTIVE);
    }

    @Override
    public long countActiveEnrolledCourses(String userId) {
        return courseMembershipRepository.countByUserIdAndStatus(userId, MembershipStatus.ACTIVE);
    }
}
