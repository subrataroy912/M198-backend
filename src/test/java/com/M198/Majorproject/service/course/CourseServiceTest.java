package com.M198.Majorproject.service.course;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.M198.Majorproject.dto.CreateCourseRequest;
import com.M198.Majorproject.dto.EnrollCourseRequest;
import com.M198.Majorproject.dto.UpdateCourseRequest;
import com.M198.Majorproject.entity.course.Course;
import com.M198.Majorproject.entity.course.CourseAccessType;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;
import com.M198.Majorproject.entity.course.EnrollmentCode;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.entity.explore.CourseDiscovery;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.course.CourseRepository;
import com.M198.Majorproject.repository.course.EnrollmentCodeRepository;
import com.M198.Majorproject.repository.explore.CourseDiscoveryRepository;
import com.cloudinary.Cloudinary;

class CourseServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CourseMembershipRepository membershipRepository = mock(CourseMembershipRepository.class);
    private final EnrollmentCodeRepository enrollmentCodeRepository = mock(EnrollmentCodeRepository.class);
    private final CourseDiscoveryRepository courseDiscoveryRepository = mock(CourseDiscoveryRepository.class);
    private final CourseService courseService = new CourseService(
            courseRepository, membershipRepository, enrollmentCodeRepository,
            courseDiscoveryRepository, (Cloudinary) null, "", "", "");
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
        when(enrollmentCodeRepository.save(any(EnrollmentCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
    void publicCourseCreationSynchronizesPublicActiveDiscoveryRecord() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Mathematics");
        request.setSubject("Science");
        request.setVisibility(CourseVisibility.PUBLIC);

        courseService.createCourse(teacher, request);

        var discovery = org.mockito.ArgumentCaptor.forClass(CourseDiscovery.class);
        verify(courseDiscoveryRepository).save(discovery.capture());
        assertEquals("course-1", discovery.getValue().getCourseId());
        assertEquals("Mathematics", discovery.getValue().getTitle());
        assertEquals("Science", discovery.getValue().getSubject());
        assertEquals(CourseVisibility.PUBLIC, discovery.getValue().getVisibility());
        assertEquals(CourseStatus.ACTIVE, discovery.getValue().getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(discovery.getValue().getLastActivityAt());
    }

    @Test
    void studentCannotCreateCourse() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Mathematics");

        assertThrows(CourseService.CourseAccessException.class,
                () -> courseService.createCourse(student, request));
    }

    @Test
    void studentWithCreatorRoleCanCreateCourse() {
        Authentication creatorStudent = mock(Authentication.class);
        when(creatorStudent.isAuthenticated()).thenReturn(true);
        when(creatorStudent.getName()).thenReturn("student-creator-1");
        doReturn(java.util.List.of(
                new SimpleGrantedAuthority("ROLE_STUDENT"),
                new SimpleGrantedAuthority("ROLE_CREATOR")))
                .when(creatorStudent).getAuthorities();

        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Physics Study Group");

        var response = courseService.createCourse(creatorStudent, request);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals("Physics Study Group", response.getTitle());
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
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndUserId("course-1", "student-1"))
                .thenReturn(Optional.of(CourseMembership.builder()
                        .courseId("course-1").userId("student-1")
                        .status(MembershipStatus.ACTIVE).build()));

        assertEquals("Mathematics", courseService.getCourse("course-1", student).getTitle());
    }

    @Test
    void publicActiveCourseCanBeReadWithoutMembership() {
        Course course = Course.builder().id("course-1").title("Mathematics")
                .visibility(CourseVisibility.PUBLIC).status(CourseStatus.ACTIVE).build();
        when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE)).thenReturn(Optional.of(course));
        when(membershipRepository.countByCourseIdAndStatus("course-1", MembershipStatus.ACTIVE)).thenReturn(3L);

        var response = courseService.getPublicCourse("course-1");

        assertEquals("Mathematics", response.getTitle());
        assertEquals(3L, response.getMemberCount());
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

    @Test
    void enrollByCodeFindsCourseAndEnrollsMember() {
        Course course = Course.builder().id("course-1").status(CourseStatus.ACTIVE).enrollmentEnabled(true).build();
        EnrollmentCode code = EnrollmentCode.builder().courseId("course-1").code("JOIN1234").active(true).build();
        when(enrollmentCodeRepository.findByCodeAndActiveTrue("JOIN1234")).thenReturn(Optional.of(code));
        when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE)).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndUserId("course-1", "student-1")).thenReturn(Optional.empty());

        var response = courseService.enrollByCode(student, "JOIN1234");

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        assertEquals("course-1", response.getId());
        verify(membershipRepository).save(any(CourseMembership.class));
    }

    @Test
    void getCoursePopulatesEnrollmentCode() {
        Course course = Course.builder().id("course-1").title("Biology").status(CourseStatus.ACTIVE).build();
        EnrollmentCode code = EnrollmentCode.builder().courseId("course-1").code("BIO12345").active(true).build();
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndUserId("course-1", "student-1"))
                .thenReturn(Optional.of(CourseMembership.builder()
                        .courseId("course-1").userId("student-1")
                        .status(MembershipStatus.ACTIVE).build()));
        when(enrollmentCodeRepository.findByCourseIdAndActiveTrue("course-1")).thenReturn(Optional.of(code));

        var response = courseService.getCourse("course-1", student);

        assertEquals("BIO12345", response.getEnrollmentCode());
    }

    @Test
    void inviteCourseCreationDoesNotGenerateEnrollmentCode() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle("Private Seminar");
        request.setAccessType(CourseAccessType.INVITE);

        var response = courseService.createCourse(teacher, request);

        assertEquals("course-1", response.getId());
        assertEquals(CourseAccessType.INVITE, response.getAccessType());
        org.junit.jupiter.api.Assertions.assertFalse(response.isEnrollmentEnabled());
        org.junit.jupiter.api.Assertions.assertNull(response.getEnrollmentCode());
        verify(membershipRepository).save(any(CourseMembership.class));
    }

    @Test
    void enrollmentInInviteCourseThrowsAccessException() {
        Course course = Course.builder()
                .id("course-invite")
                .status(CourseStatus.ACTIVE)
                .accessType(CourseAccessType.INVITE)
                .enrollmentEnabled(true)
                .build();
        when(courseRepository.findByIdAndStatus("course-invite", CourseStatus.ACTIVE)).thenReturn(Optional.of(course));

        EnrollCourseRequest request = new EnrollCourseRequest("ANYCODE");
        var ex = assertThrows(CourseService.CourseAccessException.class,
                () -> courseService.enroll("course-invite", student, request));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("invite-only"));
    }

    @Test
    void openCourseEnrollmentSucceedsWithoutCode() {
        Course course = Course.builder()
                .id("course-open")
                .status(CourseStatus.ACTIVE)
                .accessType(CourseAccessType.OPEN)
                .enrollmentEnabled(true)
                .build();
        when(courseRepository.findByIdAndStatus("course-open", CourseStatus.ACTIVE)).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndUserId("course-open", "student-1")).thenReturn(Optional.empty());

        var response = courseService.enroll("course-open", student, new EnrollCourseRequest());

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        assertEquals("course-open", response.getId());
        verify(membershipRepository).save(any(CourseMembership.class));
    }

    @Test
    void getCourseAllowsUnenrolledUserToViewPublicCourse() {
        Course course = Course.builder()
                .id("6aa4583ab463bdd7707f1556")
                .status(CourseStatus.ACTIVE)
                .visibility(CourseVisibility.PUBLIC)
                .accessType(CourseAccessType.OPEN)
                .title("Public Open Class")
                .build();
        when(courseRepository.findById("6aa4583ab463bdd7707f1556")).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndUserId("6aa4583ab463bdd7707f1556", "student-1")).thenReturn(Optional.empty());

        var response = courseService.getCourse("6aa4583ab463bdd7707f1556", student);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        assertEquals("6aa4583ab463bdd7707f1556", response.getId());
        assertEquals("VIEWER", response.getRole());
        org.junit.jupiter.api.Assertions.assertFalse(response.isEnrolled());
        org.junit.jupiter.api.Assertions.assertNull(response.getEnrollmentCode());
    }
}
