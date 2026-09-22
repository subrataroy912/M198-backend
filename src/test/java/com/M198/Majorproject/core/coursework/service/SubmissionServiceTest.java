package com.M198.Majorproject.core.coursework.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.M198.Majorproject.core.course.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.M198.Majorproject.core.course.dto.GradeSubmissionRequest;
import com.M198.Majorproject.core.course.dto.UpdateSubmissionRequest;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.entity.Coursework;
import com.M198.Majorproject.core.course.entity.CourseworkStatus;
import com.M198.Majorproject.core.course.entity.CourseworkType;
import com.M198.Majorproject.core.course.entity.Submission;
import com.M198.Majorproject.core.course.entity.SubmissionStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseworkRepository;
import com.M198.Majorproject.core.course.repository.SubmissionRepository;

class SubmissionServiceTest {

    private final CourseworkRepository courseworkRepository = mock(CourseworkRepository.class);
    private final CourseMembershipRepository membershipRepository = mock(CourseMembershipRepository.class);
    private final SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
    private final SubmissionService service = new SubmissionService(
            courseworkRepository, membershipRepository, submissionRepository);
    private final Authentication student = mock(Authentication.class);
    private final Authentication teacher = mock(Authentication.class);

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        when(student.isAuthenticated()).thenReturn(true);
        when(student.getName()).thenReturn("student-1");
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(student).getAuthorities();
        when(teacher.isAuthenticated()).thenReturn(true);
        when(teacher.getName()).thenReturn("teacher-1");
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(teacher).getAuthorities();
        when(membershipRepository.findByCourseIdAndUserIdAndStatus(
                "course-1", "student-1", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(CourseMembership.builder()
                        .courseId("course-1").userId("student-1")
                        .role(MembershipRole.STUDENT).status(MembershipStatus.ACTIVE).build()));
        when(membershipRepository.findByCourseIdAndUserIdAndStatus(
                "course-1", "teacher-1", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(CourseMembership.builder()
                        .courseId("course-1").userId("teacher-1")
                        .role(MembershipRole.TEACHER).status(MembershipStatus.ACTIVE).build()));
    }

    @Test
    void studentCannotStartDraftAssignment() {
        when(courseworkRepository.findByIdAndStatus("work-1", CourseworkStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThrows(SubmissionService.SubmissionNotFoundException.class,
                () -> service.start("work-1", student));
    }

    @Test
    void studentCannotStartArchivedAssignment() {
        when(courseworkRepository.findByIdAndStatus("work-1", CourseworkStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThrows(SubmissionService.SubmissionNotFoundException.class,
                () -> service.start("work-1", student));
    }

    @Test
    void studentStartsPublishedAssignmentAsDraft() {
        when(courseworkRepository.findByIdAndStatus("work-1", CourseworkStatus.PUBLISHED))
                .thenReturn(Optional.of(coursework(CourseworkStatus.PUBLISHED)));
        when(submissionRepository.findByCourseworkIdAndStudentId("work-1", "student-1"))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId("submission-1");
            return submission;
        });

        var response = service.start("work-1", student);

        assertEquals(SubmissionStatus.DRAFT, response.getStatus());
        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    void teacherCannotGradeDraftSubmission() {
        when(courseworkRepository.findById("work-1"))
                .thenReturn(Optional.of(coursework(CourseworkStatus.PUBLISHED)));
        when(submissionRepository.findById("submission-1"))
                .thenReturn(Optional.of(Submission.builder()
                        .id("submission-1").courseworkId("work-1")
                        .courseId("course-1").studentId("student-1")
                        .status(SubmissionStatus.DRAFT).build()));
        GradeSubmissionRequest request = grade(8);

        assertThrows(SubmissionService.SubmissionConflictException.class,
                () -> service.grade("work-1", "submission-1", teacher, request));
    }

    @Test
    void teacherGradesTurnedInSubmission() {
        when(courseworkRepository.findById("work-1"))
                .thenReturn(Optional.of(coursework(CourseworkStatus.PUBLISHED)));
        Submission submission = Submission.builder()
                .id("submission-1").courseworkId("work-1").courseId("course-1")
                .studentId("student-1").status(SubmissionStatus.TURNED_IN).build();
        when(submissionRepository.findById("submission-1")).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);
        var response = service.grade("work-1", "submission-1", teacher, grade(8));

        assertEquals(SubmissionStatus.GRADED, response.getStatus());
        assertEquals(BigDecimal.valueOf(8), response.getScore());
    }

    @Test
    void gradedSubmissionCannotBeEdited() {
        when(courseworkRepository.findByIdAndStatus("work-1", CourseworkStatus.PUBLISHED))
                .thenReturn(Optional.of(coursework(CourseworkStatus.PUBLISHED)));
        when(submissionRepository.findByCourseworkIdAndStudentId("work-1", "student-1"))
                .thenReturn(Optional.of(Submission.builder().status(SubmissionStatus.GRADED).build()));

        assertThrows(SubmissionService.SubmissionConflictException.class,
                () -> service.update("work-1", student, new UpdateSubmissionRequest()));
    }

    private Coursework coursework(CourseworkStatus status) {
        return Coursework.builder().id("work-1").courseId("course-1")
                .type(CourseworkType.ASSIGNMENT).status(status).maximumPoints(10).build();
    }

    private GradeSubmissionRequest grade(int score) {
        GradeSubmissionRequest request = new GradeSubmissionRequest();
        request.setScore(BigDecimal.valueOf(score));
        return request;
    }
}
