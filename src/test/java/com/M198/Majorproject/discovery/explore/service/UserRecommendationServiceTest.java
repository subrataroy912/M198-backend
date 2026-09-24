package com.M198.Majorproject.discovery.explore.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.discovery.explore.dto.RecommendedUserResponse;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;
import com.M198.Majorproject.user.profile.security.AuthenticatedUserResolver;

class UserRecommendationServiceTest {

        private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
        private final CourseMembershipRepository courseMembershipRepository = mock(CourseMembershipRepository.class);
        private final CourseRepository courseRepository = mock(CourseRepository.class);
        private final AuthenticatedUserResolver authenticatedUserResolver = mock(AuthenticatedUserResolver.class);

        private final UserRecommendationService service = new UserRecommendationService(
                        userProfileRepository,
                        courseMembershipRepository,
                        courseRepository,
                        authenticatedUserResolver);

        @Test
        void unauthenticatedReturnsPublicCreatorsWithFeaturedReason() {
                Authentication auth = mock(Authentication.class);
                when(authenticatedUserResolver.resolveAuthenticatedUserIdSafe(auth)).thenReturn(null);

                UserProfile creator = UserProfile.builder()
                                .userId("creator-1")
                                .displayName("Dr. Alan Turing")
                                .headline("Computer Science")
                                .canCreateCourses(true)
                                .profileVisibility(ProfileVisibility.PUBLIC)
                                .build();

                when(userProfileRepository.findAllByCanCreateCoursesTrueAndProfileVisibilityAndDeletedAtIsNull(
                                eq(ProfileVisibility.PUBLIC), any()))
                                .thenReturn(List.of(creator));

                Page<RecommendedUserResponse> page = service.getRecommendedUsers(auth, 0, 10);

                assertEquals(1, page.getTotalElements());
                RecommendedUserResponse first = page.getContent().get(0);
                assertEquals("creator-1", first.getId());
                assertEquals("Dr. Alan Turing", first.getName());
                assertEquals(UserRecommendationService.REASON_FEATURED_CREATOR, first.getRecommendationReason());
                assertEquals(0, first.getSharedCoursesCount());
        }

        @Test
        void authenticatedUserRecommendsPeersWithSharedSpacesAndDepartment() {
                Authentication auth = mock(Authentication.class);
                when(authenticatedUserResolver.resolveAuthenticatedUserIdSafe(auth)).thenReturn("user-current");

                UserProfile currentProfile = UserProfile.builder()
                                .userId("user-current")
                                .displayName("Me")
                                .headline("Software Engineering")
                                .profileVisibility(ProfileVisibility.PUBLIC)
                                .build();
                when(userProfileRepository.findByUserIdAndDeletedAtIsNull("user-current"))
                                .thenReturn(Optional.of(currentProfile));

                // Enrolled courses
                CourseMembership myMembership = CourseMembership.builder()
                                .userId("user-current")
                                .courseId("course-algorithms")
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(courseMembershipRepository.findAllByUserIdAndStatus("user-current", MembershipStatus.ACTIVE))
                                .thenReturn(List.of(myMembership));

                Course course = Course.builder()
                                .id("course-algorithms")
                                .title("Algorithms & Data Structures")
                                .status(CourseStatus.ACTIVE)
                                .build();
                when(courseRepository.findAllByIdInAndStatus(List.of("course-algorithms"), CourseStatus.ACTIVE))
                                .thenReturn(List.of(course));

                // Peer in the same course
                CourseMembership peerMembership = CourseMembership.builder()
                                .userId("peer-1")
                                .courseId("course-algorithms")
                                .status(MembershipStatus.ACTIVE)
                                .build();
                when(courseMembershipRepository.findAllByCourseIdInAndStatus(List.of("course-algorithms"),
                                MembershipStatus.ACTIVE))
                                .thenReturn(List.of(myMembership, peerMembership));

                UserProfile peerProfile = UserProfile.builder()
                                .userId("peer-1")
                                .displayName("Ada Lovelace")
                                .headline("Software Engineering")
                                .profileVisibility(ProfileVisibility.PUBLIC)
                                .build();
                when(userProfileRepository.findAllByUserIdInAndDeletedAtIsNull(any()))
                                .thenReturn(List.of(peerProfile));

                when(userProfileRepository.findAllByCanCreateCoursesTrueAndProfileVisibilityAndDeletedAtIsNull(any(),
                                any()))
                                .thenReturn(List.of());
                when(userProfileRepository.findAllByProfileVisibilityAndDeletedAtIsNull(any(), any()))
                                .thenReturn(new PageImpl<>(List.of()));

                Page<RecommendedUserResponse> page = service.getRecommendedUsers(auth, 0, 10);

                assertEquals(1, page.getTotalElements());
                RecommendedUserResponse rec = page.getContent().get(0);
                assertEquals("peer-1", rec.getId());
                assertEquals("Ada Lovelace", rec.getName());
                assertEquals(1, rec.getSharedCoursesCount());
                assertEquals(List.of("Algorithms & Data Structures"), rec.getSharedCourseTitles());
                assertTrue(rec.isSameDepartment());
                assertEquals(UserRecommendationService.REASON_SHARED_SPACES, rec.getRecommendationReason());
        }

        @Test
        void ignoresDeletedAndPrivateProfiles() {
                Authentication auth = mock(Authentication.class);
                when(authenticatedUserResolver.resolveAuthenticatedUserIdSafe(auth)).thenReturn("user-current");

                when(userProfileRepository.findByUserIdAndDeletedAtIsNull("user-current"))
                                .thenReturn(Optional.empty());

                CourseMembership myMembership = CourseMembership.builder()
                                .userId("user-current")
                                .courseId("course-1")
                                .status(MembershipStatus.ACTIVE)
                                .build();
                CourseMembership peerMembership = CourseMembership.builder()
                                .userId("private-user")
                                .courseId("course-1")
                                .status(MembershipStatus.ACTIVE)
                                .build();

                when(courseMembershipRepository.findAllByUserIdAndStatus("user-current", MembershipStatus.ACTIVE))
                                .thenReturn(List.of(myMembership));
                when(courseMembershipRepository.findAllByCourseIdInAndStatus(List.of("course-1"), MembershipStatus.ACTIVE))
                                .thenReturn(List.of(myMembership, peerMembership));

                // Private profile
                UserProfile privateProfile = UserProfile.builder()
                                .userId("private-user")
                                .displayName("Hidden User")
                                .profileVisibility(ProfileVisibility.PRIVATE)
                                .build();

                when(userProfileRepository.findAllByUserIdInAndDeletedAtIsNull(any()))
                                .thenReturn(List.of(privateProfile));

                when(userProfileRepository.findAllByCanCreateCoursesTrueAndProfileVisibilityAndDeletedAtIsNull(any(),
                                any()))
                                .thenReturn(List.of());
                when(userProfileRepository.findAllByProfileVisibilityAndDeletedAtIsNull(any(), any()))
                                .thenReturn(new PageImpl<>(List.of()));

                Page<RecommendedUserResponse> page = service.getRecommendedUsers(auth, 0, 10);

                assertTrue(page.getContent().isEmpty());
        }
}
