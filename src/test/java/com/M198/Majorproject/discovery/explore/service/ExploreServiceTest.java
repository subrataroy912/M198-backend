package com.M198.Majorproject.discovery.explore.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.discovery.explore.entity.CourseDiscovery;
import com.M198.Majorproject.discovery.explore.repository.CourseDiscoveryRepository;

class ExploreServiceTest {

        private final CourseDiscoveryRepository repository = mock(CourseDiscoveryRepository.class);
        private final ExploreService service = new ExploreService(repository);

        // Defined as a constant to keep the tests clean and readable
        private final List<CourseVisibility> allowedVisibilities = List.of(
                        CourseVisibility.PUBLIC,
                        CourseVisibility.PRIVATE);

        @Test
        void feedRequestsPublicAndPrivateActiveCourses() {
                CourseDiscovery course = CourseDiscovery.builder()
                                .courseId("course-1")
                                .title("Public course")
                                .coverUrl("https://example.com/cover.png")
                                .logoUrl("https://example.com/logo.png")
                                .theme("emerald")
                                .visibility(CourseVisibility.PUBLIC)
                                .status(CourseStatus.ACTIVE)
                                .build();

                // Updated to use the list of visibilities
                when(repository.findFeed(
                                allowedVisibilities, CourseStatus.ACTIVE, PageRequest.of(0, 20)))
                                .thenReturn(new PageImpl<>(List.of(course), PageRequest.of(0, 20), 1));

                var result = service.feed(null, 0, 20);

                assertEquals("course-1", result.getContent().get(0).getCourseId());
                assertEquals("Public course", result.getContent().get(0).getTitle());
                assertEquals("https://example.com/cover.png", result.getContent().get(0).getCoverUrl());
                assertEquals("https://example.com/logo.png", result.getContent().get(0).getLogoUrl());
                assertEquals("emerald", result.getContent().get(0).getTheme());
                assertEquals("https://example.com/cover.png", result.getContent().get(0).getCover());
                assertEquals("https://example.com/logo.png", result.getContent().get(0).getLogo());

                // Updated to verify using the list of visibilities
                verify(repository).findFeed(
                                allowedVisibilities, CourseStatus.ACTIVE, PageRequest.of(0, 20));
        }

        @Test
        void archivedCoursesAreExcludedByFeedQuery() {
                // Updated to use the list of visibilities
                when(repository.findFeed(
                                allowedVisibilities, CourseStatus.ACTIVE, PageRequest.of(0, 20)))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

                service.feed(null, 0, 20);

                // Updated to verify using the list of visibilities
                verify(repository).findFeed(
                                allowedVisibilities, CourseStatus.ACTIVE, PageRequest.of(0, 20));
        }
}