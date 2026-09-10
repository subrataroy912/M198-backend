package com.M198.Majorproject.service.analytics;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.CourseAnalyticsResponse;
import com.M198.Majorproject.dto.GradebookEntryResponse;
import com.M198.Majorproject.entity.analytics.CourseAnalyticsSummary;
import com.M198.Majorproject.entity.analytics.StudentGradebookEntry;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.repository.analytics.CourseAnalyticsSummaryRepository;
import com.M198.Majorproject.repository.analytics.StudentGradebookEntryRepository;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CourseAnalyticsSummaryRepository summaryRepository;
    private final StudentGradebookEntryRepository gradebookRepository;
    private final CourseMembershipRepository membershipRepository;

    public CourseAnalyticsResponse summary(String courseId, Authentication a) {
        if (!hasRole(a, "ROLE_ADMIN")) {
            requireStaff(courseId, userId(a));

        }
        CourseAnalyticsSummary summary = summaryRepository.findByCourseId(courseId).orElseThrow(AnalyticsNotFoundException::new);
        return summaryResponse(summary);
    }

    public List<GradebookEntryResponse> gradebook(String courseId, String studentId, Authentication a) {
        String caller = userId(a);
        if (!caller.equals(studentId)) {
            requireStaff(courseId, caller);

        }
        membershipRepository.findByCourseIdAndUserIdAndStatus(courseId, studentId, MembershipStatus.ACTIVE).orElseThrow(AnalyticsNotFoundException::new);
        return gradebookRepository.findAllByCourseIdAndStudentIdOrderByDueAtAsc(courseId, studentId).stream().map(this::gradebookResponse).toList();
    }

    private void requireStaff(String courseId, String userId) {
        var membership = membershipRepository.findByCourseIdAndUserIdAndStatus(courseId, userId, MembershipStatus.ACTIVE).orElseThrow(AnalyticsAccessException::new);
        if (membership.getRole() != MembershipRole.OWNER && membership.getRole() != MembershipRole.TEACHER && membership.getRole() != MembershipRole.ASSISTANT) {
            throw new AnalyticsAccessException();

        }
    }

    private String userId(Authentication a) {
        if (a == null || !a.isAuthenticated() || a.getName() == null) {
            throw new AnalyticsAccessException();

        }
        return a.getName();
    }

    private boolean hasRole(Authentication a, String role) {
        return a != null && a.getAuthorities().stream().anyMatch(value -> role.equals(value.getAuthority()));
    }

    private CourseAnalyticsResponse summaryResponse(CourseAnalyticsSummary value) {
        CourseAnalyticsResponse response = new CourseAnalyticsResponse();
        response.setCourseId(value.getCourseId());
        response.setStudentCount(value.getStudentCount());
        response.setCourseworkCount(value.getCourseworkCount());
        response.setSubmissionCount(value.getSubmissionCount());
        response.setTurnedInCount(value.getTurnedInCount());
        response.setMissingCount(value.getMissingCount());
        response.setGradedCount(value.getGradedCount());
        response.setAverageScore(value.getAverageScore());
        response.setOnTimeSubmissionRate(value.getOnTimeSubmissionRate());
        response.setMissingSubmissionRate(value.getMissingSubmissionRate());
        response.setGeneratedAt(value.getGeneratedAt());
        return response;
    }

    private GradebookEntryResponse gradebookResponse(StudentGradebookEntry value) {
        GradebookEntryResponse response = new GradebookEntryResponse();
        response.setCourseworkId(value.getCourseworkId());
        response.setTitle(value.getTitle());
        response.setMaximumPoints(value.getMaximumPoints());
        response.setScore(value.getScore());
        response.setStatus(value.getStatus());
        response.setFeedback(value.getFeedback());
        response.setDueAt(value.getDueAt());
        response.setGradedAt(value.getGradedAt());
        return response;
    }

    public static class AnalyticsNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class AnalyticsAccessException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
