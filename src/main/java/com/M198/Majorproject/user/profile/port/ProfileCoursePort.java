package com.M198.Majorproject.user.profile.port;

/**
 * Query port for profile enrichment operations that require course and enrollment counts.
 */
public interface ProfileCoursePort {

    /**
     * Returns the count of all courses owned by the user regardless of course status.
     *
     * @param userId the owner's user identifier
     * @return total courses created
     */
    long countCreatedCourses(String userId);

    /**
     * Returns the count of active courses owned by the user.
     *
     * @param userId the owner's user identifier
     * @return count of active courses created
     */
    long countActiveCreatedCourses(String userId);

    /**
     * Returns the count of active course enrollments for the user.
     *
     * @param userId the member's user identifier
     * @return count of active course memberships
     */
    long countActiveEnrolledCourses(String userId);
}
