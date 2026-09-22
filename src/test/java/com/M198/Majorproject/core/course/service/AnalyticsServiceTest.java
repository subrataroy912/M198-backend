package com.M198.Majorproject.core.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.repository.CourseAnalyticsSummaryRepository;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.StudentGradebookEntryRepository;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import com.M198.Majorproject.core.course.security.CourseMembershipResolver;
import com.M198.Majorproject.user.profile.entity.UserProfile;

class AnalyticsServiceTest {

    private final CourseAnalyticsSummaryRepository summaryRepository = mock(CourseAnalyticsSummaryRepository.class);
    private final StudentGradebookEntryRepository gradebookRepository = mock(StudentGradebookEntryRepository.class);
    private final CourseMembershipRepository membershipRepository = mock(CourseMembershipRepository.class);
    private final CourseProfilePort courseProfilePort = mock(CourseProfilePort.class);
    private final CourseAccessPolicy courseAccessPolicy =
            new CourseAccessPolicy(new CourseMembershipResolver(membershipRepository));

    private final AnalyticsService analyticsService = new AnalyticsService(
            summaryRepository,
            gradebookRepository,
            membershipRepository,
            courseProfilePort,
            courseAccessPolicy);

    private final Authentication teacher = mock(Authentication.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(teacher.isAuthenticated()).thenReturn(true);
        when(teacher.getName()).thenReturn("teacher-1");
        org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_CREATOR")))
                .when(teacher).getAuthorities();
    }

    @Test
    void teacherGradebookUsesCourseProfilePortToResolveStudentDetails() {
        CourseMembership staffMembership = CourseMembership.builder()
                .courseId("course-1")
                .userId("teacher-1")
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();
        CourseMembership memberMembership = CourseMembership.builder()
                .courseId("course-1")
                .userId("student-1")
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();

        when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "teacher-1", MembershipStatus.ACTIVE))
                .thenReturn(java.util.Optional.of(staffMembership));
        when(membershipRepository.findAllByCourseIdAndStatus("course-1", MembershipStatus.ACTIVE))
                .thenReturn(List.of(staffMembership, memberMembership));
        when(gradebookRepository.findAllByCourseIdOrderByDueAtAsc("course-1"))
                .thenReturn(List.of());

        UserProfile studentProfile = UserProfile.builder()
                .userId("student-1")
                .displayName("Student One")
                .avatarUrl("https://images.example/avatar.png")
                .build();
        when(courseProfilePort.findAllByUserIdIn(List.of("student-1")))
                .thenReturn(List.of(studentProfile));

        var gradebook = analyticsService.teacherGradebook("course-1", teacher);

        assertNotNull(gradebook);
        assertEquals(1, gradebook.size());
        assertEquals("Student One", gradebook.get(0).getMemberName());
        assertEquals("https://images.example/avatar.png", gradebook.get(0).getAvatarUrl());
    }
}
