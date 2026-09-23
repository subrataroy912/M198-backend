package com.M198.Majorproject.core.course.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import com.M198.Majorproject.core.course.dto.*;

/**
 * Application-facing compatibility facade for the existing course HTTP API.
 * Course controllers can retain this stable API while lifecycle work is isolated
 * from integration adapters.
 */
@Service
public class CourseService {
    private final CourseLifecycleService lifecycle;
    private final CourseEnrollmentService enrollment;
    public CourseService(CourseLifecycleService lifecycle, CourseEnrollmentService enrollment) { this.lifecycle = lifecycle; this.enrollment = enrollment; }
    public CourseCoverUploadResponse requestCoverUpload(Authentication a) { return lifecycle.requestCoverUpload(a); }
    public CourseCoverUploadResponse requestLogoUpload(Authentication a) { return lifecycle.requestLogoUpload(a); }
    public CourseResponse createCourse(Authentication a, CreateCourseRequest r) { return lifecycle.createCourse(a, r); }
    public CourseResponse createCourse(Authentication a, CreateCourseRequest r, MultipartFile c, MultipartFile l) { return lifecycle.createCourse(a, r, c, l); }
    public List<CourseResponse> listMyCourses(Authentication a) { return lifecycle.listMyCourses(a); }
    public CourseResponse getCourse(String id, Authentication a) { return lifecycle.getCourse(id, a); }
    public PublicCourseResponse getPublicCourse(String id) { return lifecycle.getPublicCourse(id); }
    public CourseResponse enrollByCode(Authentication a, String code) { return enrollment.enrollByCode(a, code); }
    public CourseResponse enroll(String id, Authentication a, EnrollCourseRequest r) { return enrollment.enroll(id, a, r); }
    public void leave(String id, Authentication a) { enrollment.leave(id, a); }
    public void removeMember(String id, String userId, Authentication a) { enrollment.removeMember(id, userId, a); }
    public CourseResponse update(String id, Authentication a, UpdateCourseRequest r) { return lifecycle.update(id, a, r); }
    public CourseResponse update(String id, Authentication a, UpdateCourseRequest r, MultipartFile c, MultipartFile l) { return lifecycle.update(id, a, r, c, l); }
    public void archive(String id, Authentication a) { lifecycle.archive(id, a); }
    public void deleteCourse(String id, Authentication a) { lifecycle.deleteCourse(id, a); }
    public List<CourseMemberResponse> roster(String id, Authentication a) { return lifecycle.roster(id, a); }
    public CourseMemberResponse updateMemberRole(String id, String userId, UpdateMemberRoleRequest r, Authentication a) { return lifecycle.updateMemberRole(id, userId, r, a); }
    public void cancelJoinRequest(String id, Authentication a) { lifecycle.cancelJoinRequest(id, a); }
    public List<JoinRequestResponse> listPendingRequests(String id, Authentication a) { return lifecycle.listPendingRequests(id, a); }
    public CourseResponse approveJoinRequest(String id, String userId, Authentication a) { return lifecycle.approveJoinRequest(id, userId, a); }
    public CourseResponse declineJoinRequest(String id, String userId, Authentication a) { return lifecycle.declineJoinRequest(id, userId, a); }
    public InviteTokenResponse generateInviteLink(String id, Authentication a) { return lifecycle.generateInviteLink(id, a); }
    public InviteValidationResponse validateInviteToken(String token) { return lifecycle.validateInviteToken(token); }
    public static class CourseNotFoundException extends RuntimeException { private static final long serialVersionUID = 1L; }
    public static class CourseIdFormatException extends RuntimeException { private static final long serialVersionUID = 1L; }
    public static class CourseBadRequestException extends RuntimeException { private static final long serialVersionUID = 1L; public CourseBadRequestException(String message) { super(message); } }
    public static class CourseAccessException extends RuntimeException { private static final long serialVersionUID = 1L; public CourseAccessException(String message) { super(message); } }
    public static class CourseConflictException extends RuntimeException { private static final long serialVersionUID = 1L; public CourseConflictException(String message) { super(message); } }
}
