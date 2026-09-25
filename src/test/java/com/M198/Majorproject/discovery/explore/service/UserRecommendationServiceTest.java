package com.M198.Majorproject.discovery.explore.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
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
            authenticatedUserResolver
    );

    @Test
    void unauthenticatedReturnsEmptyList() {
        Authentication auth = mock(Authentication.class);
        when(authenticatedUserResolver.resolveAuthenticatedUserIdSafe(auth)).thenReturn(null);

        Page<RecommendedUserResponse> page = service.getRecommendedUsers(auth, 0, 10);

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void userWithZeroJoinedSpacesReturnsEmptyList() {
        Authentication auth = mock(Authentication.class);
        when(authenticatedUserResolver.resolveAuthenticatedUserIdSafe(auth)).thenReturn("rahul-biswas");

        when(courseMembershipRepository.findAllByUserIdAndStatus("rahul-biswas", MembershipStatus.ACTIVE))
                .thenReturn(List.of());

        Page<RecommendedUserResponse> page = service.getRecommendedUsers(auth, 0, 10);

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void recommendsDirectClassmatesAndSecondDegreeMutualSpacePeers() {
        Authentication auth = mock(Authentication.class);
        when(authenticatedUserResolver.resolveAuthenticatedUserIdSafe(auth)).thenReturn("user-current");

        // Current user is enrolled in course-1
        CourseMembership myMembership = CourseMembership.builder()
                .userId("user-current")
                .courseId("course-1")
                .status(MembershipStatus.ACTIVE)
                .build();
        when(courseMembershipRepository.findAllByUserIdAndStatus("user-current", MembershipStatus.ACTIVE))
                .thenReturn(List.of(myMembership));

        Course course1 = Course.builder()
                .id("course-1")
                .title("Electronics 101")
                .status(CourseStatus.ACTIVE)
                .build();
        when(courseRepository.findAllByIdInAndStatus(List.of("course-1"), CourseStatus.ACTIVE))
                .thenReturn(List.of(course1));

        // 1st-degree peer (Alice) is in course-1
        CourseMembership aliceMembership = CourseMembership.builder()
                .userId("alice-1")
                .courseId("course-1")
                .status(MembershipStatus.ACTIVE)
                .build();
        // Alice is also enrolled in course-2 (outside current user's courses)
        CourseMembership aliceInCourse2 = CourseMembership.builder()
                .userId("alice-1")
                .courseId("course-2")
                .status(MembershipStatus.ACTIVE)
                .build();

        // 2nd-degree peer (Bob) is in course-2 with Alice
        CourseMembership bobInCourse2 = CourseMembership.builder()
                .userId("bob-2")
                .courseId("course-2")
                .status(MembershipStatus.ACTIVE)
                .build();

        when(courseMembershipRepository.findAllByCourseIdInAndStatus(any(), any()))
                .thenAnswer(invocation -> {
                    java.util.Collection<?> courseIds = invocation.getArgument(0);
                    if (courseIds != null && courseIds.contains("course-1")) {
                        return List.of(myMembership, aliceMembership);
                    }
                    if (courseIds != null && courseIds.contains("course-2")) {
                        return List.of(aliceInCourse2, bobInCourse2);
                    }
                    return List.of();
                });

        when(courseMembershipRepository.findAllByUserIdInAndStatus(any(), any()))
                .thenReturn(List.of(aliceMembership, aliceInCourse2));

        UserProfile aliceProfile = UserProfile.builder()
                .userId("alice-1")
                .displayName("Alice")
                .headline("ETCE")
                .profileVisibility(ProfileVisibility.PUBLIC)
                .build();
        UserProfile bobProfile = UserProfile.builder()
                .userId("bob-2")
                .displayName("Bob")
                .headline("Physics")
                .profileVisibility(ProfileVisibility.PUBLIC)
                .build();

        when(userProfileRepository.findAllByUserIdInAndDeletedAtIsNull(any()))
                .thenReturn(List.of(aliceProfile, bobProfile));

        Page<RecommendedUserResponse> page = service.getRecommendedUsers(auth, 0, 10);

        assertEquals(2, page.getTotalElements());

        // Alice (1st-degree direct classmate) should rank first
        RecommendedUserResponse first = page.getContent().get(0);
        assertEquals("alice-1", first.getId());
        assertEquals(UserRecommendationService.REASON_SHARED_SPACES, first.getRecommendationReason());
        assertEquals(1, first.getSharedCoursesCount());
        assertEquals(List.of("Electronics 101"), first.getSharedCourseTitles());

        // Bob (2nd-degree mutual space peer connected via Alice) should rank second
        RecommendedUserResponse second = page.getContent().get(1);
        assertEquals("bob-2", second.getId());
        assertEquals(UserRecommendationService.REASON_MUTUAL_SPACE_PEERS, second.getRecommendationReason());
        assertEquals(1, second.getMutualPeersCount());
        assertEquals(0, second.getSharedCoursesCount());
    }

    @Test
    void ignoresDeletedAndPrivateProfiles() {
        Authentication auth = mock(Authentication.class);
        when(authenticatedUserResolver.resolveAuthenticatedUserIdSafe(auth)).thenReturn("user-current");

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
        when(courseMembershipRepository.findAllByUserIdInAndStatus(any(), any()))
                .thenReturn(List.of());

        // Private profile
        UserProfile privateProfile = UserProfile.builder()
                .userId("private-user")
                .displayName("Hidden User")
                .profileVisibility(ProfileVisibility.PRIVATE)
                .build();

        when(userProfileRepository.findAllByUserIdInAndDeletedAtIsNull(any()))
                .thenReturn(List.of(privateProfile));

        Page<RecommendedUserResponse> page = service.getRecommendedUsers(auth, 0, 10);

        assertTrue(page.getContent().isEmpty());
    }
}
