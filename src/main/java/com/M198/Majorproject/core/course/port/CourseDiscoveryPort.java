package com.M198.Majorproject.core.course.port;

import com.M198.Majorproject.core.course.entity.Course;

/**
 * Boundary for maintaining the discovery read model from course lifecycle
 * changes.
 */
public interface CourseDiscoveryPort {
    void sync(Course course, long enrollmentCount);

    void remove(String courseId);
}
