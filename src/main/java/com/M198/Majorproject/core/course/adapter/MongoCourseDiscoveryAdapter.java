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

        // Only PUBLIC and PRIVATE active courses belong in discovery; LINK_ONLY are private/invite-only
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
        discovery.setPopularityScore(calculatePopularityScore(enrollmentCount));
        // Saving data in db
        repository.save(discovery);
    }

    public void remove(String courseId) {
        repository.deleteByCourseId(courseId);
    }

    // TODO: Have to do advance calculation here for better feedback
    private double calculatePopularityScore(long enrollmentCount) {
        return enrollmentCount;
    }
}
