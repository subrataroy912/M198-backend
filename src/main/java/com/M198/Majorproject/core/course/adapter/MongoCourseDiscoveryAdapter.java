package com.M198.Majorproject.core.course.adapter;

import java.time.Instant;
import org.springframework.stereotype.Component;
import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.port.CourseDiscoveryPort;
import com.M198.Majorproject.discovery.explore.entity.CourseDiscovery;
import com.M198.Majorproject.discovery.explore.repository.CourseDiscoveryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MongoCourseDiscoveryAdapter implements CourseDiscoveryPort {
    private final CourseDiscoveryRepository repository;

    public void sync(Course course, long enrollmentCount) {
        if (course.getId() == null) {
            return;
        }

        // Only PUBLIC and PRIVATE active courses belong in discovery; LINK_ONLY are
        // private/invite-only
        if (course.getResolvedAccessType() == com.M198.Majorproject.core.course.entity.CourseAccessType.LINK_ONLY
                || course.getStatus() != com.M198.Majorproject.core.course.entity.CourseStatus.ACTIVE) {
            repository.deleteByCourseId(course.getId());
            return;
        }

        // Searching course by id from db
        CourseDiscovery discovery = repository
                .findByCourseId(course.getId())
                .orElseGet(CourseDiscovery::new);
        // Data setting in proper variable
        discovery.setCourseId(course.getId());
        discovery.setTitle(course.getTitle());
        discovery.setSubject(course.getSubject());
        discovery.setTags(course.getTags());
        discovery.setCoverUrl(course.getCoverUrl());
        discovery.setLogoUrl(course.getLogoUrl());
        discovery.setTheme(course.getTheme());
        discovery.setAccessType(course.getResolvedAccessType());
        discovery.setVisibility(course.getVisibility());
        discovery.setStatus(course.getStatus());
        discovery.setEnrollmentCount(enrollmentCount);
        discovery.setLastActivityAt(Instant.now());
        discovery.setPopularityScore(calculatePopularityScore(enrollmentCount, course));
        // Saving data in db
        repository.save(discovery);
    }

    public void remove(String courseId) {
        repository.deleteByCourseId(courseId);
    }

    private double calculatePopularityScore(long enrollmentCount, Course course) {
        // 1. Safe fallback for legacy data missing a creation date
        if (course.getCreatedAt() == null) {
            return Math.log10(enrollmentCount + 1);
        }

        // 2. Calculate course age in hours (enforce a minimum of 1 hour to prevent
        // division by zero)
        long ageInHours = java.time.Duration.between(course.getCreatedAt(), Instant.now()).toHours();
        if (ageInHours < 1) {
            ageInHours = 1;
        }

        // 3. Set gravity factor (1.5 is standard; higher means older content is
        // punished faster)
        double gravity = 1.5;

        // 4. Calculate score: Smooth the enrollments and apply time-decay gravity
        // Multiplying by 100 makes the floating-point score easier to read in the
        // database
        double smoothedEnrollments = Math.log10(enrollmentCount + 1) * 100;

        return smoothedEnrollments / Math.pow(ageInHours, gravity);
    }
}
