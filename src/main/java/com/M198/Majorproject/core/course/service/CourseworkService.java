package com.M198.Majorproject.core.course.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.core.course.dto.CourseworkResponse;
import com.M198.Majorproject.core.course.dto.CreateCourseworkRequest;
import com.M198.Majorproject.core.course.dto.UpdateCourseworkRequest;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.Coursework;
import com.M198.Majorproject.core.course.entity.CourseworkAttachment;
import com.M198.Majorproject.core.course.entity.CourseworkStatus;
import com.M198.Majorproject.core.course.entity.CourseworkType;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.core.course.repository.CourseworkRepository;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import com.M198.Majorproject.user.profile.entity.UserProfile;

@Service
@RequiredArgsConstructor
public class CourseworkService {

    private final CourseRepository courseRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final CourseworkRepository courseworkRepository;
    private final CourseProfilePort profilePort;

    public CourseworkResponse create(
            String courseId, Authentication authentication, CreateCourseworkRequest request) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication,
                () -> new CourseworkAccessException("Authentication required"));
        requireActiveCourse(courseId);
        CourseMembership membership;
        if (request.getType() == CourseworkType.ANNOUNCEMENT) {
            membership = courseAccessPolicy.requireActiveMember(courseId, userId, CourseworkNotFoundException::new);
        } else {
            membership = courseAccessPolicy.requireStaff(courseId, userId, CourseworkNotFoundException::new,
                    () -> new CourseworkAccessException("Course staff role required"));
        }
        validateAssignmentFields(request.getType().name(), request.getDueAt(), request.getMaximumPoints());

        // Default to PUBLISHED so newly created announcements and coursework are
        // immediately visible to all members unless explicitly marked as DRAFT.
        CourseworkStatus initialStatus = request.getStatus() == CourseworkStatus.DRAFT
                ? CourseworkStatus.DRAFT
                : CourseworkStatus.PUBLISHED;
        Instant publishedAt = initialStatus == CourseworkStatus.PUBLISHED ? Instant.now() : null;

        boolean isStaff = courseAccessPolicy.isStaff(membership);
        boolean pinned = Boolean.TRUE.equals(request.getPinned()) && isStaff;
        List<CourseworkAttachment> attachments = request.getAttachments() != null
                ? request.getAttachments()
                : Collections.emptyList();

        Coursework coursework = Coursework.builder()
                .courseId(courseId)
                .creatorId(userId)
                .type(request.getType())
                .title(normalizeRequired(request.getTitle()))
                .description(normalize(request.getDescription()))
                .status(initialStatus)
                .pinned(pinned)
                .attachments(attachments)
                .publishedAt(publishedAt)
                .dueAt(request.getDueAt())
                .maximumPoints(request.getMaximumPoints())
                .build();
        Coursework saved = courseworkRepository.save(coursework);
        UserProfile profile = profilePort != null ? profilePort.findByUserId(userId).orElse(null) : null;
        return toResponse(saved, profile);
    }

    public Page<CourseworkResponse> list(String courseId, Authentication authentication, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        String userId = courseAccessPolicy.authenticatedUserId(authentication,
                () -> new CourseworkAccessException("Authentication required"));
        requireActiveCourse(courseId);
        CourseMembership membership = courseAccessPolicy.requireActiveMember(courseId, userId,
                CourseworkNotFoundException::new);
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<Coursework> pageOfCoursework = courseAccessPolicy.isStaff(membership)
                ? courseworkRepository.findAllByCourseIdAndStatusNotOrderByPinnedDescPublishedAtDescCreatedAtDesc(
                        courseId, CourseworkStatus.ARCHIVED, pageRequest)
                : courseworkRepository.findAllByCourseIdAndStatusOrderByPinnedDescPublishedAtDesc(
                        courseId, CourseworkStatus.PUBLISHED, pageRequest);

        Set<String> creatorIds = pageOfCoursework.getContent().stream()
                .map(Coursework::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, UserProfile> profileMap = creatorIds.isEmpty() || profilePort == null
                ? Collections.emptyMap()
                : profilePort.findAllByUserIdIn(creatorIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));

        return pageOfCoursework.map(cw -> toResponse(cw, profileMap.get(cw.getCreatorId())));
    }

    public CourseworkResponse get(
            String courseId, String courseworkId, Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication,
                () -> new CourseworkAccessException("Authentication required"));
        requireActiveCourse(courseId);
        CourseMembership membership = courseAccessPolicy.requireActiveMember(courseId, userId,
                CourseworkNotFoundException::new);
        Coursework coursework = courseworkRepository.findByIdAndCourseId(courseworkId, courseId)
                .orElseThrow(CourseworkNotFoundException::new);
        if (!courseAccessPolicy.isStaff(membership) && coursework.getStatus() != CourseworkStatus.PUBLISHED) {
            throw new CourseworkNotFoundException();
        }
        UserProfile profile = coursework.getCreatorId() != null && profilePort != null
                ? profilePort.findByUserId(coursework.getCreatorId()).orElse(null)
                : null;
        return toResponse(coursework, profile);
    }

    public CourseworkResponse update(
            String courseId, String courseworkId, Authentication authentication, UpdateCourseworkRequest request) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication,
                () -> new CourseworkAccessException("Authentication required"));
        requireActiveCourse(courseId);
        CourseMembership membership = courseAccessPolicy.requireActiveMember(courseId, userId,
                CourseworkNotFoundException::new);
        Coursework coursework = courseworkRepository.findByIdAndCourseId(courseworkId, courseId)
                .orElseThrow(CourseworkNotFoundException::new);

        boolean isCreator = userId.equals(coursework.getCreatorId());
        boolean isStaff = courseAccessPolicy.isStaff(membership);
        if (!isCreator && !isStaff) {
            throw new CourseworkAccessException("You do not have permission to modify this post");
        }

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
        if (request.getPinned() != null) {
            if (!isStaff) {
                throw new CourseworkAccessException("Only space staff can pin or unpin posts");
            }
            coursework.setPinned(request.getPinned());
        }
        if (request.getAttachments() != null) {
            coursework.setAttachments(request.getAttachments());
        }
        Coursework saved = courseworkRepository.save(coursework);
        UserProfile profile = saved.getCreatorId() != null && profilePort != null
                ? profilePort.findByUserId(saved.getCreatorId()).orElse(null)
                : null;
        return toResponse(saved, profile);
    }

    public void archive(String courseId, String courseworkId, Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication,
                () -> new CourseworkAccessException("Authentication required"));
        requireActiveCourse(courseId);
        CourseMembership membership = courseAccessPolicy.requireActiveMember(courseId, userId,
                CourseworkNotFoundException::new);
        Coursework coursework = courseworkRepository.findByIdAndCourseId(courseworkId, courseId)
                .orElseThrow(CourseworkNotFoundException::new);

        boolean isCreator = userId.equals(coursework.getCreatorId());
        boolean isStaff = courseAccessPolicy.isStaff(membership);
        if (!isCreator && !isStaff) {
            throw new CourseworkAccessException("You do not have permission to delete this post");
        }

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

    public CourseworkResponse toResponse(Coursework coursework) {
        return toResponse(coursework, null);
    }

    public CourseworkResponse toResponse(Coursework coursework, UserProfile creatorProfile) {
        CourseworkResponse response = new CourseworkResponse();
        response.setId(coursework.getId());
        response.setCourseId(coursework.getCourseId());
        response.setCreatorId(coursework.getCreatorId());
        if (creatorProfile != null) {
            String name = creatorProfile.getDisplayName();
            if (name == null || name.isBlank()) {
                String first = creatorProfile.getFirstName() != null ? creatorProfile.getFirstName().trim() : "";
                String last = creatorProfile.getLastName() != null ? creatorProfile.getLastName().trim() : "";
                String full = (first + " " + last).trim();
                name = !full.isEmpty() ? full
                        : (creatorProfile.getHandle() != null ? creatorProfile.getHandle() : "Member");
            }
            response.setCreatorName(name);
            response.setCreatorAvatarUrl(creatorProfile.getAvatarUrl());
            response.setCreatorHandle(creatorProfile.getHandle());
        }
        response.setType(coursework.getType());
        response.setTitle(coursework.getTitle());
        response.setDescription(coursework.getDescription());
        response.setStatus(coursework.getStatus());
        response.setPinned(coursework.isPinned());
        response.setAttachments(
                coursework.getAttachments() != null ? coursework.getAttachments() : Collections.emptyList());
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
