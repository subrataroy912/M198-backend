package com.M198.Majorproject.core.course.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.M198.Majorproject.core.course.entity.Coursework;
import com.M198.Majorproject.core.course.repository.*;
import com.M198.Majorproject.discovery.comment.repository.CommentRepository;
import com.M198.Majorproject.discovery.notification.entity.NotificationResourceType;
import com.M198.Majorproject.discovery.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.core.course.port.CourseDiscoveryPort;

/** Handles cross-module cleanup after course authorization has succeeded. */
@Service
@RequiredArgsConstructor
public class CourseDeletionCleanupService {
    private final CourseworkRepository coursework;
    private final SubmissionRepository submissions;
    private final CommentRepository comments;
    private final AttachmentRepository attachments;
    private final NotificationRepository notifications;
    private final StudentGradebookEntryRepository gradebook;
    private final CourseAnalyticsSummaryRepository analytics;
    private final EnrollmentCodeRepository enrollmentCodes;
    private final CourseMembershipRepository memberships;
    private final CourseDiscoveryPort discovery;

    public void clean(String courseId) {
        List<String> courseworkIds = coursework.findAllByCourseId(courseId).stream().map(Coursework::getId).toList();
        notifications.deleteAllByResourceTypeAndResourceId(NotificationResourceType.COURSE, courseId);
        if (!courseworkIds.isEmpty())
            notifications.deleteAllByResourceTypeAndResourceIdIn(NotificationResourceType.COURSEWORK, courseworkIds);
        comments.deleteAllByCourseId(courseId);
        attachments.deleteAllByCourseId(courseId);
        submissions.deleteAllByCourseId(courseId);
        coursework.deleteAllByCourseId(courseId);
        gradebook.deleteAllByCourseId(courseId);
        analytics.deleteByCourseId(courseId);
        enrollmentCodes.deleteAllByCourseId(courseId);
        memberships.deleteAllByCourseId(courseId);
        discovery.remove(courseId);
    }
}
