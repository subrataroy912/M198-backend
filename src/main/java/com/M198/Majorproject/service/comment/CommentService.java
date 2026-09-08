package com.M198.Majorproject.service.comment;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.CommentResponse;
import com.M198.Majorproject.dto.CreateCommentRequest;
import com.M198.Majorproject.entity.comment.Comment;
import com.M198.Majorproject.entity.comment.CommentTargetType;
import com.M198.Majorproject.entity.comment.CommentVisibility;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.entity.submission.Submission;
import com.M198.Majorproject.repository.comment.CommentRepository;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.coursework.CourseworkRepository;
import com.M198.Majorproject.repository.submission.SubmissionRepository;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final CourseworkRepository courseworkRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseMembershipRepository membershipRepository;

    public CommentService(CommentRepository commentRepository, CourseworkRepository courseworkRepository,
            SubmissionRepository submissionRepository, CourseMembershipRepository membershipRepository) {
        this.commentRepository = commentRepository;
        this.courseworkRepository = courseworkRepository;
        this.submissionRepository = submissionRepository;
        this.membershipRepository = membershipRepository;
    }

    public CommentResponse addCourseworkComment(String courseworkId, Authentication authentication, CreateCommentRequest request) {
        String userId = userId(authentication);
        String courseId = courseworkRepository.findById(courseworkId).map(value -> value.getCourseId())
                .orElseThrow(CommentNotFoundException::new);
        member(courseId, userId);
        return save(courseId, courseworkId, userId, CommentTargetType.COURSEWORK, CommentVisibility.PUBLIC, request.getBody());
    }

    public List<CommentResponse> courseworkComments(String courseworkId, Authentication authentication) {
        String userId = userId(authentication);
        String courseId = courseworkRepository.findById(courseworkId).map(value -> value.getCourseId())
                .orElseThrow(CommentNotFoundException::new);
        member(courseId, userId);
        return commentRepository.findAllByTargetTypeAndTargetIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
                CommentTargetType.COURSEWORK, courseworkId, CommentVisibility.PUBLIC).stream().map(this::response).toList();
    }

    public CommentResponse addSubmissionComment(String submissionId, Authentication authentication, CreateCommentRequest request) {
        String userId = userId(authentication);
        Submission submission = submissionRepository.findById(submissionId).orElseThrow(CommentNotFoundException::new);
        CourseMembership membership = member(submission.getCourseId(), userId);
        if (!submission.getStudentId().equals(userId) && !staff(membership)) throw new CommentAccessException();
        return save(submission.getCourseId(), submissionId, userId, CommentTargetType.SUBMISSION, CommentVisibility.PRIVATE, request.getBody());
    }

    public List<CommentResponse> submissionComments(String submissionId, Authentication authentication) {
        String userId = userId(authentication);
        Submission submission = submissionRepository.findById(submissionId).orElseThrow(CommentNotFoundException::new);
        CourseMembership membership = member(submission.getCourseId(), userId);
        if (!submission.getStudentId().equals(userId) && !staff(membership)) throw new CommentAccessException();
        return commentRepository.findAllByTargetTypeAndTargetIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
                CommentTargetType.SUBMISSION, submissionId, CommentVisibility.PRIVATE).stream().map(this::response).toList();
    }

    private CommentResponse save(String courseId, String targetId, String authorId, CommentTargetType type,
            CommentVisibility visibility, String body) {
        return response(commentRepository.save(Comment.builder().courseId(courseId).targetId(targetId).authorId(authorId)
                .targetType(type).visibility(visibility).body(body.trim()).build()));
    }
    private CourseMembership member(String courseId, String userId) { return membershipRepository
            .findByCourseIdAndUserIdAndStatus(courseId, userId, MembershipStatus.ACTIVE).orElseThrow(CommentAccessException::new); }
    private boolean staff(CourseMembership m) { return m.getRole() == MembershipRole.OWNER || m.getRole() == MembershipRole.TEACHER || m.getRole() == MembershipRole.ASSISTANT; }
    private String userId(Authentication a) { if (a == null || !a.isAuthenticated() || a.getName() == null) throw new CommentAccessException(); return a.getName(); }
    private CommentResponse response(Comment c) { CommentResponse r = new CommentResponse(); r.setId(c.getId()); r.setCourseId(c.getCourseId()); r.setAuthorId(c.getAuthorId()); r.setTargetId(c.getTargetId()); r.setVisibility(c.getVisibility()); r.setBody(c.getBody()); r.setCreatedAt(c.getCreatedAt()); r.setEditedAt(c.getEditedAt()); return r; }
    public static class CommentNotFoundException extends RuntimeException { private static final long serialVersionUID = 1L; }
    public static class CommentAccessException extends RuntimeException { private static final long serialVersionUID = 1L; }
}
