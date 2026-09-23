package com.M198.Majorproject.core.course.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.M198.Majorproject.common.exception.AnalyticsAccessException;
import com.M198.Majorproject.common.exception.AnalyticsNotFoundException;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.core.course.dto.CourseAnalyticsResponse;
import com.M198.Majorproject.core.course.dto.CourseGradebookResponse;
import com.M198.Majorproject.core.course.dto.GradebookEntryResponse;
import com.M198.Majorproject.core.course.entity.CourseAnalyticsSummary;
import com.M198.Majorproject.core.course.entity.StudentGradebookEntry;
import com.M198.Majorproject.core.course.repository.CourseAnalyticsSummaryRepository;
import com.M198.Majorproject.core.course.repository.StudentGradebookEntryRepository;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.core.course.entity.SubmissionStatus;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CourseAnalyticsSummaryRepository summaryRepository;
    private final StudentGradebookEntryRepository gradebookRepository;
    private final CourseMembershipRepository membershipRepository;
    private final CourseProfilePort courseProfilePort;
    private final CourseAccessPolicy courseAccessPolicy;


    public List<CourseGradebookResponse> courseGradebook(String courseId, Authentication a) {
        if (isNotAdmin(a)) {
            courseAccessPolicy.requireStaff(courseId, courseAccessPolicy.authenticatedUserId(a, AnalyticsAccessException::new), AnalyticsAccessException::new, AnalyticsAccessException::new);
        }
        List<CourseMembership> members = membershipRepository.findAllByCourseIdAndStatus(courseId, MembershipStatus.ACTIVE)
                .stream()
                .filter(m -> m.getRole() == MembershipRole.MEMBER)
                .toList();

        List<String> memberIds = members.stream().map(CourseMembership::getUserId).toList();
        Map<String, UserProfile> profiles = new HashMap<>();
        if (courseProfilePort != null && !memberIds.isEmpty()) {
            courseProfilePort.findAllByUserIdIn(memberIds).forEach(p -> profiles.put(p.getUserId(), p));
        }

        Map<String, List<StudentGradebookEntry>> entriesByMember = gradebookRepository
                .findAllByCourseIdOrderByDueAtAsc(courseId).stream()
                .collect(Collectors.groupingBy(StudentGradebookEntry::getStudentId));

        return members.stream().map(m -> {
            String sId = m.getUserId();
            CourseGradebookResponse res = new CourseGradebookResponse();
            res.setId(sId);
            res.setMemberId(sId);
            UserProfile p = profiles.get(sId);
            if (p != null) {
                res.setMemberName(p.getDisplayName());
                res.setAvatar(p.getAvatarUrl());
                res.setAvatarUrl(p.getAvatarUrl());
            } else {
                res.setMemberName("Member (" + (sId.length() > 4 ? sId.substring(sId.length() - 4) : sId) + ")");
            }

            List<StudentGradebookEntry> entries = entriesByMember.getOrDefault(sId, List.of());
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
        if (isNotAdmin(a)) {
            courseAccessPolicy.requireStaff(courseId, courseAccessPolicy.authenticatedUserId(a, AnalyticsAccessException::new), AnalyticsAccessException::new, AnalyticsAccessException::new);

        }
        CourseAnalyticsSummary summary = summaryRepository.
                findByCourseId(courseId).
                orElseThrow(AnalyticsNotFoundException::new);
        return summaryResponse(summary);
    }

    public List<GradebookEntryResponse> gradebook(String courseId, String studentId, Authentication a) {
        String caller = courseAccessPolicy.authenticatedUserId(a, AnalyticsAccessException::new);
        if (!caller.equals(studentId)) {
            courseAccessPolicy.requireStaff(courseId, caller, AnalyticsAccessException::new, AnalyticsAccessException::new);

        }
        membershipRepository.findByCourseIdAndUserIdAndStatus(courseId, studentId, MembershipStatus.ACTIVE).orElseThrow(AnalyticsNotFoundException::new);
        return gradebookRepository.findAllByCourseIdAndStudentIdOrderByDueAtAsc(courseId, studentId).stream().map(this::gradebookResponse).toList();
    }

    private boolean isNotAdmin(Authentication a) {
        return a == null || a.getAuthorities().stream()
                .noneMatch(value -> "ROLE_ADMIN".equals(value.getAuthority()));
    }

    private @NonNull CourseAnalyticsResponse summaryResponse(@NonNull CourseAnalyticsSummary value)
    {
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

    private @NonNull GradebookEntryResponse gradebookResponse(@NonNull StudentGradebookEntry value)
    {
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

}
