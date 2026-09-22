package com.M198.Majorproject.core.course.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;

class CourseAccessPolicyTest {

    private static final String COURSE_ID = "course-1";
    private static final String USER_ID = "user-1";

    @Test
    void inactiveMembershipIsDeniedByEveryAuthorizationOperation() {
        CourseMembershipRepository repository = mock(CourseMembershipRepository.class);
        CourseAccessPolicy policy = new CourseAccessPolicy(new CourseMembershipResolver(repository));
        when(repository.findByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> policy.requireActiveMember(COURSE_ID, USER_ID, AccessDeniedException::new));
        assertThrows(AccessDeniedException.class,
                () -> policy.requireTeacherOrOwner(COURSE_ID, USER_ID,
                        AccessDeniedException::new, AccessDeniedException::new));
        assertThrows(AccessDeniedException.class,
                () -> policy.requireStudent(COURSE_ID, USER_ID,
                        AccessDeniedException::new, AccessDeniedException::new));
        assertThrows(AccessDeniedException.class,
                () -> policy.requireStaff(COURSE_ID, USER_ID,
                        AccessDeniedException::new, AccessDeniedException::new));
    }

    @Test
    void membershipRolesReceiveConsistentAccessDecisions() {
        for (MembershipRole role : MembershipRole.values()) {
            CourseMembershipRepository repository = mock(CourseMembershipRepository.class);
            CourseAccessPolicy policy = new CourseAccessPolicy(new CourseMembershipResolver(repository));
            when(repository.findByCourseIdAndUserIdAndStatus(COURSE_ID, USER_ID, MembershipStatus.ACTIVE))
                    .thenReturn(Optional.of(CourseMembership.builder()
                            .courseId(COURSE_ID).userId(USER_ID).role(role)
                            .status(MembershipStatus.ACTIVE).build()));

            assertDoesNotThrow(() -> policy.requireActiveMember(
                    COURSE_ID, USER_ID, AccessDeniedException::new));
            assertRoleAccess(role == MembershipRole.OWNER || role == MembershipRole.TEACHER,
                    () -> policy.requireTeacherOrOwner(COURSE_ID, USER_ID,
                            AccessDeniedException::new, AccessDeniedException::new));
            assertRoleAccess(role == MembershipRole.STUDENT,
                    () -> policy.requireStudent(COURSE_ID, USER_ID,
                            AccessDeniedException::new, AccessDeniedException::new));
            assertRoleAccess(role != MembershipRole.STUDENT,
                    () -> policy.requireStaff(COURSE_ID, USER_ID,
                            AccessDeniedException::new, AccessDeniedException::new));
        }
    }

    private void assertRoleAccess(boolean allowed, Runnable operation) {
        if (allowed) {
            assertDoesNotThrow(operation::run);
        } else {
            assertThrows(AccessDeniedException.class, operation::run);
        }
    }

    private static class AccessDeniedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
