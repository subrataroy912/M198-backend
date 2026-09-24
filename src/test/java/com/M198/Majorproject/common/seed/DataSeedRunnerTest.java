package com.M198.Majorproject.common.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.Coursework;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.core.course.repository.CourseworkRepository;
import com.M198.Majorproject.core.course.repository.StudentGradebookEntryRepository;
import com.M198.Majorproject.core.course.repository.SubmissionRepository;
import com.M198.Majorproject.discovery.comment.repository.CommentRepository;
import com.M198.Majorproject.discovery.explore.repository.CourseDiscoveryRepository;
import com.M198.Majorproject.discovery.notification.repository.NotificationRepository;
import com.M198.Majorproject.discovery.notification.repository.NotificationSettingsRepository;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.identity.repository.UserRepository;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
class DataSeedRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private NotificationSettingsRepository notificationSettingsRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseDiscoveryRepository courseDiscoveryRepository;

    @Mock
    private CourseMembershipRepository courseMembershipRepository;

    @Mock
    private CourseworkRepository courseworkRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private StudentGradebookEntryRepository studentGradebookEntryRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataSeedRunner runner;

    @BeforeEach
    void setUp() {
        runner = new DataSeedRunner(
                userRepository,
                userProfileRepository,
                notificationSettingsRepository,
                courseRepository,
                courseDiscoveryRepository,
                courseMembershipRepository,
                courseworkRepository,
                submissionRepository,
                studentGradebookEntryRepository,
                commentRepository,
                notificationRepository,
                passwordEncoder);
    }

    @Test
    @DisplayName("Should cleanly seed North Bengal academic personas, spaces, and activity")
    void shouldSeedAllNorthBengalEntities() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode("Password123!")).thenReturn("mock_hashed_pw");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return User.builder()
                    .id("u-" + Math.abs(u.getEmail().hashCode()))
                    .email(u.getEmail())
                    .status(u.getStatus())
                    .canCreateCourses(u.isCanCreateCourses())
                    .isAdmin(u.isAdmin())
                    .build();
        });

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            return Course.builder()
                    .id("c-" + Math.abs(c.getTitle().hashCode()))
                    .title(c.getTitle())
                    .ownerId(c.getOwnerId())
                    .subject(c.getSubject())
                    .tags(c.getTags())
                    .theme(c.getTheme())
                    .accessType(c.getAccessType())
                    .status(c.getStatus())
                    .build();
        });

        when(courseworkRepository.save(any(Coursework.class))).thenAnswer(invocation -> {
            Coursework cw = invocation.getArgument(0);
            return Coursework.builder()
                    .id("cw-" + Math.abs(cw.getTitle().hashCode()))
                    .courseId(cw.getCourseId())
                    .creatorId(cw.getCreatorId())
                    .title(cw.getTitle())
                    .maximumPoints(cw.getMaximumPoints())
                    .dueAt(cw.getDueAt())
                    .build();
        });

        runner.run();

        // 17 users and profiles
        verify(userRepository, times(17)).save(any(User.class));
        verify(userProfileRepository, times(17)).save(any());
        verify(notificationSettingsRepository, times(17)).save(any());

        // 8 academic courses and course discovery records
        verify(courseRepository, times(8)).save(any(Course.class));
        verify(courseDiscoveryRepository, times(8)).save(any());

        // Memberships enrolled across JGEC, SIT, NBU, SGP, JPI
        verify(courseMembershipRepository, atLeastOnce()).save(any());

        // Academic activities
        verify(courseworkRepository, atLeastOnce()).save(any());
        verify(submissionRepository, atLeastOnce()).save(any());
        verify(studentGradebookEntryRepository, atLeastOnce()).save(any());
        verify(commentRepository, atLeastOnce()).save(any());
        verify(notificationRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Should clean previous seed users before re-seeding")
    void shouldCleanPreviousSeedUsers() {
        User oldSeedUser = User.builder()
                .id("old-seed-1")
                .email("old@jgec.ac.in")
                .build();

        Course oldCourse = Course.builder()
                .id("old-course-1")
                .ownerId("old-seed-1")
                .title("Old Course")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(oldSeedUser));
        when(courseRepository.findAll()).thenReturn(List.of(oldCourse));
        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc("old-seed-1", null))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("mock_hashed_pw");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return User.builder()
                    .id("u-" + Math.abs(u.getEmail().hashCode()))
                    .email(u.getEmail())
                    .build();
        });

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            return Course.builder()
                    .id("c-" + Math.abs(c.getTitle().hashCode()))
                    .title(c.getTitle())
                    .ownerId(c.getOwnerId())
                    .build();
        });

        when(courseworkRepository.save(any(Coursework.class))).thenAnswer(invocation -> {
            Coursework cw = invocation.getArgument(0);
            return Coursework.builder()
                    .id("cw-" + Math.abs(cw.getTitle().hashCode()))
                    .title(cw.getTitle())
                    .build();
        });

        runner.run();

        verify(courseRepository).deleteById("old-course-1");
        verify(userRepository).deleteById("old-seed-1");
    }
}
