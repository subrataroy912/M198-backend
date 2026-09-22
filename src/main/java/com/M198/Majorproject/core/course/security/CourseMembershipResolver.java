package com.M198.Majorproject.core.course.security;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;

/** Resolves memberships that currently grant access to a course. */
@Component
public class CourseMembershipResolver {

    private final CourseMembershipRepository membershipRepository;

    public CourseMembershipResolver(CourseMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public CourseMembership requireActiveMember(
            String courseId, String userId, Supplier<? extends RuntimeException> exceptionSupplier) {
        return membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, MembershipStatus.ACTIVE).orElseThrow(exceptionSupplier);
    }
}
