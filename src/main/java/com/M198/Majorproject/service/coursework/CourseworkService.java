package com.M198.Majorproject.service.coursework;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.dto.CourseworkResponse;
import com.M198.Majorproject.dto.CreateCourseworkRequest;
import com.M198.Majorproject.dto.UpdateCourseworkRequest;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.entity.coursework.Coursework;
import com.M198.Majorproject.entity.coursework.CourseworkStatus;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.course.CourseRepository;
import com.M198.Majorproject.repository.coursework.CourseworkRepository;

@Service
@RequiredArgsConstructor
public class CourseworkService {

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository membershipRepository;
    private final CourseworkRepository courseworkRepository;

    public CourseworkResponse create(
            String courseId, Authentication authentication, CreateCourseworkRequest request) {
        String userId = authenticatedUserId(authentication);
        requireActiveCourse(courseId);
        requireStaff(courseId, userId);
        validateAssignmentFields(request.getType().name(), request.getDueAt(), request.getMaximumPoints());

        Coursework coursework = Coursework.builder()
                .courseId(courseId)
                .creatorId(userId)
                .type(request.getType())
                .title(normalizeRequired(request.getTitle()))
                .description(normalize(request.getDescription()))
                .status(CourseworkStatus.DRAFT)
                .dueAt(request.getDueAt())
                .maximumPoints(request.getMaximumPoints())
                .build();
        return toResponse(courseworkRepository.save(coursework));
    }

    public Page<CourseworkResponse> list(String courseId, Authentication authentication, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        String userId = authenticatedUserId(authentication);
        requireActiveCourse(courseId);
        activeMembership(courseId, userId);
        return courseworkRepository.findAllByCourseIdAndStatusOrderByPublishedAtDesc(
                courseId, CourseworkStatus.PUBLISHED, PageRequest.of(page, size)).map(this::toResponse);
    }

    public CourseworkResponse get(
            String courseId, String courseworkId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        requireActiveCourse(courseId);
        CourseMembership membership = activeMembership(courseId, userId);
        Coursework coursework = courseworkRepository.findByIdAndCourseId(courseworkId, courseId)
                .orElseThrow(CourseworkNotFoundException::new);
        if (!isStaff(membership) && coursework.getStatus() != CourseworkStatus.PUBLISHED) {
            throw new CourseworkNotFoundException();
        }
        return toResponse(coursework);
    }

    public CourseworkResponse update(
            String courseId, String courseworkId, Authentication authentication, UpdateCourseworkRequest request) {
        String userId = authenticatedUserId(authentication);
        requireActiveCourse(courseId);
        requireStaff(courseId, userId);
        Coursework coursework = courseworkRepository.findByIdAndCourseId(courseworkId, courseId)
                .orElseThrow(CourseworkNotFoundException::new);
        if (request.getTitle() != null) {
            coursework.setTitle(normalizeRequired(request.getTitle()));
        }
        if (request.getDescription() != null) {
            coursework.setDescription(normalize(request.getDescription()));
        }
        if (request.getDueAt() != null) {
            coursework.setDueAt(request.getDueAt());
        }
        if (request.getMaximumPoints() != null) {
            if (request.getMaximumPoints() < 0) {
                throw new IllegalArgumentException("Maximum points cannot be negative");
            }
            coursework.setMaximumPoints(request.getMaximumPoints());
        }
        if (request.getStatus() != null) {
            applyStatus(coursework, request.getStatus());
        }
        Coursework saved = courseworkRepository.save(coursework);
        return toResponse(saved);
    }

    public void archive(String courseId, String courseworkId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        requireActiveCourse(courseId);
        requireStaff(courseId, userId);
        Coursework coursework = courseworkRepository.findByIdAndCourseId(courseworkId, courseId)
                .orElseThrow(CourseworkNotFoundException::new);
        coursework.setStatus(CourseworkStatus.ARCHIVED);
        coursework.setArchivedAt(Instant.now());
        courseworkRepository.save(coursework);
    }

    private void applyStatus(Coursework coursework, CourseworkStatus status) {
        if (coursework.getStatus() == CourseworkStatus.ARCHIVED && status != CourseworkStatus.ARCHIVED) {
            throw new CourseworkAccessException("Archived coursework cannot be reopened");
        }
        if (status == CourseworkStatus.PUBLISHED && coursework.getPublishedAt() == null) {
            coursework.setPublishedAt(Instant.now());
        }
        if (status == CourseworkStatus.ARCHIVED && coursework.getArchivedAt() == null) {
            coursework.setArchivedAt(Instant.now());
        }
        if (status == CourseworkStatus.DRAFT) {
            coursework.setPublishedAt(null);
            coursework.setArchivedAt(null);
        }
        if (status == CourseworkStatus.PUBLISHED) {
            coursework.setArchivedAt(null);
        }
        coursework.setStatus(status);
    }

    private void validateAssignmentFields(String type, Instant dueAt, Integer maximumPoints) {
        if (!"ASSIGNMENT".equals(type) && (dueAt != null || maximumPoints != null)) {
            throw new IllegalArgumentException("Due date and maximum points require an assignment");
        }
        if (maximumPoints != null && maximumPoints < 0) {
            throw new IllegalArgumentException("Maximum points cannot be negative");
        }
    }

    private void requireActiveCourse(String courseId) {
        courseRepository.findByIdAndStatus(courseId, CourseStatus.ACTIVE)
                .orElseThrow(CourseworkNotFoundException::new);
    }

    private CourseMembership activeMembership(String courseId, String userId) {
        return membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, MembershipStatus.ACTIVE).orElseThrow(CourseworkNotFoundException::new);
    }

    private void requireStaff(String courseId, String userId) {
        if (!isStaff(activeMembership(courseId, userId))) {
            throw new CourseworkAccessException("Course staff role required");
        }
    }

    private boolean isStaff(CourseMembership membership) {
        return membership.getRole() == MembershipRole.OWNER
                || membership.getRole() == MembershipRole.TEACHER
                || membership.getRole() == MembershipRole.ASSISTANT;
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new CourseworkAccessException("Authentication required");
        }
        return authentication.getName();
    }

    private CourseworkResponse toResponse(Coursework coursework) {
        CourseworkResponse response = new CourseworkResponse();
        response.setId(coursework.getId());
        response.setCourseId(coursework.getCourseId());
        response.setCreatorId(coursework.getCreatorId());
        response.setType(coursework.getType());
        response.setTitle(coursework.getTitle());
        response.setDescription(coursework.getDescription());
        response.setStatus(coursework.getStatus());
        response.setPublishedAt(coursework.getPublishedAt());
        response.setDueAt(coursework.getDueAt());
        response.setMaximumPoints(coursework.getMaximumPoints());
        response.setCreatedAt(coursework.getCreatedAt());
        response.setUpdatedAt(coursework.getUpdatedAt());
        return response;
    }

    private String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Coursework title is required");
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

    public static class CourseworkNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class CourseworkAccessException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CourseworkAccessException(String message) {
            super(message);
        }
    }
}
