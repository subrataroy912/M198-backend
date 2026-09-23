package com.M198.Majorproject.core.course.adapter;

import java.time.Instant;
import org.springframework.stereotype.Component;
import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.core.course.port.CourseDiscoveryPort;
import com.M198.Majorproject.discovery.explore.entity.CourseDiscovery;
import com.M198.Majorproject.discovery.explore.repository.CourseDiscoveryRepository;

@Component
class MongoCourseDiscoveryAdapter implements CourseDiscoveryPort {
    private final CourseDiscoveryRepository repository;
    MongoCourseDiscoveryAdapter(CourseDiscoveryRepository repository) { this.repository = repository; }

    private CourseVisibility resolveVisibility(CourseAccessType accessType) {
        return (accessType == CourseAccessType.PUBLIC) ? CourseVisibility.PUBLIC : CourseVisibility.PRIVATE;
    }

    public void sync(Course course, long enrollmentCount) {
        CourseDiscovery discovery = repository.findByCourseId(course.getId()).orElseGet(CourseDiscovery::new);
        discovery.setCourseId(course.getId());
        discovery.setTitle(course.getTitle());
        discovery.setSubject(course.getSubject());
        discovery.setTags(course.getTags());
        discovery.setCoverUrl(course.getCoverUrl());
        discovery.setLogoUrl(course.getLogoUrl());
        discovery.setTheme(course.getTheme());
        CourseAccessType accessType = course.getAccessType() != null ? course.getAccessType() : CourseAccessType.PUBLIC;
        discovery.setAccessType(accessType);
        discovery.setVisibility(resolveVisibility(accessType));
        discovery.setStatus(course.getStatus());
        discovery.setEnrollmentCount(enrollmentCount);
        discovery.setLastActivityAt(Instant.now());
        repository.save(discovery);
    }
    public void remove(String courseId) { repository.deleteByCourseId(courseId); }
}
