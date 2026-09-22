package com.M198.Majorproject.core.course.adapter;

import java.time.Instant;
import org.springframework.stereotype.Component;
import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.SpaceType;
import com.M198.Majorproject.core.course.port.CourseDiscoveryPort;
import com.M198.Majorproject.discovery.explore.entity.CourseDiscovery;
import com.M198.Majorproject.discovery.explore.repository.CourseDiscoveryRepository;

@Component
class MongoCourseDiscoveryAdapter implements CourseDiscoveryPort {
    private final CourseDiscoveryRepository repository;
    MongoCourseDiscoveryAdapter(CourseDiscoveryRepository repository) { this.repository = repository; }
    public void sync(Course course, long enrollmentCount) {
        CourseDiscovery discovery = repository.findByCourseId(course.getId()).orElseGet(CourseDiscovery::new);
        discovery.setCourseId(course.getId()); discovery.setTitle(course.getTitle());
        discovery.setSpaceType(course.getSpaceType() == null ? SpaceType.ACADEMIC_CLASS : course.getSpaceType());
        discovery.setSubject(course.getSubject()); discovery.setTags(course.getTags()); discovery.setCoverUrl(course.getCoverUrl());
        discovery.setLogoUrl(course.getLogoUrl()); discovery.setTheme(course.getTheme()); discovery.setVisibility(course.getVisibility());
        discovery.setAccessType(course.getAccessType() == null ? CourseAccessType.OPEN : course.getAccessType());
        discovery.setStatus(course.getStatus()); discovery.setEnrollmentCount(enrollmentCount); discovery.setLastActivityAt(Instant.now());
        repository.save(discovery);
    }
    public void remove(String courseId) { repository.deleteByCourseId(courseId); }
}
