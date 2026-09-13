/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : CourseMembershipRepository
 * PURPOSE    : Manages membership records linking users to courses and tracking their roles.
 *
 * Membership records are the gatekeepers for access control in classrooms,
 * allowing teachers, owners, students, and active/inactive states to be evaluated reliably.
 */
package com.M198.Majorproject.repository.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.MembershipStatus;

public interface CourseMembershipRepository extends MongoRepository<CourseMembership, String> {

    Optional<CourseMembership> findByCourseIdAndUserId(String courseId, String userId);

    Optional<CourseMembership> findByCourseIdAndUserIdAndStatus(
            String courseId, String userId, MembershipStatus status);

    List<CourseMembership> findAllByCourseIdAndStatus(String courseId, MembershipStatus status);

    List<CourseMembership> findAllByUserIdAndStatus(String userId, MembershipStatus status);

    boolean existsByCourseIdAndUserIdAndStatus(String courseId, String userId, MembershipStatus status);

    long countByCourseIdAndStatus(String courseId, MembershipStatus status);

    List<CourseMembership> findAllByCourseIdInAndStatus(java.util.Collection<String> courseIds, MembershipStatus status);
}
