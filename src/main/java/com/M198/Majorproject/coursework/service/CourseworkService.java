package com.M198.Majorproject.coursework.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.coursework.dto.CourseworkResponse;
import com.M198.Majorproject.coursework.dto.CreateCourseworkRequest;
import com.M198.Majorproject.coursework.dto.UpdateCourseworkRequest;
import com.M198.Majorproject.course.entity.CourseMembership;
import com.M198.Majorproject.course.entity.CourseStatus;
import com.M198.Majorproject.course.entity.MembershipRole;
import com.M198.Majorproject.course.entity.MembershipStatus;
import com.M198.Majorproject.coursework.entity.Coursework;
import com.M198.Majorproject.coursework.entity.CourseworkStatus;
import com.M198.Majorproject.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.course.repository.CourseRepository;
import com.M198.Majorproject.coursework.repository.CourseworkRepository;

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

        // Honour the caller's requested initial status; guard against ARCHIVED on creation.
        CourseworkStatus initialStatus =
                request.getStatus() == CourseworkStatus.PUBLISHED
                        ? CourseworkStatus.PUBLISHED
                        : CourseworkStatus.DRAFT;
        Instant publishedAt = initialStatus == CourseworkStatus.PUBLISHED ? Instant.now() : null;

        Coursework coursework = Coursework.builder()
                .courseId(courseId)
                .creatorId(userId)
                .type(request.getType())
                .title(normalizeRequired(request.getTitle()))
                .description(normalize(request.getDescription()))
                .status(initialStatus)
                .publishedAt(publishedAt)
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
        CourseMembership membership = activeMembership(courseId, userId);
        PageRequest pageRequest = PageRequest.of(page, size);

        if (isStaff(membership)) {
            // Staff (OWNER, TEACHER, ASSISTANT) see every non-archived item:
            // PUBLISHED items appear first (sorted by publishedAt DESC),
            // DRAFT items (null publishedAt) follow, sorted by createdAt DESC.
            return courseworkRepository
                    .findAllByCourseIdAndStatusNotOrderByPublishedAtDescCreatedAtDesc(
                            courseId, CourseworkStatus.ARCHIVED, pageRequest)
                    .map(this::toResponse);
        }

        // Students see only published items.
        return courseworkRepository
                .findAllByCourseIdAndStatusOrderByPublishedAtDesc(
                        courseId, CourseworkStatus.PUBLISHED, pageRequest)
                .map(this::toResponse);
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

        if (coursework.getDueAt() == null) {
            response.setTemporalStatus("NO_DUE_DATE");
        } else {
            Instant now = Instant.now();
            if (coursework.getDueAt().isBefore(now)) {
                response.setTemporalStatus("PAST");
            } else if (coursework.getDueAt().isBefore(now.plusSeconds(7 * 24 * 3600))) {
                response.setTemporalStatus("THIS_WEEK");
            } else {
                response.setTemporalStatus("UPCOMING");
            }
        }
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
