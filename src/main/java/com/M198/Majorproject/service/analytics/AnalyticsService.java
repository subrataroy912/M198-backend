package com.M198.Majorproject.service.analytics;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.CourseAnalyticsResponse;
import com.M198.Majorproject.dto.GradebookEntryResponse;
import com.M198.Majorproject.dto.TeacherGradebookResponse;
import com.M198.Majorproject.entity.analytics.CourseAnalyticsSummary;
import com.M198.Majorproject.entity.analytics.StudentGradebookEntry;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.entity.identity.UserProfile;
import com.M198.Majorproject.entity.submission.SubmissionStatus;
import com.M198.Majorproject.repository.analytics.CourseAnalyticsSummaryRepository;
import com.M198.Majorproject.repository.analytics.StudentGradebookEntryRepository;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.identity.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CourseAnalyticsSummaryRepository summaryRepository;
    private final StudentGradebookEntryRepository gradebookRepository;
    private final CourseMembershipRepository membershipRepository;
    private final UserProfileRepository userProfileRepository;

    public List<TeacherGradebookResponse> teacherGradebook(String courseId, Authentication a) {
        if (!hasRole(a, "ROLE_ADMIN")) {
            requireStaff(courseId, userId(a));
        }
        List<CourseMembership> students = membershipRepository.findAllByCourseIdAndStatus(courseId, MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> m.getRole() == MembershipRole.STUDENT)
                .toList();

        List<String> studentIds = students.stream().map(CourseMembership::getUserId).toList();
        Map<String, UserProfile> profiles = new HashMap<>();
        if (userProfileRepository != null && !studentIds.isEmpty()) {
            userProfileRepository.findAllByUserIdIn(studentIds).forEach(p -> profiles.put(p.getUserId(), p));
        }

        Map<String, List<StudentGradebookEntry>> entriesByStudent = gradebookRepository
                .findAllByCourseIdOrderByDueAtAsc(courseId).stream()
                .collect(Collectors.groupingBy(StudentGradebookEntry::getStudentId));

        return students.stream().map(m -> {
            String sId = m.getUserId();
            TeacherGradebookResponse res = new TeacherGradebookResponse();
            res.setId(sId);
            res.setStudentId(sId);
            UserProfile p = profiles.get(sId);
            if (p != null) {
                res.setStudentName(p.getDisplayName());
                res.setAvatar(p.getAvatarUrl());
                res.setAvatarUrl(p.getAvatarUrl());
            } else {
                res.setStudentName("Student (" + (sId.length() > 4 ? sId.substring(sId.length() - 4) : sId) + ")");
            }

            List<StudentGradebookEntry> entries = entriesByStudent.getOrDefault(sId, List.of());
            long missing = entries.stream().filter(e -> e.getStatus() == SubmissionStatus.MISSING).count();
            res.setMissingCount(missing);
            res.setSubmittedCount(entries.stream().filter(e -> e.getStatus() == SubmissionStatus.TURNED_IN || e.getStatus() == SubmissionStatus.GRADED).count());

            List<StudentGradebookEntry> graded = entries.stream().filter(e -> e.getScore() != null && e.getMaximumPoints() != null && e.getMaximumPoints() > 0).toList();
            if (!graded.isEmpty()) {
                double totalRatio = graded.stream().mapToDouble(e -> e.getScore().doubleValue() / e.getMaximumPoints()).average().orElse(0.0);
                res.setAverage(Math.round(totalRatio * 100) + "%");
                res.setAverageScore(BigDecimal.valueOf(Math.round(totalRatio * 100)));
            } else {
                res.setAverage("—");
            }
            return res;
        }).toList();
    }

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
