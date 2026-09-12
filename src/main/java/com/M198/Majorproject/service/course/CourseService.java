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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.CourseCoverUploadResponse;
import com.M198.Majorproject.dto.CourseMemberResponse;
import com.M198.Majorproject.dto.CourseResponse;
import com.M198.Majorproject.dto.CreateCourseRequest;
import com.M198.Majorproject.dto.EnrollCourseRequest;
import com.M198.Majorproject.dto.PublicCourseResponse;
import com.M198.Majorproject.dto.UpdateCourseRequest;
import com.M198.Majorproject.entity.course.Course;
import com.M198.Majorproject.entity.course.CourseAccessType;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;
import com.M198.Majorproject.entity.course.EnrollmentCode;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.entity.explore.CourseDiscovery;
import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.course.CourseRepository;
import com.M198.Majorproject.repository.course.EnrollmentCodeRepository;
import com.M198.Majorproject.repository.explore.CourseDiscoveryRepository;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.repository.identity.UserProfileRepository;
import com.cloudinary.Cloudinary;

@Service

public class CourseService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository membershipRepository;
    private final EnrollmentCodeRepository enrollmentCodeRepository;
    private final CourseDiscoveryRepository courseDiscoveryRepository;
    private final UserProfileRepository userProfileRepository;
    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    @Autowired
    public CourseService(
            CourseRepository courseRepository,
            CourseMembershipRepository membershipRepository,
            EnrollmentCodeRepository enrollmentCodeRepository,
            CourseDiscoveryRepository courseDiscoveryRepository,
            UserProfileRepository userProfileRepository,
            Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.courseRepository = courseRepository;
        this.membershipRepository = membershipRepository;
        this.enrollmentCodeRepository = enrollmentCodeRepository;
        this.courseDiscoveryRepository = courseDiscoveryRepository;
        this.userProfileRepository = userProfileRepository;
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public CourseService(
            CourseRepository courseRepository,
            CourseMembershipRepository membershipRepository,
            EnrollmentCodeRepository enrollmentCodeRepository,
            CourseDiscoveryRepository courseDiscoveryRepository,
            Cloudinary cloudinary,
            String cloudName,
            String apiKey,
            String apiSecret) {
        this(courseRepository, membershipRepository, enrollmentCodeRepository, courseDiscoveryRepository, null, cloudinary, cloudName, apiKey, apiSecret);
    }

    public CourseService(
            CourseRepository courseRepository,
            CourseMembershipRepository membershipRepository,
            EnrollmentCodeRepository enrollmentCodeRepository) {
        this(courseRepository, membershipRepository, enrollmentCodeRepository, null, null, null, "", "", "");
    }

    public CourseCoverUploadResponse requestCoverUpload(Authentication authentication) {
        authenticatedUserId(authentication);
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new CourseAccessException("Cloudinary is not configured");
        }
        String publicId = "course_covers/" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("public_id", publicId);
        parameters.put("timestamp", timestamp);
        CourseCoverUploadResponse response = new CourseCoverUploadResponse();
        response.setUploadUrl("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
        response.setPublicId(publicId);
        response.setUploadApiKey(apiKey);
        response.setUploadSignature(cloudinary.apiSignRequest(parameters, apiSecret, 0));
        response.setUploadTimestamp(timestamp);
        return response;
    }

    public CourseResponse createCourse(Authentication authentication, CreateCourseRequest request) {
        String userId = authenticatedUserId(authentication);
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

        Course course = courseRepository.save(Course.builder()
                .ownerId(userId)
                .title(normalizeRequired(request.getTitle()))
                .section(normalize(request.getSection()))
                .subject(normalize(request.getSubject()))
                .description(normalize(request.getDescription()))
                .coverUrl(normalize(request.getCoverUrl()))
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
        String userId = authenticatedUserId(authentication);
        return membershipRepository.findAllByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
                .map(membership -> courseRepository.findByIdAndStatus(membership.getCourseId(), CourseStatus.ACTIVE)
                        .map(c -> {
                            CourseResponse res = toResponse(c);
                            if (membership.getRole() != null) {
                                res.setRole(membership.getRole().name());
                            }
                            res.setEnrolled(true);
                            return res;
                        }))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    public CourseResponse getCourse(String courseId, Authentication authentication) {
        validateCourseId(courseId);
        String userId = authenticatedUserId(authentication);
        Course course = courseRepository.findById(courseId).orElse(null);
        var membership = membershipRepository.findByCourseIdAndUserId(courseId, userId);
        boolean isStaffOrEnrolled = membership.filter(value -> value.getStatus() == MembershipStatus.ACTIVE).isPresent();
        boolean isPublicCourse = course != null && (course.getVisibility() == CourseVisibility.PUBLIC || course.getAccessType() == CourseAccessType.OPEN);

        if (course == null || course.getStatus() != CourseStatus.ACTIVE || (!isStaffOrEnrolled && !isPublicCourse)) {
            logger.warn(
                    "Course access denied: courseId={}, userId={}, courseExists={}, courseStatus={}, membershipExists={}, membershipStatus={}",
                    courseId, userId, course != null, course == null ? null : course.getStatus(),
                    membership.isPresent(),
                    membership.map(CourseMembership::getStatus).orElse(null));
            throw new CourseNotFoundException();
        }
        CourseResponse response = toResponse(course);
        if (isStaffOrEnrolled) {
            response.setRole(membership.get().getRole() != null ? membership.get().getRole().name() : "STUDENT");
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
                .orElseThrow(CourseNotFoundException::new);
        PublicCourseResponse response = new PublicCourseResponse();
        response.setId(course.getId());
        response.setTitle(course.getTitle());
        response.setSection(course.getSection());
        response.setSubject(course.getSubject());
        response.setDescription(course.getDescription());
        response.setCoverUrl(course.getCoverUrl());
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
            throw new CourseAccessException("Enrollment code is required");
        }
        EnrollmentCode enrollmentCode = enrollmentCodeRepository.findByCodeAndActiveTrue(code.trim().toUpperCase())
                .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new CourseAccessException("Invalid enrollment code"));

        return enroll(enrollmentCode.getCourseId(), authentication, new EnrollCourseRequest(code.trim().toUpperCase()));
    }

    public CourseResponse enroll(String courseId, Authentication authentication, EnrollCourseRequest request) {
        String userId = authenticatedUserId(authentication);
        requireAnyRole(authentication, AccountType.STUDENT, AccountType.TEACHER, AccountType.ADMIN);
        Course course = activeCourse(courseId);
        if (!course.isEnrollmentEnabled()) {
            throw new CourseAccessException("Enrollment is disabled");
        }
        CourseAccessType accessType = course.getAccessType() != null
                ? course.getAccessType()
                : (course.getVisibility() == CourseVisibility.PUBLIC ? CourseAccessType.OPEN : CourseAccessType.CODE);

        if (accessType == CourseAccessType.INVITE) {
            throw new CourseAccessException("This class is invite-only. Please request an invitation from the instructor.");
        }

        boolean isPublicCourse = course.getVisibility() == CourseVisibility.PUBLIC;

        if (accessType == CourseAccessType.CODE && !isPublicCourse) {
            String code = request != null && request.getCode() != null ? request.getCode().trim() : "";
            if (code.isEmpty()) {
                throw new CourseAccessException("Enrollment code is required");
            }
            enrollmentCodeRepository.findByCodeAndActiveTrue(code.toUpperCase())
                    .filter(value -> value.getCourseId().equals(courseId))
                    .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(Instant.now()))
                    .orElseThrow(() -> new CourseAccessException("Invalid enrollment code"));
        } else if (accessType == CourseAccessType.OPEN || isPublicCourse) {
            if (request != null && request.getCode() != null && !request.getCode().trim().isEmpty()) {
                enrollmentCodeRepository.findByCodeAndActiveTrue(request.getCode().trim().toUpperCase())
                        .filter(value -> value.getCourseId().equals(courseId))
                        .filter(value -> value.getExpiresAt() == null || value.getExpiresAt().isAfter(Instant.now()))
                        .orElseThrow(() -> new CourseAccessException("Invalid enrollment code"));
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
            membership.setRole(MembershipRole.STUDENT);
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
                    .role(MembershipRole.STUDENT)
                    .status(MembershipStatus.ACTIVE)
                    .joinedAt(Instant.now())
                    .build());
        } catch (DuplicateKeyException exception) {
            CourseResponse res = toResponse(course);
            res.setRole("STUDENT");
            res.setEnrolled(true);
            return res;
        }
        syncDiscovery(course);
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
        syncDiscovery(activeCourse(courseId));
    }

    public CourseResponse update(String courseId, Authentication authentication, UpdateCourseRequest request) {
        String userId = authenticatedUserId(authentication);
        Course course = activeCourse(courseId);
        requireOwnerOrTeacher(courseId, userId);
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
            if (request.getAccessType() == CourseAccessType.INVITE) {
                course.setVisibility(CourseVisibility.PRIVATE);
                course.setEnrollmentEnabled(false);
            } else if (request.getAccessType() == CourseAccessType.OPEN) {
                course.setVisibility(CourseVisibility.PUBLIC);
                course.setEnrollmentEnabled(true);
            } else if (request.getAccessType() == CourseAccessType.CODE) {
                course.setVisibility(CourseVisibility.PRIVATE);
                course.setEnrollmentEnabled(true);
            }
        }
        if (request.getVisibility() != null) {
            course.setVisibility(request.getVisibility());
        }
        if (request.getEnrollmentEnabled() != null) {
            course.setEnrollmentEnabled(request.getEnrollmentEnabled());
        }
        if (request.getCoverUrl() != null) {
            course.setCoverUrl(normalize(request.getCoverUrl()));
        }
        Course savedCourse = courseRepository.save(course);
        syncDiscovery(savedCourse);
        return toResponse(savedCourse);
    }

    public void archive(String courseId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        Course course = activeCourse(courseId);
        requireOwnerOrTeacher(courseId, userId);
        course.setStatus(CourseStatus.ARCHIVED);
        course.setArchivedAt(Instant.now());
        Course archivedCourse = courseRepository.save(course);
        syncDiscovery(archivedCourse);
    }

    public List<CourseMemberResponse> roster(String courseId, Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        activeCourse(courseId);
        if (!hasRole(authentication, AccountType.ADMIN)) {
            requireActiveMember(courseId, userId);
        }
        List<CourseMembership> memberships = membershipRepository.findAllByCourseIdAndStatus(courseId, MembershipStatus.ACTIVE);
        Map<String, UserProfile> profileMap = new HashMap<>();
        if (userProfileRepository != null) {
            List<String> userIds = memberships.stream().map(CourseMembership::getUserId).toList();
            if (!userIds.isEmpty()) {
                userProfileRepository.findAllByUserIdIn(userIds).forEach(p -> profileMap.put(p.getUserId(), p));
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

    private Course activeCourse(String courseId) {
        return courseRepository.findByIdAndStatus(courseId, CourseStatus.ACTIVE)
                .orElseThrow(CourseNotFoundException::new);
    }

    private void validateCourseId(String courseId) {
        if (courseId == null || courseId.isBlank() || !courseId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new CourseIdFormatException();
        }
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

    private void requireCanCreateCourse(Authentication authentication, String userId) {
        if (hasRole(authentication, AccountType.TEACHER) || hasRole(authentication, AccountType.ADMIN)) {
            return;
        }
        if (hasAuthority(authentication, "ROLE_CREATOR")) {
            return;
        }
        if (userProfileRepository != null && userId != null) {
            boolean canCreate = userProfileRepository.findByUserId(userId)
                    .map(UserProfile::isCanCreateCourses)
                    .orElse(false);
            if (canCreate) {
                return;
            }
        }
        throw new CourseAccessException("Course creation privileges required");
    }

    private boolean hasAuthority(Authentication authentication, String authorityName) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(authorityName));
    }

    private void requireAnyRole(Authentication authentication, AccountType... roles) {
        for (AccountType role : roles) {
            if (hasRole(authentication, role)) {
                return;
            }
        }
        throw new CourseAccessException("Teacher or administrator role required");
    }

    private boolean hasRole(Authentication authentication, AccountType role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    private CourseResponse toResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setOwnerId(course.getOwnerId());
        response.setRole("STUDENT");
        response.setEnrolled(true);
        if (userProfileRepository != null && course.getOwnerId() != null) {
            userProfileRepository.findByUserId(course.getOwnerId()).ifPresent(p -> {
                response.setOwnerName(p.getDisplayName());
                response.setOwnerAvatarUrl(p.getAvatarUrl());
            });
        }
        if (membershipRepository != null && course.getId() != null) {
            response.setMemberCount(membershipRepository.countByCourseIdAndStatus(course.getId(), MembershipStatus.ACTIVE));
        }
        if (enrollmentCodeRepository != null && course.getId() != null) {
            enrollmentCodeRepository.findByCourseIdAndActiveTrue(course.getId())
                    .ifPresent(ec -> response.setEnrollmentCode(ec.getCode()));
        }
        response.setTitle(course.getTitle());
        response.setSection(course.getSection());
        response.setSubject(course.getSubject());
        response.setDescription(course.getDescription());
        response.setCoverUrl(course.getCoverUrl());
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
        if (courseDiscoveryRepository == null) {
            return;
        }
        CourseDiscovery discovery = courseDiscoveryRepository.findByCourseId(course.getId())
                .orElseGet(CourseDiscovery::new);
        discovery.setCourseId(course.getId());
        discovery.setTitle(course.getTitle());
        discovery.setSubject(course.getSubject());
        discovery.setVisibility(course.getVisibility());
        discovery.setAccessType(course.getAccessType() != null ? course.getAccessType() : CourseAccessType.OPEN);
        discovery.setStatus(course.getStatus());
        discovery.setEnrollmentCount(
                membershipRepository.countByCourseIdAndStatus(course.getId(), MembershipStatus.ACTIVE));
        discovery.setLastActivityAt(Instant.now());
        courseDiscoveryRepository.save(discovery);
    }

    public static class CourseNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class CourseIdFormatException extends RuntimeException {

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
