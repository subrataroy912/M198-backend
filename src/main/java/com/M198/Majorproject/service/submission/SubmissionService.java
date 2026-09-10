package com.M198.Majorproject.service.submission;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.dto.GradeSubmissionRequest;
import com.M198.Majorproject.dto.SubmissionResponse;
import com.M198.Majorproject.dto.UpdateSubmissionRequest;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.entity.coursework.Coursework;
import com.M198.Majorproject.entity.coursework.CourseworkType;
import com.M198.Majorproject.entity.submission.Submission;
import com.M198.Majorproject.entity.submission.SubmissionStatus;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.coursework.CourseworkRepository;
import com.M198.Majorproject.repository.submission.SubmissionRepository;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final CourseworkRepository courseworkRepository;
    private final CourseMembershipRepository membershipRepository;
    private final SubmissionRepository submissionRepository;

    public SubmissionResponse start(String courseworkId, Authentication authentication) {
        String studentId = authenticatedUserId(authentication);
        Coursework coursework = publishedAssignment(courseworkId);
        requireStudentMember(coursework.getCourseId(), studentId);
        if (coursework.getType() != CourseworkType.ASSIGNMENT) {
            throw new SubmissionConflictException("Only assignments accept submissions");
        }
        return submissionRepository.findByCourseworkIdAndStudentId(courseworkId, studentId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    try {
                        return toResponse(submissionRepository.save(Submission.builder()
                                .courseworkId(courseworkId)
                                .courseId(coursework.getCourseId())
                                .studentId(studentId)
                                .status(SubmissionStatus.DRAFT)
                                .build()));
                    } catch (DuplicateKeyException exception) {
                        throw new SubmissionConflictException("Submission already exists");
                    }
                });
    }

    public SubmissionResponse mine(String courseworkId, Authentication authentication) {
        String studentId = authenticatedUserId(authentication);
        Coursework coursework = publishedAssignment(courseworkId);
        requireStudentMember(coursework.getCourseId(), studentId);
        return submissionRepository.findByCourseworkIdAndStudentId(courseworkId, studentId)
                .map(this::toResponse)
                .orElseThrow(SubmissionNotFoundException::new);
    }

    public SubmissionResponse update(
            String courseworkId, Authentication authentication, UpdateSubmissionRequest request) {
        String studentId = authenticatedUserId(authentication);
        Coursework coursework = publishedAssignment(courseworkId);
        requireStudentMember(coursework.getCourseId(), studentId);
        Submission submission = submissionRepository.findByCourseworkIdAndStudentId(courseworkId, studentId)
                .orElseThrow(SubmissionNotFoundException::new);
        if (submission.getStatus() == SubmissionStatus.GRADED
                || submission.getStatus() == SubmissionStatus.RETURNED) {
            throw new SubmissionConflictException("Graded submissions cannot be edited");
        }
        if (request.getAnswerText() != null) {
            submission.setAnswerText(request.getAnswerText().trim());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != SubmissionStatus.DRAFT
                    && request.getStatus() != SubmissionStatus.TURNED_IN) {
                throw new SubmissionConflictException("Invalid student submission status");
            }
            submission.setStatus(request.getStatus());
            if (request.getStatus() == SubmissionStatus.TURNED_IN) {
                submission.setSubmittedAt(Instant.now());
                submission.setLate(coursework.getDueAt() != null && Instant.now().isAfter(coursework.getDueAt()));
            }
        }
        return toResponse(submissionRepository.save(submission));
    }

    public Page<SubmissionResponse> list(String courseworkId, Authentication authentication, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        Coursework coursework = coursework(courseworkId);
        requireStaff(coursework.getCourseId(), authenticatedUserId(authentication));
        return submissionRepository.findAllByCourseworkIdOrderByCreatedAtAsc(
                courseworkId, PageRequest.of(page, size)).map(this::toResponse);
    }

    public SubmissionResponse grade(
            String courseworkId, String submissionId, Authentication authentication, GradeSubmissionRequest request) {
        Coursework coursework = coursework(courseworkId);
        requireStaff(coursework.getCourseId(), authenticatedUserId(authentication));
        Submission submission = submissionRepository.findById(submissionId)
                .filter(value -> courseworkId.equals(value.getCourseworkId()))
                .orElseThrow(SubmissionNotFoundException::new);
        if (submission.getStatus() != SubmissionStatus.TURNED_IN
                && submission.getStatus() != SubmissionStatus.RETURNED) {
            throw new SubmissionConflictException("Only turned-in submissions can be graded");
        }
        if (coursework.getMaximumPoints() != null
                && request.getScore().compareTo(java.math.BigDecimal.valueOf(coursework.getMaximumPoints())) > 0) {
            throw new SubmissionConflictException("Score exceeds maximum points");
        }
        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback() == null ? null : request.getFeedback().trim());
        submission.setGraderId(authenticatedUserId(authentication));
        submission.setGradedAt(Instant.now());
        submission.setReturnedAt(Instant.now());
        submission.setStatus(SubmissionStatus.GRADED);
        Submission saved = submissionRepository.save(submission);
        return toResponse(saved);
    }

    private Coursework coursework(String courseworkId) {
        return courseworkRepository.findById(courseworkId).orElseThrow(SubmissionNotFoundException::new);
    }

    private Coursework publishedAssignment(String courseworkId) {
        Coursework coursework = courseworkRepository.findByIdAndStatus(
                courseworkId, com.M198.Majorproject.entity.coursework.CourseworkStatus.PUBLISHED)
                .orElseThrow(SubmissionNotFoundException::new);
        if (coursework.getType() != CourseworkType.ASSIGNMENT) {
            throw new SubmissionConflictException("Only published assignments accept submissions");
        }
        return coursework;
    }

    private void requireStudentMember(String courseId, String userId) {
        CourseMembership membership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, MembershipStatus.ACTIVE).orElseThrow(SubmissionAccessException::new);
        if (membership.getRole() != MembershipRole.STUDENT) {
            throw new SubmissionAccessException();
        }
    }

    private void requireStaff(String courseId, String userId) {
        CourseMembership membership = membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, MembershipStatus.ACTIVE).orElseThrow(SubmissionAccessException::new);
        if (membership.getRole() != MembershipRole.OWNER
                && membership.getRole() != MembershipRole.TEACHER
                && membership.getRole() != MembershipRole.ASSISTANT) {
            throw new SubmissionAccessException();
        }
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new SubmissionAccessException();
        }
        return authentication.getName();
    }

    private SubmissionResponse toResponse(Submission submission) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(submission.getId());
        response.setCourseworkId(submission.getCourseworkId());
        response.setCourseId(submission.getCourseId());
        response.setStudentId(submission.getStudentId());
        response.setStatus(submission.getStatus());
        response.setAnswerText(submission.getAnswerText());
        response.setSubmittedAt(submission.getSubmittedAt());
        response.setReturnedAt(submission.getReturnedAt());
        response.setLate(submission.isLate());
        response.setScore(submission.getScore());
        response.setGraderId(submission.getGraderId());
        response.setFeedback(submission.getFeedback());
        response.setGradedAt(submission.getGradedAt());
        return response;
    }

    public static class SubmissionNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class SubmissionAccessException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class SubmissionConflictException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public SubmissionConflictException(String message) {
            super(message);
        }
    }
}
