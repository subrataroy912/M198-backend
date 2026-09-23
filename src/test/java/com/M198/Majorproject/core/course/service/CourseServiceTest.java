package com.M198.Majorproject.core.course.service;

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

import com.M198.Majorproject.core.course.port.CourseDiscoveryPort;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.dto.CreateCourseRequest;
import com.M198.Majorproject.core.course.dto.EnrollCourseRequest;
import com.M198.Majorproject.core.course.dto.UpdateCourseRequest;
import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.core.course.entity.EnrollmentCode;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.core.course.repository.EnrollmentCodeRepository;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import com.M198.Majorproject.core.course.security.CourseMembershipResolver;

class CourseServiceTest {

        private final CourseRepository courseRepository = mock(CourseRepository.class);
        private final CourseMembershipRepository membershipRepository = mock(CourseMembershipRepository.class);
        private final EnrollmentCodeRepository enrollmentCodeRepository = mock(EnrollmentCodeRepository.class);
        private final CourseDiscoveryPort discoveryPort = mock(CourseDiscoveryPort.class);
        private final CourseProfilePort profilePort = mock(CourseProfilePort.class);
        private final CourseDeletionCleanupService deletionCleanupService = mock(CourseDeletionCleanupService.class);
        private final CourseMediaService mediaService = mock(CourseMediaService.class);
        private final CourseAccessPolicy courseAccessPolicy = new CourseAccessPolicy(
                        new CourseMembershipResolver(membershipRepository));
        private final CourseLifecycleService courseService = new CourseLifecycleService(
                        courseRepository, membershipRepository, enrollmentCodeRepository,
                        discoveryPort, profilePort, deletionCleanupService, mediaService, courseAccessPolicy);
        private final Authentication teacher = mock(Authentication.class);
        private final Authentication student = mock(Authentication.class);

        @BeforeEach
        void setUp() {
                when(teacher.isAuthenticated()).thenReturn(true);
                when(teacher.getName()).thenReturn("teacher-1");
                doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_CREATOR")))
                                .when(teacher).getAuthorities();
                when(student.isAuthenticated()).thenReturn(true);
                when(student.getName()).thenReturn("student-1");
                doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")))
                                .when(student).getAuthorities();
                when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
                        Course course = invocation.getArgument(0);
                        course.setId("course-1");
                        return course;
                });
                when(enrollmentCodeRepository.save(any(EnrollmentCode.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(profilePort.findByUserId(any())).thenReturn(Optional.empty());
                when(profilePort.findAllByUserIdIn(any())).thenReturn(java.util.List.of());
                when(mediaService.resolveAsset(any(), any())).thenAnswer(invocation -> {
                        String val = invocation.getArgument(0);
                        return val != null && !val.trim().isEmpty() ? val.trim() : null;
                });
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

                verify(discoveryPort).sync(any(Course.class), org.mockito.ArgumentMatchers.anyLong());
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
                Course course = Course.builder().id("course-1").status(CourseStatus.ACTIVE).enrollmentEnabled(true)
                                .build();
                EnrollmentCode code = EnrollmentCode.builder().courseId("course-2").code("ABCD1234").active(true)
                                .build();
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(enrollmentCodeRepository.findByCodeAndActiveTrue("ABCD1234")).thenReturn(Optional.of(code));

                EnrollCourseRequest request = new EnrollCourseRequest();
                request.setCode("ABCD1234");

                assertThrows(CourseService.CourseAccessException.class,
                                () -> courseService.enroll("course-1", student, request));
        }

        @Test
        void memberCanReadActiveCourse() {
                Course course = Course.builder().id("course-1").title("Mathematics").status(CourseStatus.ACTIVE)
                                .build();
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
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
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
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(membershipRepository.findByCourseIdAndUserIdAndStatus(
                                "course-1", "teacher-1", MembershipStatus.ACTIVE))
                                .thenReturn(Optional
                                                .of(CourseMembership.builder().courseId("course-1").userId("teacher-1")
                                                                .role(MembershipRole.ADMIN).build()));
                UpdateCourseRequest request = new UpdateCourseRequest();
                request.setCoverUrl(" https://images.example/course.png ");

                var response = courseService.update("course-1", teacher, request);

                assertEquals("https://images.example/course.png", response.getCoverUrl());
        }

        @Test
        void teacherCanUpdateCourseLogoUrl() {
                Course course = Course.builder().id("course-1").title("Mathematics")
                                .status(CourseStatus.ACTIVE).build();
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(membershipRepository.findByCourseIdAndUserIdAndStatus(
                                "course-1", "teacher-1", MembershipStatus.ACTIVE))
                                .thenReturn(Optional
                                                .of(CourseMembership.builder().courseId("course-1").userId("teacher-1")
                                                                .role(MembershipRole.ADMIN).build()));
                UpdateCourseRequest request = new UpdateCourseRequest();
                request.setLogoUrl(" https://images.example/logo.png ");

                var response = courseService.update("course-1", teacher, request);

                assertEquals("https://images.example/logo.png", response.getLogoUrl());
                assertEquals("https://images.example/logo.png", response.getLogo());
        }

        @Test
        void teacherCanClearCourseCoverAndLogoUrlWithBlankStrings() {
                Course course = Course.builder().id("course-1").title("Mathematics")
                                .coverUrl("https://images.example/course.png")
                                .logoUrl("https://images.example/logo.png")
                                .status(CourseStatus.ACTIVE).build();
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(membershipRepository.findByCourseIdAndUserIdAndStatus(
                                "course-1", "teacher-1", MembershipStatus.ACTIVE))
                                .thenReturn(Optional
                                                .of(CourseMembership.builder().courseId("course-1").userId("teacher-1")
                                                                .role(MembershipRole.ADMIN).build()));
                UpdateCourseRequest request = new UpdateCourseRequest();
                request.setCoverUrl("   ");
                request.setLogoUrl("");

                var response = courseService.update("course-1", teacher, request);

                org.junit.jupiter.api.Assertions.assertNull(response.getCoverUrl());
                org.junit.jupiter.api.Assertions.assertNull(response.getLogoUrl());
        }

        @Test
        void enrollByCodeFindsCourseAndEnrollsMember() {
                Course course = Course.builder().id("course-1").status(CourseStatus.ACTIVE).enrollmentEnabled(true)
                                .build();
                EnrollmentCode code = EnrollmentCode.builder().courseId("course-1").code("JOIN1234").active(true)
                                .build();
                when(enrollmentCodeRepository.findByCodeAndActiveTrue("JOIN1234")).thenReturn(Optional.of(code));
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(membershipRepository.findByCourseIdAndUserId("course-1", "student-1"))
                                .thenReturn(Optional.empty());

                var response = courseService.enrollByCode(student, "JOIN1234");

                org.junit.jupiter.api.Assertions.assertNotNull(response);
                assertEquals("course-1", response.getId());
                verify(membershipRepository).save(any(CourseMembership.class));
        }

        @Test
        void getCoursePopulatesEnrollmentCode() {
                Course course = Course.builder().id("course-1").title("Biology").status(CourseStatus.ACTIVE).build();
                EnrollmentCode code = EnrollmentCode.builder().courseId("course-1").code("BIO12345").active(true)
                                .build();
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
                when(courseRepository.findByIdAndStatus("course-invite", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));

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
                when(courseRepository.findByIdAndStatus("course-open", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(membershipRepository.findByCourseIdAndUserId("course-open", "student-1"))
                                .thenReturn(Optional.empty());

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
                when(membershipRepository.findByCourseIdAndUserId("6aa4583ab463bdd7707f1556", "student-1"))
                                .thenReturn(Optional.empty());

                var response = courseService.getCourse("6aa4583ab463bdd7707f1556", student);

                org.junit.jupiter.api.Assertions.assertNotNull(response);
                assertEquals("6aa4583ab463bdd7707f1556", response.getId());
                assertEquals("VIEWER", response.getRole());
                org.junit.jupiter.api.Assertions.assertFalse(response.isEnrolled());
                org.junit.jupiter.api.Assertions.assertNull(response.getEnrollmentCode());
        }

        @Test
        void publicCourseWithCodeAccessTypeEnrollmentSucceedsWithoutCode() {
                Course course = Course.builder()
                                .id("6aa437aecbe563688e22b18d")
                                .status(CourseStatus.ACTIVE)
                                .visibility(CourseVisibility.PUBLIC)
                                .accessType(CourseAccessType.CODE)
                                .enrollmentEnabled(true)
                                .build();
                when(courseRepository.findByIdAndStatus("6aa437aecbe563688e22b18d", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(membershipRepository.findByCourseIdAndUserId("6aa437aecbe563688e22b18d", "student-1"))
                                .thenReturn(Optional.empty());

                var response = courseService.enroll("6aa437aecbe563688e22b18d", student, new EnrollCourseRequest());

                org.junit.jupiter.api.Assertions.assertNotNull(response);
                assertEquals("6aa437aecbe563688e22b18d", response.getId());
                verify(membershipRepository).save(any(CourseMembership.class));
        }

        @Test
        void activeMemberEnrollmentIsIdempotent() {
                Course course = Course.builder()
                                .id("6aa451d330db1c61fdfc3ab5")
                                .status(CourseStatus.ACTIVE)
                                .visibility(CourseVisibility.PUBLIC)
                                .accessType(CourseAccessType.OPEN)
                                .enrollmentEnabled(true)
                                .build();
                CourseMembership activeMembership = CourseMembership.builder()
                                .courseId("6aa451d330db1c61fdfc3ab5")
                                .userId("student-1")
                                .role(MembershipRole.MEMBER)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(courseRepository.findByIdAndStatus("6aa451d330db1c61fdfc3ab5", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));
                when(membershipRepository.findByCourseIdAndUserId("6aa451d330db1c61fdfc3ab5", "student-1"))
                                .thenReturn(Optional.of(activeMembership));

                var response = courseService.enroll("6aa451d330db1c61fdfc3ab5", student, new EnrollCourseRequest());

                org.junit.jupiter.api.Assertions.assertNotNull(response);
                assertEquals("6aa451d330db1c61fdfc3ab5", response.getId());
                org.junit.jupiter.api.Assertions.assertTrue(response.isEnrolled());
                assertEquals("MEMBER", response.getRole());
        }

        @Test
        void removeMemberSuccessfully() {
                Course course = Course.builder().id("course-1").status(CourseStatus.ACTIVE).build();
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));

                CourseMembership teacherMembership = CourseMembership.builder()
                                .courseId("course-1")
                                .userId("teacher-1")
                                .role(MembershipRole.ADMIN)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "teacher-1",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(teacherMembership));

                CourseMembership studentMembership = CourseMembership.builder()
                                .courseId("course-1")
                                .userId("student-2")
                                .role(MembershipRole.MEMBER)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "student-2",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(studentMembership));

                courseService.removeMember("course-1", "student-2", teacher);

                assertEquals(MembershipStatus.REMOVED, studentMembership.getStatus());
                org.junit.jupiter.api.Assertions.assertNotNull(studentMembership.getRemovedAt());
                verify(membershipRepository).save(studentMembership);
        }

        @Test
        void removeMemberFailsWhenTargetIsOwner() {
                Course course = Course.builder().id("course-1").status(CourseStatus.ACTIVE).build();
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));

                CourseMembership teacherMembership = CourseMembership.builder()
                                .courseId("course-1")
                                .userId("teacher-1")
                                .role(MembershipRole.ADMIN)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "teacher-1",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(teacherMembership));

                CourseMembership ownerMembership = CourseMembership.builder()
                                .courseId("course-1")
                                .userId("owner-1")
                                .role(MembershipRole.OWNER)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "owner-1",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(ownerMembership));

                assertThrows(CourseService.CourseConflictException.class,
                                () -> courseService.removeMember("course-1", "owner-1", teacher));
        }

        @Test
        void removeMemberFailsWhenCallerNotTeacherOrOwner() {
                Course course = Course.builder().id("course-1").status(CourseStatus.ACTIVE).build();
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));

                CourseMembership studentMembership = CourseMembership.builder()
                                .courseId("course-1")
                                .userId("student-1")
                                .role(MembershipRole.MEMBER)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "student-1",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(studentMembership));

                assertThrows(CourseService.CourseAccessException.class,
                                () -> courseService.removeMember("course-1", "student-2", student));
        }

        @Test
        void deleteCourse_success_cascadesAllDependents() {
                Course course = Course.builder()
                                .id("course-1")
                                .ownerId("owner-1")
                                .status(CourseStatus.ACTIVE)
                                .build();
                when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

                Authentication ownerAuth = mock(Authentication.class);
                when(ownerAuth.isAuthenticated()).thenReturn(true);
                when(ownerAuth.getName()).thenReturn("owner-1");
                doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")))
                                .when(ownerAuth).getAuthorities();

                courseService.deleteCourse("course-1", ownerAuth);

                verify(deletionCleanupService).clean("course-1");
                verify(courseRepository).deleteById("course-1");
        }

        @Test
        void deleteCourse_rejectsNonOwner() {
                Course course = Course.builder()
                                .id("course-1")
                                .ownerId("owner-1")
                                .status(CourseStatus.ACTIVE)
                                .build();
                when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

                CourseMembership teacherMembership = CourseMembership.builder()
                                .courseId("course-1")
                                .userId("teacher-2")
                                .role(MembershipRole.ADMIN)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "teacher-2",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(teacherMembership));

                Authentication teacher2 = mock(Authentication.class);
                when(teacher2.isAuthenticated()).thenReturn(true);
                when(teacher2.getName()).thenReturn("teacher-2");
                doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")))
                                .when(teacher2).getAuthorities();

                assertThrows(CourseService.CourseAccessException.class,
                                () -> courseService.deleteCourse("course-1", teacher2));
        }

        @Test
        void deleteCourse_allowsAdmin() {
                Course course = Course.builder()
                                .id("course-1")
                                .ownerId("owner-1")
                                .status(CourseStatus.ARCHIVED)
                                .build();
                when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));

                Authentication adminAuth = mock(Authentication.class);
                when(adminAuth.isAuthenticated()).thenReturn(true);
                when(adminAuth.getName()).thenReturn("admin-1");
                doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                                .when(adminAuth).getAuthorities();

                courseService.deleteCourse("course-1", adminAuth);

                verify(courseRepository).deleteById("course-1");
        }

        @Test
        void createAndModifyCourseWithSpaceLinks() {
                com.M198.Majorproject.core.course.entity.SpaceLink link1 = com.M198.Majorproject.core.course.entity.SpaceLink
                                .builder()
                                .id("link-1")
                                .title("Project Repo")
                                .url("https://github.com/example/repo")
                                .category("REPOSITORY")
                                .build();

                CreateCourseRequest request = new CreateCourseRequest();
                request.setTitle("Robotics Club");
                request.setSpaceType(com.M198.Majorproject.core.course.entity.SpaceType.CLUB_SOCIETY);
                request.setLinks(java.util.List.of(link1));

                var response = courseService.createCourse(teacher, request);
                org.junit.jupiter.api.Assertions.assertNotNull(response.getLinks());
                assertEquals(1, response.getLinks().size());
                assertEquals("Project Repo", response.getLinks().get(0).getTitle());
                assertEquals("https://github.com/example/repo", response.getLinks().get(0).getUrl());

                Course course = Course.builder()
                                .id("course-1")
                                .ownerId("teacher-1")
                                .title("Robotics Club")
                                .status(CourseStatus.ACTIVE)
                                .links(java.util.List.of(link1))
                                .build();
                when(courseRepository.findByIdAndStatus("course-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(course));

                CourseMembership ownerMembership = CourseMembership.builder()
                                .courseId("course-1")
                                .userId("teacher-1")
                                .role(MembershipRole.OWNER)
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("course-1", "teacher-1",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(ownerMembership));

                com.M198.Majorproject.core.course.entity.SpaceLink link2 = com.M198.Majorproject.core.course.entity.SpaceLink
                                .builder()
                                .id("link-2")
                                .title("Discord Server")
                                .url("https://discord.gg/example")
                                .category("COMMUNICATION")
                                .build();

                UpdateCourseRequest updateRequest = new UpdateCourseRequest();
                updateRequest.setLinks(java.util.List.of(link1, link2));

                var updated = courseService.update("course-1", teacher, updateRequest);
                org.junit.jupiter.api.Assertions.assertNotNull(updated.getLinks());
                assertEquals(2, updated.getLinks().size());
        }
}
