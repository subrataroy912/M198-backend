/**
 * CREATED BY : SUBRATA ROY
 * SERVICE    : CourseService
 * PURPOSE    : Handles course creation, enrollment, roster management, ownership rules,
 *              and lifecycle updates for classroom operations.
 *
 * This service is responsible for teacher/student course interaction.
 * It validates memberships, enforces access control, and keeps course data consistent.
 */
package com.M198.Majorproject.core.course.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.core.course.dto.CourseCoverUploadResponse;
import com.M198.Majorproject.core.course.dto.CourseMemberResponse;
import com.M198.Majorproject.core.course.dto.CourseResponse;
import com.M198.Majorproject.core.course.dto.CreateCourseRequest;
import com.M198.Majorproject.core.course.dto.EnrollCourseRequest;
import com.M198.Majorproject.core.course.dto.PublicCourseResponse;
import com.M198.Majorproject.core.course.dto.UpdateCourseRequest;
import com.M198.Majorproject.core.course.dto.UpdateMemberRoleRequest;
import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.core.course.entity.EnrollmentCode;
import com.M198.Majorproject.core.course.entity.MeetingType;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.entity.SpaceType;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.core.course.repository.EnrollmentCodeRepository;
import com.M198.Majorproject.core.course.port.CourseDiscoveryPort;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import org.springframework.web.multipart.MultipartFile;

@Service

public class CourseLifecycleService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CourseLifecycleService.class);

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository membershipRepository;
    private final EnrollmentCodeRepository enrollmentCodeRepository;
    private final CourseDiscoveryPort courseDiscoveryPort;
    private final CourseProfilePort courseProfilePort;
    private final CourseDeletionCleanupService deletionCleanupService;
    private final CourseMediaService mediaService;
    private final CourseAccessPolicy courseAccessPolicy;

    @Autowired
    public CourseLifecycleService(
            CourseRepository courseRepository,
            CourseMembershipRepository membershipRepository,
            EnrollmentCodeRepository enrollmentCodeRepository,
            CourseDiscoveryPort courseDiscoveryPort,
            CourseProfilePort courseProfilePort,
            CourseDeletionCleanupService deletionCleanupService,
            CourseMediaService mediaService,
            CourseAccessPolicy courseAccessPolicy) {
        this.courseRepository = courseRepository;
        this.membershipRepository = membershipRepository;
        this.enrollmentCodeRepository = enrollmentCodeRepository;
        this.courseDiscoveryPort = courseDiscoveryPort;
        this.courseProfilePort = courseProfilePort;
        this.deletionCleanupService = deletionCleanupService;
        this.mediaService = mediaService;
        this.courseAccessPolicy = courseAccessPolicy;
    }

    public CourseCoverUploadResponse requestCoverUpload(Authentication authentication) {
        courseAccessPolicy.authenticatedUserId(authentication,
                () -> new CourseService.CourseAccessException("Authentication required"));
        return mediaService.requestUpload(CourseMediaService.Asset.COVER);
    }

    public CourseCoverUploadResponse requestLogoUpload(Authentication authentication) {
        courseAccessPolicy.authenticatedUserId(authentication,
                () -> new CourseService.CourseAccessException("Authentication required"));
        return mediaService.requestUpload(CourseMediaService.Asset.LOGO);
    }

    public CourseResponse createCourse(
            Authentication authentication,
            CreateCourseRequest request,
            MultipartFile coverFile,
            MultipartFile logoFile) {
        mediaService.applyMultipartAssets(request, coverFile, logoFile);
        return createCourse(authentication, request);
    }

    public CourseResponse createCourse(Authentication authentication, CreateCourseRequest request) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        requireCanCreateCourse(authentication, userId);

        CourseAccessType accessType = request.getAccessType();
        CourseVisibility visibility = request.getVisibility();

        if (accessType == null) {
            if (visibility == CourseVisibility.PUBLIC) {
                accessType = CourseAccessType.OPEN;
            } else {
                accessType = CourseAccessType.CODE;
                visibility = CourseVisibility.PRIVATE;
            }
        } else {
            if (visibility == null) {
                visibility = accessType == CourseAccessType.OPEN ? CourseVisibility.PUBLIC : CourseVisibility.PRIVATE;
            }
        }
        boolean isInvite = accessType == CourseAccessType.INVITE;
        boolean enrollmentEnabled = !isInvite;

        SpaceType spaceType = request.getSpaceType() != null ? request.getSpaceType() : SpaceType.ACADEMIC_CLASS;
        MeetingType meetingType = request.getMeetingType() != null ? request.getMeetingType() : MeetingType.IN_PERSON;

        String coverUrl = mediaService.resolveAsset(request.getCoverUrl(), CourseMediaService.Asset.COVER);
        String logoUrl = mediaService.resolveAsset(request.getLogoUrl(), CourseMediaService.Asset.LOGO);

        Course course = courseRepository.save(Course.builder()
                .ownerId(userId)
                .title(normalizeRequired(request.getTitle()))
                .spaceType(spaceType)
                .section(normalize(request.getSection()))
                .subject(normalize(request.getSubject()))
                .description(normalize(request.getDescription()))
                .coverUrl(coverUrl)
                .logoUrl(logoUrl)
                .theme(normalize(request.getTheme()))
                .meetingType(meetingType)
                .location(normalize(request.getLocation()))
                .tags(request.getTags() != null ? request.getTags() : Collections.emptyList())
                .links(request.getLinks() != null ? request.getLinks() : Collections.emptyList())
                .accessType(accessType)
                .visibility(visibility)
                .enrollmentEnabled(enrollmentEnabled)
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
            String enrollmentCodeValue = null;
            if (!isInvite) {
                EnrollmentCode enrollmentCode = enrollmentCodeRepository.save(EnrollmentCode.builder()
                        .courseId(course.getId())
                        .code(generateEnrollmentCode())
                        .createdBy(userId)
                        .active(true)
                        .build());
                enrollmentCodeValue = enrollmentCode.getCode();
            }
            CourseResponse response = toResponse(course);
            response.setEnrollmentCode(enrollmentCodeValue);
            syncDiscovery(course);
            return response;
        } catch (RuntimeException exception) {
            compensateCourseCreation(course, ownerMembership);
            throw exception;
        }
    }

    private void compensateCourseCreation(Course course, CourseMembership ownerMembership) {
        try {
            membershipRepository.delete(ownerMembership);
        } finally {
            courseRepository.delete(course);
        }
    }

    public List<CourseResponse> listMyCourses(Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        List<CourseMembership> memberships = membershipRepository.findAllByUserIdAndStatus(userId,
                MembershipStatus.ACTIVE);
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> courseIds = memberships.stream()
                .map(CourseMembership::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Course> activeCourses = courseRepository.findAllByIdInAndStatus(courseIds, CourseStatus.ACTIVE);
        if (activeCourses.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Course> courseMap = activeCourses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

        Set<String> activeCourseIds = courseMap.keySet();

        // Batch fetch owner profiles
        Set<String> ownerIds = activeCourses.stream()
                .map(Course::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, UserProfile> profileMap = Collections.emptyMap();
        if (!ownerIds.isEmpty()) {
            profileMap = courseProfilePort.findAllByUserIdIn(ownerIds).stream()
                    .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));
        }

        // Batch fetch member counts
        Map<String, Long> memberCountMap = Collections.emptyMap();
        if (!activeCourseIds.isEmpty()) {
            List<CourseMembership> allCourseMemberships = membershipRepository
                    .findAllByCourseIdInAndStatus(activeCourseIds, MembershipStatus.ACTIVE);
            memberCountMap = allCourseMemberships.stream()
                    .collect(Collectors.groupingBy(CourseMembership::getCourseId, Collectors.counting()));
        }

        // Batch fetch active enrollment codes
        Map<String, String> enrollmentCodeMap = Collections.emptyMap();
        if (!activeCourseIds.isEmpty()) {
            List<EnrollmentCode> codes = enrollmentCodeRepository.findAllByCourseIdInAndActiveTrue(activeCourseIds);
            enrollmentCodeMap = codes.stream()
                    .collect(Collectors.toMap(EnrollmentCode::getCourseId, EnrollmentCode::getCode, (a, b) -> a));
        }

        // Assemble responses preserving membership order
        List<CourseResponse> responses = new ArrayList<>();
        for (CourseMembership membership : memberships) {
            Course course = courseMap.get(membership.getCourseId());
            if (course == null) {
                continue;
            }
            UserProfile ownerProfile = profileMap.get(course.getOwnerId());
            long count = memberCountMap.getOrDefault(course.getId(), 0L);
            String enrollmentCode = enrollmentCodeMap.get(course.getId());

            CourseResponse res = toResponse(course, ownerProfile, count, enrollmentCode);
            if (membership.getRole() != null) {
                res.setRole(membership.getRole().name());
            }
            res.setEnrolled(true);
            responses.add(res);
        }
        return responses;
    }

    public CourseResponse getCourse(String courseId, Authentication authentication) {
        validateCourseId(courseId);
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        Course course = courseRepository.findById(courseId).orElse(null);
        var membership = membershipRepository.findByCourseIdAndUserId(courseId, userId);
        boolean isStaffOrEnrolled = membership.filter(value -> value.getStatus() == MembershipStatus.ACTIVE)
                .isPresent();
        boolean isPublicCourse = course != null && (course.getVisibility() == CourseVisibility.PUBLIC
                || course.getAccessType() == CourseAccessType.OPEN);

        if (course == null || course.getStatus() != CourseStatus.ACTIVE || (!isStaffOrEnrolled && !isPublicCourse)) {
            logger.warn(
                    "Course access denied: courseId={}, userId={}, courseExists={}, courseStatus={}, membershipExists={}, membershipStatus={}",
                    courseId, userId, course != null, course == null ? null : course.getStatus(),
                    membership.isPresent(),
                    membership.map(CourseMembership::getStatus).orElse(null));
            throw new CourseService.CourseNotFoundException();
        }
        CourseResponse response = toResponse(course);
        if (isStaffOrEnrolled) {
            response.setRole(membership.get().getRole() != null ? membership.get().getRole().name() : "MEMBER");
            response.setEnrolled(true);
        } else {
            response.setRole("VIEWER");
            response.setEnrolled(false);
            response.setEnrollmentCode(null);
        }
        return response;
    }

    public PublicCourseResponse getPublicCourse(String courseId) {
        validateCourseId(courseId);
        Course course = courseRepository.findByIdAndStatus(courseId, CourseStatus.ACTIVE)
                .filter(value -> value.getVisibility() == CourseVisibility.PUBLIC)
                .orElseThrow(CourseService.CourseNotFoundException::new);
        PublicCourseResponse response = new PublicCourseResponse();
        response.setId(course.getId());
        response.setTitle(course.getTitle());
        response.setSpaceType(course.getSpaceType() != null ? course.getSpaceType() : SpaceType.ACADEMIC_CLASS);
        response.setSection(course.getSection());
        response.setSubject(course.getSubject());
        response.setDescription(course.getDescription());
        response.setCoverUrl(course.getCoverUrl());
        response.setLogoUrl(course.getLogoUrl());
        response.setTheme(course.getTheme());
        response.setMeetingType(course.getMeetingType() != null ? course.getMeetingType() : MeetingType.IN_PERSON);
        response.setLocation(course.getLocation());
        response.setTags(course.getTags());
        response.setVisibility(course.getVisibility());
        response.setAccessType(course.getAccessType() != null
                ? course.getAccessType()
                : (course.getVisibility() == CourseVisibility.PUBLIC ? CourseAccessType.OPEN : CourseAccessType.CODE));
        response.setEnrollmentEnabled(course.isEnrollmentEnabled());
        response.setMemberCount(membershipRepository.countByCourseIdAndStatus(courseId, MembershipStatus.ACTIVE));
        return response;
    }

    public CourseResponse enrollByCode(Authentication authentication, String code) {
        if (code == null || code.isBlank()) {
            throw new CourseService.CourseAccessException("Enrollment code is required");
        }
        EnrollmentCode enrollmentCode = enrollmentCodeRepository.findByCodeAndActiveTrue(code.trim().toUpperCase())
                .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new CourseService.CourseAccessException("Invalid enrollment code"));

        return enroll(enrollmentCode.getCourseId(), authentication, new EnrollCourseRequest(code.trim().toUpperCase()));
    }

    public CourseResponse enroll(String courseId, Authentication authentication, EnrollCourseRequest request) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        Course course = activeCourse(courseId);
        if (!course.isEnrollmentEnabled()) {
            throw new CourseService.CourseAccessException("Enrollment is disabled");
        }
        CourseAccessType accessType = course.getAccessType() != null
                ? course.getAccessType()
                : (course.getVisibility() == CourseVisibility.PUBLIC ? CourseAccessType.OPEN : CourseAccessType.CODE);

        if (accessType == CourseAccessType.INVITE) {
            throw new CourseService.CourseAccessException(
                    "This class is invite-only. Please request an invitation from the instructor.");
        }

        boolean isPublicCourse = course.getVisibility() == CourseVisibility.PUBLIC;

        if (accessType == CourseAccessType.CODE && !isPublicCourse) {
            String code = request != null && request.getCode() != null ? request.getCode().trim() : "";
            if (code.isEmpty()) {
                throw new CourseService.CourseAccessException("Enrollment code is required");
            }
            enrollmentCodeRepository.findByCodeAndActiveTrue(code.toUpperCase())
                    .filter(value -> value.getCourseId().equals(courseId))
                    .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(Instant.now()))
                    .orElseThrow(() -> new CourseService.CourseAccessException("Invalid enrollment code"));
        } else if (accessType == CourseAccessType.OPEN || isPublicCourse) {
            if (request != null && request.getCode() != null && !request.getCode().trim().isEmpty()) {
                enrollmentCodeRepository.findByCodeAndActiveTrue(request.getCode().trim().toUpperCase())
                        .filter(value -> value.getCourseId().equals(courseId))
                        .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(Instant.now()))
                        .orElseThrow(() -> new CourseService.CourseAccessException("Invalid enrollment code"));
            }
        }
        var existingMembership = membershipRepository.findByCourseIdAndUserId(courseId, userId);
        if (existingMembership.filter(value -> value.getStatus() == MembershipStatus.ACTIVE).isPresent()) {
            CourseResponse res = toResponse(course);
            if (existingMembership.get().getRole() != null) {
                res.setRole(existingMembership.get().getRole().name());
            }
            res.setEnrolled(true);
            return res;
        }
        if (existingMembership.isPresent()) {
            CourseMembership membership = existingMembership.get();
            membership.setRole(MembershipRole.MEMBER);
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setJoinedAt(Instant.now());
            membership.setRemovedAt(null);
            membershipRepository.save(membership);
            syncDiscovery(course);
            return toResponse(course);
        }
        try {
            membershipRepository.save(CourseMembership.builder()
                    .courseId(courseId)
                    .userId(userId)
                    .role(MembershipRole.MEMBER)
                    .status(MembershipStatus.ACTIVE)
                    .joinedAt(Instant.now())
                    .build());
        } catch (DuplicateKeyException exception) {
            CourseResponse res = toResponse(course);
            res.setRole("MEMBER");
            res.setEnrolled(true);
            return res;
        }
        syncDiscovery(course);
        return toResponse(course);
    }

    public void leave(String courseId, Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        activeCourse(courseId);
        CourseMembership membership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new CourseService.CourseNotFoundException());
        if (membership.getRole() == MembershipRole.OWNER) {
            throw new CourseService.CourseConflictException("Course owner cannot leave the course");
        }
        membership.setStatus(MembershipStatus.LEFT);
        membership.setRemovedAt(Instant.now());
        membershipRepository.save(membership);
        syncDiscovery(activeCourse(courseId));
    }

    public void removeMember(String courseId, String memberUserId, Authentication authentication) {
        String currentUserId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        Course course = activeCourse(courseId);
        CourseMembership callerMembership = courseAccessPolicy.requireAdminOrOwner(
                courseId, currentUserId,
                () -> new CourseService.CourseAccessException("Course membership required"),
                () -> new CourseService.CourseAccessException("Course owner or admin role required"));

        CourseMembership targetMembership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, memberUserId, MembershipStatus.ACTIVE)
                .orElseThrow(CourseService.CourseNotFoundException::new);

        if (targetMembership.getRole() == MembershipRole.OWNER) {
            throw new CourseService.CourseConflictException("Course owner cannot be removed");
        }

        if (callerMembership.getRole() != MembershipRole.OWNER && targetMembership.getRole() == MembershipRole.ADMIN) {
            throw new CourseService.CourseAccessException("Only course owner can remove an administrator");
        }

        targetMembership.setStatus(MembershipStatus.REMOVED);
        targetMembership.setRemovedAt(Instant.now());
        membershipRepository.save(targetMembership);
        syncDiscovery(course);
    }

    public CourseResponse update(
            String courseId,
            Authentication authentication,
            UpdateCourseRequest request,
            MultipartFile coverFile,
            MultipartFile logoFile) {
        mediaService.applyMultipartAssets(request, coverFile, logoFile);
        return update(courseId, authentication, request);
    }

    public CourseResponse update(String courseId, Authentication authentication, UpdateCourseRequest request) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        Course course = activeCourse(courseId);
        courseAccessPolicy.requireAdminOrOwner(courseId, userId, () -> new CourseService.CourseAccessException("Course membership required"), () -> new CourseService.CourseAccessException("Course owner or admin role required"));
        if (request.getTitle() != null) {
            course.setTitle(normalizeRequired(request.getTitle()));
        }
        if (request.getSection() != null) {
            course.setSection(normalize(request.getSection()));
        }
        if (request.getSubject() != null) {
            course.setSubject(normalize(request.getSubject()));
        }
        if (request.getDescription() != null) {
            course.setDescription(normalize(request.getDescription()));
        }
        if (request.getAccessType() != null) {
            course.setAccessType(request.getAccessType());
            if (null != request.getAccessType())
                switch (request.getAccessType()) {
                    case INVITE -> {
                        course.setVisibility(CourseVisibility.PRIVATE);
                        course.setEnrollmentEnabled(false);
                    }
                    case OPEN -> {
                        course.setVisibility(CourseVisibility.PUBLIC);
                        course.setEnrollmentEnabled(true);
                    }
                    case CODE -> {
                        course.setVisibility(CourseVisibility.PRIVATE);
                        course.setEnrollmentEnabled(true);
                    }
                    default -> {
                    }
                }
        }
        if (request.getVisibility() != null) {
            course.setVisibility(request.getVisibility());
        }
        if (request.getEnrollmentEnabled() != null) {
            course.setEnrollmentEnabled(request.getEnrollmentEnabled());
        }
        if (request.getCoverUrl() != null) {
            course.setCoverUrl(mediaService.resolveAsset(request.getCoverUrl(), CourseMediaService.Asset.COVER));
        }
        if (request.getLogoUrl() != null) {
            course.setLogoUrl(mediaService.resolveAsset(request.getLogoUrl(), CourseMediaService.Asset.LOGO));
        }
        if (request.getTheme() != null) {
            course.setTheme(normalize(request.getTheme()));
        }
        if (request.getSpaceType() != null) {
            course.setSpaceType(request.getSpaceType());
        }
        if (request.getMeetingType() != null) {
            course.setMeetingType(request.getMeetingType());
        }
        if (request.getLocation() != null) {
            course.setLocation(normalize(request.getLocation()));
        }
        if (request.getTags() != null) {
            course.setTags(request.getTags());
        }
        if (request.getLinks() != null) {
            course.setLinks(request.getLinks());
        }
        Course savedCourse = courseRepository.save(course);
        syncDiscovery(savedCourse);
        return toResponse(savedCourse);
    }

    public void archive(String courseId, Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        Course course = activeCourse(courseId);
        courseAccessPolicy.requireAdminOrOwner(courseId, userId, () -> new CourseService.CourseAccessException("Course membership required"), () -> new CourseService.CourseAccessException("Course owner or admin role required"));
        course.setStatus(CourseStatus.ARCHIVED);
        course.setArchivedAt(Instant.now());
        Course archivedCourse = courseRepository.save(course);
        syncDiscovery(archivedCourse);
    }

    public void deleteCourse(String courseId, Authentication authentication) {
        validateCourseId(courseId);
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(CourseService.CourseNotFoundException::new);
        requireOwnerOrAdmin(course, userId, authentication);

        deletionCleanupService.clean(courseId);

        courseRepository.deleteById(courseId);

        logger.info("Permanently deleted course {} and cascaded all associated dependents by user {}", courseId,
                userId);
    }

    public List<CourseMemberResponse> roster(String courseId, Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        activeCourse(courseId);
        if (!hasAuthority(authentication, "ROLE_ADMIN")) {
            courseAccessPolicy.requireActiveMember(courseId, userId, CourseService.CourseNotFoundException::new);
        }
        List<CourseMembership> memberships = membershipRepository.findAllByCourseIdAndStatus(courseId,
                MembershipStatus.ACTIVE);
        Map<String, UserProfile> profileMap = new HashMap<>();
         {
            List<String> userIds = memberships.stream().map(CourseMembership::getUserId).toList();
            if (!userIds.isEmpty()) {
                courseProfilePort.findAllByUserIdIn(userIds).forEach(p -> profileMap.put(p.getUserId(), p));
            }
        }
        return memberships.stream()
                .map(membership -> {
                    CourseMemberResponse response = new CourseMemberResponse();
                    response.setUserId(membership.getUserId());
                    response.setRole(membership.getRole());
                    response.setJoinedAt(membership.getJoinedAt());
                    UserProfile profile = profileMap.get(membership.getUserId());
                    if (profile != null) {
                        response.setName(profile.getDisplayName());
                        response.setAvatarUrl(profile.getAvatarUrl());
                    }
                    return response;
                }).toList();
    }

    public CourseMemberResponse updateMemberRole(
            String courseId,
            String targetUserId,
            UpdateMemberRoleRequest request,
            Authentication authentication) {
        String currentUserId = courseAccessPolicy.authenticatedUserId(
                authentication, () -> new CourseService.CourseAccessException("Authentication required"));
        Course course = activeCourse(courseId);

        boolean isGlobalAdmin = hasAuthority(authentication, "ROLE_ADMIN");
        CourseMembership callerMembership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, currentUserId, MembershipStatus.ACTIVE)
                .orElse(null);

        boolean isOwner = (callerMembership != null && callerMembership.getRole() == MembershipRole.OWNER)
                || (course.getOwnerId() != null && course.getOwnerId().equals(currentUserId));

        if (!isGlobalAdmin && !isOwner) {
            throw new CourseService.CourseAccessException("Only the space owner can update member roles");
        }

        CourseMembership targetMembership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, targetUserId, MembershipStatus.ACTIVE)
                .orElseThrow(CourseService.CourseNotFoundException::new);

        if (targetMembership.getRole() == MembershipRole.OWNER) {
            throw new CourseService.CourseConflictException("Cannot change role of the space owner");
        }

        MembershipRole newRole = request.getRole();
        if (newRole != MembershipRole.ADMIN && newRole != MembershipRole.MEMBER) {
            throw new CourseService.CourseBadRequestException("Role must be ADMIN or MEMBER");
        }

        targetMembership.setRole(newRole);
        targetMembership.setUpdatedAt(Instant.now());
        membershipRepository.save(targetMembership);

        CourseMemberResponse response = new CourseMemberResponse();
        response.setUserId(targetMembership.getUserId());
        response.setRole(targetMembership.getRole());
        response.setJoinedAt(targetMembership.getJoinedAt());
        courseProfilePort.findByUserId(targetUserId).ifPresent(p -> {
            response.setName(p.getDisplayName());
            response.setAvatarUrl(p.getAvatarUrl());
        });

        logger.info("Updated member role in course {}: targetUser={}, newRole={}, byUser={}",
                courseId, targetUserId, newRole, currentUserId);

        return response;
    }

    private Course activeCourse(String courseId) {
        return courseRepository.findByIdAndStatus(courseId, CourseStatus.ACTIVE)
                .orElseThrow(CourseService.CourseNotFoundException::new);
    }

    private void validateCourseId(String courseId) {
        if (courseId == null || courseId.isBlank() || !courseId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new CourseService.CourseIdFormatException();
        }
    }

    private void requireOwnerOrAdmin(Course course, String userId, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return;
        }
        if (course.getOwnerId() != null && course.getOwnerId().equals(userId)) {
            return;
        }
        CourseMembership membership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                course.getId(), userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new CourseService.CourseAccessException("Course membership required"));
        if (membership.getRole() != MembershipRole.OWNER) {
            throw new CourseService.CourseAccessException("Course owner or administrator role required");
        }
    }

    private void requireCanCreateCourse(Authentication authentication, String userId) {
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return;
        }
        if (hasAuthority(authentication, "ROLE_CREATOR")) {
            return;
        }
        if (userId != null) {
            boolean canCreate = courseProfilePort.findByUserId(userId)
                    .map(UserProfile::isCanCreateCourses)
                    .orElse(false);
            if (canCreate) {
                return;
            }
        }
        throw new CourseService.CourseAccessException("Course creation privileges required");
    }

    private boolean hasAuthority(Authentication authentication, String authorityName) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(authorityName));
    }

    private CourseResponse toResponse(Course course) {
        UserProfile ownerProfile = null;
        if (course.getOwnerId() != null) {
            ownerProfile = courseProfilePort.findByUserId(course.getOwnerId()).orElse(null);
        }
        long memberCount = 0;
        if (course.getId() != null) {
            memberCount = membershipRepository.countByCourseIdAndStatus(course.getId(), MembershipStatus.ACTIVE);
        }
        String enrollmentCode = null;
        if (course.getId() != null) {
            enrollmentCode = enrollmentCodeRepository.findByCourseIdAndActiveTrue(course.getId())
                    .map(EnrollmentCode::getCode).orElse(null);
        }
        return toResponse(course, ownerProfile, memberCount, enrollmentCode);
    }

    private CourseResponse toResponse(Course course, UserProfile ownerProfile, long memberCount,
            String enrollmentCode) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setOwnerId(course.getOwnerId());
        response.setRole("MEMBER");
        response.setEnrolled(true);
        if (ownerProfile != null) {
            response.setOwnerName(ownerProfile.getDisplayName());
            response.setOwnerAvatarUrl(ownerProfile.getAvatarUrl());
        }
        response.setMemberCount(memberCount);
        if (enrollmentCode != null) {
            response.setEnrollmentCode(enrollmentCode);
        }
        response.setTitle(course.getTitle());
        response.setSpaceType(course.getSpaceType() != null ? course.getSpaceType() : SpaceType.ACADEMIC_CLASS);
        response.setSection(course.getSection());
        response.setSubject(course.getSubject());
        response.setDescription(course.getDescription());
        response.setCoverUrl(course.getCoverUrl());
        response.setLogoUrl(course.getLogoUrl());
        response.setTheme(course.getTheme());
        response.setMeetingType(course.getMeetingType() != null ? course.getMeetingType() : MeetingType.IN_PERSON);
        response.setLocation(course.getLocation());
        response.setTags(course.getTags());
        response.setLinks(course.getLinks() != null ? course.getLinks() : Collections.emptyList());
        CourseAccessType accessType = course.getAccessType() != null
                ? course.getAccessType()
                : (course.getVisibility() == CourseVisibility.PUBLIC ? CourseAccessType.OPEN : CourseAccessType.CODE);
        response.setAccessType(accessType);
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

    /**
     * Keeps the public-course read model aligned with the source course. The
     * projection intentionally stores no private course content; explore
     * queries additionally filter it to PUBLIC and ACTIVE courses.
     */
    private void syncDiscovery(Course course) {
        courseDiscoveryPort.sync(course,
                membershipRepository.countByCourseIdAndStatus(course.getId(), MembershipStatus.ACTIVE));
    }

}
