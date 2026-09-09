/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : CourseService
 * PURPOSE    : Handles course creation, enrollment, roster management, ownership rules,
 *              and lifecycle updates for classroom operations.
 *
 * This service is responsible for teacher/student course interaction.
 * It validates memberships, enforces access control, and keeps course data consistent.
 */
package com.M198.Majorproject.service.course;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.CourseResponse;
import com.M198.Majorproject.dto.CreateCourseRequest;
import com.M198.Majorproject.dto.EnrollCourseRequest;
import com.M198.Majorproject.dto.CourseMemberResponse;
import com.M198.Majorproject.dto.UpdateCourseRequest;
import com.M198.Majorproject.entity.course.Course;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.EnrollmentCode;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;
import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.course.CourseRepository;
import com.M198.Majorproject.repository.course.EnrollmentCodeRepository;
import com.inngest.Inngest;
import com.inngest.InngestEvent;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository membershipRepository;
    private final EnrollmentCodeRepository enrollmentCodeRepository;
    private final Inngest inngest;

    public CourseService(
            CourseRepository courseRepository,
            CourseMembershipRepository membershipRepository,
            EnrollmentCodeRepository enrollmentCodeRepository,
            Inngest inngest) {
        this.courseRepository = courseRepository;
        this.membershipRepository = membershipRepository;
        this.enrollmentCodeRepository = enrollmentCodeRepository;
        this.inngest = inngest;
    }

    public CourseResponse createCourse(Authentication authentication, CreateCourseRequest request) {
        String userId = authenticatedUserId(authentication);
        requireRole(authentication, AccountType.TEACHER);

        Course course = courseRepository.save(Course.builder()
                .ownerId(userId)
                .title(normalizeRequired(request.getTitle()))
                .section(normalize(request.getSection()))
                .subject(normalize(request.getSubject()))
                .description(normalize(request.getDescription()))
                .visibility(request.getVisibility() == null ? CourseVisibility.PRIVATE : request.getVisibility())
                .status(CourseStatus.ACTIVE)
                .build());

        CourseMembership ownerMembership = CourseMembership.builder()
                .courseId(course.getId())
                .userId(userId)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
        try {
            membershipRepository.save(ownerMembership);
            EnrollmentCode enrollmentCode = enrollmentCodeRepository.save(EnrollmentCode.builder()
                    .courseId(course.getId())
                    .code(generateEnrollmentCode())
                    .createdBy(userId)
                    .active(true)
                    .build());
            CourseResponse response = toResponse(course);
            response.setEnrollmentCode(enrollmentCode.getCode());
            emitCourseCreatedEvent(course, enrollmentCode, userId);
            return response;
        } catch (RuntimeException exception) {
            compensateCourseCreation(course, ownerMembership);
            throw exception;
        }
    }

    private void emitCourseCreatedEvent(Course course, EnrollmentCode enrollmentCode, String userId) {
        if (inngest == null) {
            return;
        }

        InngestEvent event = new InngestEvent("course-created", Map.of(
                "courseId", course.getId(),
                "ownerId", userId,
                "title", course.getTitle(),
                "visibility", course.getVisibility(),
                "enrollmentCode", enrollmentCode.getCode(),
                "createdAt", course.getCreatedAt() == null ? Instant.now() : course.getCreatedAt()));
        inngest.send(event);
    }

    private void compensateCourseCreation(Course course, CourseMembership ownerMembership) {
        try {
            membershipRepository.delete(ownerMembership);
        } finally {
            courseRepository.delete(course);
        }
    }

    public List<CourseResponse> listMyCourses(Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        return membershipRepository.findAllByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
                .map(membership -> courseRepository.findByIdAndStatus(membership.getCourseId(), CourseStatus.ACTIVE))
                .flatMap(optionalCourse -> optionalCourse.stream())
                .map(this::toResponse)
                .toList();
    }

    public CourseResponse getCourse(String courseId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        Course course = activeCourse(courseId);
        requireActiveMember(courseId, userId);
        return toResponse(course);
    }

    public CourseResponse enroll(String courseId, Authentication authentication, EnrollCourseRequest request) {
        String userId = authenticatedUserId(authentication);
        requireRole(authentication, AccountType.STUDENT);
        Course course = activeCourse(courseId);
        if (!course.isEnrollmentEnabled()) {
            throw new CourseAccessException("Enrollment is disabled");
        }
        enrollmentCodeRepository.findByCodeAndActiveTrue(request.getCode().trim())
                .filter(value -> value.getCourseId().equals(courseId))
                .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new CourseAccessException("Invalid enrollment code"));
        var existingMembership = membershipRepository.findByCourseIdAndUserId(courseId, userId);
        if (existingMembership.filter(value -> value.getStatus() == MembershipStatus.ACTIVE).isPresent()) {
            throw new CourseConflictException("User already has membership in this course");
        }
        if (existingMembership.isPresent()) {
            CourseMembership membership = existingMembership.get();
            membership.setRole(MembershipRole.STUDENT);
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setJoinedAt(Instant.now());
            membership.setRemovedAt(null);
            membershipRepository.save(membership);
            return toResponse(course);
        }
        try {
            membershipRepository.save(CourseMembership.builder()
                    .courseId(courseId)
                    .userId(userId)
                    .role(MembershipRole.STUDENT)
                    .status(MembershipStatus.ACTIVE)
                    .joinedAt(Instant.now())
                    .build());
        } catch (DuplicateKeyException exception) {
            throw new CourseConflictException("User already has membership in this course");
        }
        return toResponse(course);
    }

    public void leave(String courseId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        activeCourse(courseId);
        CourseMembership membership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new CourseNotFoundException());
        if (membership.getRole() == MembershipRole.OWNER) {
            throw new CourseConflictException("Course owner cannot leave the course");
        }
        membership.setStatus(MembershipStatus.LEFT);
        membership.setRemovedAt(Instant.now());
        membershipRepository.save(membership);
    }

    public CourseResponse update(String courseId, Authentication authentication, UpdateCourseRequest request) {
        String userId = authenticatedUserId(authentication);
        Course course = activeCourse(courseId);
        requireOwnerOrTeacher(courseId, userId);
        if (request.getTitle() != null) course.setTitle(normalizeRequired(request.getTitle()));
        if (request.getSection() != null) course.setSection(normalize(request.getSection()));
        if (request.getSubject() != null) course.setSubject(normalize(request.getSubject()));
        if (request.getDescription() != null) course.setDescription(normalize(request.getDescription()));
        if (request.getVisibility() != null) course.setVisibility(request.getVisibility());
        if (request.getEnrollmentEnabled() != null) course.setEnrollmentEnabled(request.getEnrollmentEnabled());
        return toResponse(courseRepository.save(course));
    }

    public void archive(String courseId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        Course course = activeCourse(courseId);
        requireOwnerOrTeacher(courseId, userId);
        course.setStatus(CourseStatus.ARCHIVED);
        course.setArchivedAt(Instant.now());
        courseRepository.save(course);
    }

    public List<CourseMemberResponse> roster(String courseId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        activeCourse(courseId);
        if (!hasRole(authentication, AccountType.ADMIN)) {
            requireActiveMember(courseId, userId);
        }
        return membershipRepository.findAllByCourseIdAndStatus(courseId, MembershipStatus.ACTIVE).stream()
                .map(membership -> {
                    CourseMemberResponse response = new CourseMemberResponse();
                    response.setUserId(membership.getUserId());
                    response.setRole(membership.getRole());
                    response.setJoinedAt(membership.getJoinedAt());
                    return response;
                }).toList();
    }

    private Course activeCourse(String courseId) {
        return courseRepository.findByIdAndStatus(courseId, CourseStatus.ACTIVE)
                .orElseThrow(CourseNotFoundException::new);
    }

    private void requireActiveMember(String courseId, String userId) {
        membershipRepository.findByCourseIdAndUserIdAndStatus(courseId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(CourseNotFoundException::new);
    }

    private void requireOwnerOrTeacher(String courseId, String userId) {
        CourseMembership membership = membershipRepository.findByCourseIdAndUserIdAndStatus(
            courseId, userId, MembershipStatus.ACTIVE)
            .orElseThrow(() -> new CourseAccessException("Course membership required"));
        if (membership.getRole() != MembershipRole.OWNER && membership.getRole() != MembershipRole.TEACHER) {
            throw new CourseAccessException("Course owner or teacher role required");
        }
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new CourseAccessException("Authentication required");
        }
        return authentication.getName();
    }

    private void requireRole(Authentication authentication, AccountType requiredRole) {
        boolean permitted = hasRole(authentication, requiredRole);
        if (!permitted) {
            throw new CourseAccessException("Teacher role required");
        }
    }

    private boolean hasRole(Authentication authentication, AccountType role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    private CourseResponse toResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setOwnerId(course.getOwnerId());
        response.setTitle(course.getTitle());
        response.setSection(course.getSection());
        response.setSubject(course.getSubject());
        response.setDescription(course.getDescription());
        response.setVisibility(course.getVisibility());
        response.setStatus(course.getStatus());
        response.setEnrollmentEnabled(course.isEnrollmentEnabled());
        response.setCreatedAt(course.getCreatedAt());
        response.setUpdatedAt(course.getUpdatedAt());
        return response;
    }

    private String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Course title is required");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateEnrollmentCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public static class CourseNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static class CourseAccessException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CourseAccessException(String message) {
            super(message);
        }
    }

    public static class CourseConflictException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CourseConflictException(String message) {
            super(message);
        }
    }
}
