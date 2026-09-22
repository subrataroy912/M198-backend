package com.M198.Majorproject.core.course.security;

import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipRole;

/** Centralizes authentication and role-based course membership authorization. */
@Component
public class CourseAccessPolicy {

    private final CourseMembershipResolver membershipResolver;

    public CourseAccessPolicy(CourseMembershipResolver membershipResolver) {
        this.membershipResolver = membershipResolver;
    }

    public String authenticatedUserId(
            Authentication authentication, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw exceptionSupplier.get();
        }
        return authentication.getName();
    }

    public CourseMembership requireActiveMember(
            String courseId, String userId, Supplier<? extends RuntimeException> exceptionSupplier) {
        return membershipResolver.requireActiveMember(courseId, userId, exceptionSupplier);
    }

    public CourseMembership requireTeacherOrOwner(
            String courseId, String userId, Supplier<? extends RuntimeException> membershipException,
            Supplier<? extends RuntimeException> roleException) {
        CourseMembership membership = requireActiveMember(courseId, userId, membershipException);
        if (membership.getRole() != MembershipRole.OWNER && membership.getRole() != MembershipRole.TEACHER) {
            throw roleException.get();
        }
        return membership;
    }

    public CourseMembership requireStudent(
            String courseId, String userId, Supplier<? extends RuntimeException> membershipException,
            Supplier<? extends RuntimeException> roleException) {
        CourseMembership membership = requireActiveMember(courseId, userId, membershipException);
        if (membership.getRole() != MembershipRole.STUDENT) {
            throw roleException.get();
        }
        return membership;
    }

    public CourseMembership requireStaff(
            String courseId, String userId, Supplier<? extends RuntimeException> membershipException,
            Supplier<? extends RuntimeException> roleException) {
        CourseMembership membership = requireActiveMember(courseId, userId, membershipException);
        if (!isStaff(membership)) {
            throw roleException.get();
        }
        return membership;
    }

    public boolean isStaff(CourseMembership membership) {
        return membership.getRole() == MembershipRole.OWNER
                || membership.getRole() == MembershipRole.TEACHER
                || membership.getRole() == MembershipRole.ASSISTANT;
    }
}
