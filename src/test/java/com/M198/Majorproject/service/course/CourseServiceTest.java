package com.M198.Majorproject.service.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.M198.Majorproject.dto.CreateCourseRequest;
import com.M198.Majorproject.dto.EnrollCourseRequest;
import com.M198.Majorproject.dto.UpdateCourseRequest;
import com.M198.Majorproject.entity.course.Course;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.EnrollmentCode;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.course.CourseRepository;
import com.M198.Majorproject.repository.course.EnrollmentCodeRepository;

class CourseServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CourseMembershipRepository membershipRepository = mock(CourseMembershipRepository.class);
    private final EnrollmentCodeRepository enrollmentCodeRepository = mock(EnrollmentCodeRepository.class);
    private final CourseService courseService = new CourseService(
            courseRepository, membershipRepository, enrollmentCodeRepository);
    private final Authentication teacher = mock(Authentication.class);
    private final Authentication student = mock(Authentication.class);

    @BeforeEach
    void setUp() {
        when(teacher.isAuthenticated()).thenReturn(true);
        when(teacher.getName()).thenReturn("teacher-1");
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_TEACHER")))
                .when(teacher).getAuthorities();
        when(student.isAuthenticated()).thenReturn(true);
        when(student.getName()).thenReturn("student-1");
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .when(student).getAuthorities();
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId("course-1");
            return course;
        });
        when(enrollmentCodeRepository.save(any(EnrollmentCode.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void authenticatedTeacherCreatesCourseAndReceivesEnrollmentCode() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Mathematics");

        var response = courseService.createCourse(teacher, request);

        assertEquals("course-1", response.getId());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getEnrollmentCode());
        assertEquals(8, response.getEnrollmentCode().length());
        verify(membershipRepository).save(any(CourseMembership.class));
        verify(enrollmentCodeRepository).save(any(EnrollmentCode.class));
    }

    @Test
    void authenticatedStudentCanCreateCourse() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Mathematics");

        var response = courseService.createCourse(student, request);

        assertEquals("course-1", response.getId());
        assertEquals("student-1", response.getOwnerId());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getEnrollmentCode());
        verify(membershipRepository).save(any(CourseMembership.class));
        verify(enrollmentCodeRepository).save(any(EnrollmentCode.class));
    }

    @Test
    void enrollmentRejectsCodeFromAnotherCourse() {
        Course course = Course.builder().id("course-1").status(CourseStatus.ACTIVE).enrollmentEnabled(true).build();
        EnrollmentCode code = EnrollmentCode.builder().courseId("course-2").code("ABCD1234").active(true).build();
        when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE)).thenReturn(Optional.of(course));
        when(enrollmentCodeRepository.findByCodeAndActiveTrue("ABCD1234")).thenReturn(Optional.of(code));

        EnrollCourseRequest request = new EnrollCourseRequest();
        request.setCode("ABCD1234");

        assertThrows(CourseService.CourseAccessException.class,
                () -> courseService.enroll("course-1", student, request));
    }

    @Test
    void memberCanReadActiveCourse() {
        Course course = Course.builder().id("course-1").title("Mathematics").status(CourseStatus.ACTIVE).build();
        when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE)).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndUserIdAndStatus(
                "course-1", "student-1", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(CourseMembership.builder().courseId("course-1").userId("student-1").build()));

        assertEquals("Mathematics", courseService.getCourse("course-1", student).getTitle());
    }

    @Test
    void courseCreationCompensatesWhenEnrollmentCodeProvisioningFails() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Mathematics");
        doThrow(new IllegalStateException("code store unavailable"))
                .when(enrollmentCodeRepository).save(any(EnrollmentCode.class));

        assertThrows(IllegalStateException.class, () -> courseService.createCourse(teacher, request));

        verify(membershipRepository).delete(any(CourseMembership.class));
        verify(courseRepository).delete(any(Course.class));
    }

    @Test
    void teacherCanUpdateCourseCoverUrl() {
        Course course = Course.builder().id("course-1").title("Mathematics")
                .status(CourseStatus.ACTIVE).build();
        when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE)).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndUserIdAndStatus(
                "course-1", "teacher-1", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(CourseMembership.builder().courseId("course-1").userId("teacher-1")
                        .role(com.M198.Majorproject.entity.course.MembershipRole.TEACHER).build()));
        UpdateCourseRequest request = new UpdateCourseRequest();
        request.setCoverUrl(" https://images.example/course.png ");

        var response = courseService.update("course-1", teacher, request);

        assertEquals("https://images.example/course.png", response.getCoverUrl());
    }
}
