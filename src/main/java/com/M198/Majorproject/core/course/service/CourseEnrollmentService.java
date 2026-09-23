package com.M198.Majorproject.core.course.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.M198.Majorproject.core.course.dto.CourseResponse;
import com.M198.Majorproject.core.course.dto.EnrollCourseRequest;

import lombok.RequiredArgsConstructor;

/**
 * Enrollment-specific entry point; authorization and membership transitions
 * remain transactional in lifecycle.
 */
@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {
    private final CourseLifecycleService lifecycle;

    public CourseResponse enrollByCode(Authentication authentication, String code) {
        return lifecycle.enrollByCode(authentication, code);
    }

    public CourseResponse enroll(String courseId, Authentication authentication, EnrollCourseRequest request) {
        return lifecycle.enroll(courseId, authentication, request);
    }

    public void leave(String courseId, Authentication authentication) {
        lifecycle.leave(courseId, authentication);
    }

    public void removeMember(String courseId, String userId, Authentication authentication) {
        lifecycle.removeMember(courseId, userId, authentication);
    }
}
