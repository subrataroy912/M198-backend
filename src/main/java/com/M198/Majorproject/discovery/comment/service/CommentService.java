package com.M198.Majorproject.discovery.comment.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.discovery.comment.dto.CommentResponse;
import com.M198.Majorproject.discovery.comment.dto.CreateCommentRequest;
import com.M198.Majorproject.discovery.comment.entity.Comment;
import com.M198.Majorproject.discovery.comment.entity.CommentTargetType;
import com.M198.Majorproject.discovery.comment.entity.CommentVisibility;
import com.M198.Majorproject.discovery.comment.repository.CommentRepository;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.entity.Submission;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseworkRepository;
import com.M198.Majorproject.core.course.repository.SubmissionRepository;
import com.M198.Majorproject.user.profile.entity.UserProfile;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CourseworkRepository courseworkRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseMembershipRepository membershipRepository;
    private final CourseProfilePort profilePort;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.M198.Majorproject.discovery.notification.service.NotificationService notificationService;

    public CommentResponse addCourseworkComment(String courseworkId, Authentication authentication, CreateCommentRequest request) {
        String userId = userId(authentication);
        var coursework = courseworkRepository.findById(courseworkId)
                .orElseThrow(CommentNotFoundException::new);
        String courseId = coursework.getCourseId();
        member(courseId, userId);
        CommentResponse saved = save(courseId, courseworkId, userId, CommentTargetType.COURSEWORK, CommentVisibility.PUBLIC, request.getBody());
        if (notificationService != null
                && coursework.getCreatorId() != null
                && !coursework.getCreatorId().equals(userId)) {
            String authorName = saved.getAuthorName() != null ? saved.getAuthorName() : "A member";
            notificationService.sendNotification(
                    coursework.getCreatorId(),
                    com.M198.Majorproject.discovery.notification.entity.NotificationType.COMMENT_ADDED,
                    authorName + " commented on \"" + (coursework.getTitle() != null ? coursework.getTitle() : "your post") + "\"",
                    saved.getBody(),
                    com.M198.Majorproject.discovery.notification.entity.NotificationResourceType.COURSEWORK,
                    courseworkId,
                    courseId);
        }
        return saved;
    }

    public List<CommentResponse> courseworkComments(String courseworkId, Authentication authentication) {
        String userId = userId(authentication);
        String courseId = courseworkRepository.findById(courseworkId).map(value -> value.getCourseId())
                .orElseThrow(CommentNotFoundException::new);
        member(courseId, userId);
        List<Comment> comments = commentRepository.findAllByTargetTypeAndTargetIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
                CommentTargetType.COURSEWORK, courseworkId, CommentVisibility.PUBLIC);

        Map<String, UserProfile> profileMap = fetchProfiles(comments);
        return comments.stream().map(c -> response(c, profileMap.get(c.getAuthorId()))).toList();
    }

    public CommentResponse addSubmissionComment(String submissionId, Authentication authentication, CreateCommentRequest request) {
        String userId = userId(authentication);
        Submission submission = submissionRepository.findById(submissionId).orElseThrow(CommentNotFoundException::new);
        CourseMembership membership = member(submission.getCourseId(), userId);
        if (!submission.getStudentId().equals(userId) && !staff(membership)) {
            throw new CommentAccessException();
        }
        CommentResponse saved = save(submission.getCourseId(), submissionId, userId, CommentTargetType.SUBMISSION, CommentVisibility.PRIVATE, request.getBody());
        if (notificationService != null) {
            String recipientId = !submission.getStudentId().equals(userId)
                    ? submission.getStudentId()
                    : submission.getGraderId();
            if (recipientId == null && submission.getCourseworkId() != null) {
                recipientId = courseworkRepository.findById(submission.getCourseworkId())
                        .map(cw -> cw.getCreatorId())
                        .orElse(null);
            }
            if (recipientId != null && !recipientId.equals(userId)) {
                String authorName = saved.getAuthorName() != null ? saved.getAuthorName() : "Someone";
                notificationService.sendNotification(
                        recipientId,
                        com.M198.Majorproject.discovery.notification.entity.NotificationType.COMMENT_ADDED,
                        authorName + " added a private comment on your submission",
                        saved.getBody(),
                        com.M198.Majorproject.discovery.notification.entity.NotificationResourceType.SUBMISSION,
                        submissionId,
                        submission.getCourseId());
            }
        }
        return saved;
    }

    public List<CommentResponse> submissionComments(String submissionId, Authentication authentication) {
        String userId = userId(authentication);
        Submission submission = submissionRepository.findById(submissionId).orElseThrow(CommentNotFoundException::new);
        CourseMembership membership = member(submission.getCourseId(), userId);
        if (!submission.getStudentId().equals(userId) && !staff(membership)) {
            throw new CommentAccessException();
        }
        List<Comment> comments = commentRepository.findAllByTargetTypeAndTargetIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
                CommentTargetType.SUBMISSION, submissionId, CommentVisibility.PRIVATE);

        Map<String, UserProfile> profileMap = fetchProfiles(comments);
        return comments.stream().map(c -> response(c, profileMap.get(c.getAuthorId()))).toList();
    }

    private Map<String, UserProfile> fetchProfiles(List<Comment> comments) {
        if (comments == null || comments.isEmpty() || profilePort == null) {
            return Collections.emptyMap();
        }
        Set<String> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return profilePort.findAllByUserIdIn(authorIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p, (a, b) -> a));
    }

    private CommentResponse save(String courseId, String targetId, String authorId, CommentTargetType type,
            CommentVisibility visibility, String body) {
        Comment saved = commentRepository.save(Comment.builder().courseId(courseId).targetId(targetId).authorId(authorId)
                .targetType(type).visibility(visibility).body(body.trim()).build());
        UserProfile profile = authorId != null && profilePort != null
                ? profilePort.findByUserId(authorId).orElse(null)
                : null;
        return response(saved, profile);
    }

    private CourseMembership member(String courseId, String userId) {
        return membershipRepository
                .findByCourseIdAndUserIdAndStatus(courseId, userId, MembershipStatus.ACTIVE).orElseThrow(CommentAccessException::new);
    }

    private boolean staff(CourseMembership m) {
        return m.getRole() == MembershipRole.OWNER || m.getRole() == MembershipRole.ADMIN;
    }

    private String userId(Authentication a) {
        if (a == null || !a.isAuthenticated() || a.getName() == null) {
            throw new CommentAccessException();

        }
        return a.getName();
    }

    public CommentResponse response(Comment c) {
        return response(c, null);
    }

    public CommentResponse response(Comment c, UserProfile profile) {
        CommentResponse r = new CommentResponse();
        r.setId(c.getId());
        r.setCourseId(c.getCourseId());
        r.setAuthorId(c.getAuthorId());
        if (profile != null) {
            String name = profile.getDisplayName();
            if (name == null || name.isBlank()) {
                String first = profile.getFirstName() != null ? profile.getFirstName().trim() : "";
                String last = profile.getLastName() != null ? profile.getLastName().trim() : "";
                String full = (first + " " + last).trim();
                name = !full.isEmpty() ? full : (profile.getHandle() != null ? profile.getHandle() : "Member");
            }
            r.setAuthorName(name);
            r.setAuthorAvatarUrl(profile.getAvatarUrl());
            r.setAuthorHandle(profile.getHandle());
        }
        r.setTargetId(c.getTargetId());
        r.setVisibility(c.getVisibility());
        r.setBody(c.getBody());
        r.setCreatedAt(c.getCreatedAt());
        r.setEditedAt(c.getEditedAt());
        return r;
    }

    public static class CommentNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class CommentAccessException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
