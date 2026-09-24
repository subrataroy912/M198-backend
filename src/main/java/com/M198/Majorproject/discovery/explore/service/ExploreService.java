package com.M198.Majorproject.discovery.explore.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.M198.Majorproject.discovery.explore.dto.CourseDiscoveryResponse;
import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.discovery.explore.entity.CourseDiscovery;
import com.M198.Majorproject.discovery.explore.repository.CourseDiscoveryRepository;

@Service
@RequiredArgsConstructor
public class ExploreService {

    private final CourseDiscoveryRepository repository;

    public Page<CourseDiscoveryResponse> feed(String subject, int page, int size) {
        Pageable pageable = pageable(page, size);
        Page<CourseDiscovery> courses;

        List<CourseVisibility> allowedVisibilities = List.of(
                CourseVisibility.PUBLIC,
                CourseVisibility.PRIVATE);

        if (subject == null || subject.isBlank()) {
            courses = repository.findFeed(
                    allowedVisibilities,
                    CourseStatus.ACTIVE,
                    pageable);
        } else {
            courses = repository.findFeedBySubject(
                    subject.trim(),
                    allowedVisibilities,
                    CourseStatus.ACTIVE,
                    pageable);
        }

        return courses.map(this::response);
    }

    public Page<CourseDiscoveryResponse> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("q must not be blank");
        }

        List<CourseVisibility> allowedVisibilities = List.of(
                CourseVisibility.PUBLIC,
                CourseVisibility.PRIVATE);

        return repository.searchFeedByTitle(
                query.trim(),
                allowedVisibilities,
                CourseStatus.ACTIVE,
                pageable(page, size)).map(this::response);
    }

    public Page<CourseDiscoveryResponse> recommendations(int page, int size) {
        return feed(null, page, size);
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");

        }
        return PageRequest.of(page, size);
    }

    private CourseDiscoveryResponse response(CourseDiscovery value) {
        CourseDiscoveryResponse r = new CourseDiscoveryResponse();
        r.setCourseId(value.getCourseId());
        r.setTitle(value.getTitle());
        r.setSubject(value.getSubject());
        r.setTags(value.getTags());
        r.setCoverUrl(value.getCoverUrl());
        r.setLogoUrl(value.getLogoUrl());
        r.setTheme(value.getTheme());
        r.setAccessType(value.getAccessType() != null ? value.getAccessType() : CourseAccessType.PUBLIC);
        r.setEnrollmentCount(value.getEnrollmentCount());
        r.setPopularityScore(value.getPopularityScore());
        r.setLastActivityAt(value.getLastActivityAt());
        return r;
    }
}
