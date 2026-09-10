package com.M198.Majorproject.service.explore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;
import com.M198.Majorproject.entity.explore.CourseDiscovery;
import com.M198.Majorproject.repository.explore.CourseDiscoveryRepository;

class ExploreServiceTest {

    private final CourseDiscoveryRepository repository = mock(CourseDiscoveryRepository.class);
    private final ExploreService service = new ExploreService(repository);

    @Test
    void feedRequestsOnlyPublicActiveCourses() {
        CourseDiscovery course = CourseDiscovery.builder()
                .courseId("course-1")
                .title("Public course")
                .visibility(CourseVisibility.PUBLIC)
                .status(CourseStatus.ACTIVE)
                .build();
        when(repository.findAllByVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
                CourseVisibility.PUBLIC, CourseStatus.ACTIVE, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(course), PageRequest.of(0, 20), 1));

        var result = service.feed(null, 0, 20);

        assertEquals("course-1", result.getContent().get(0).getCourseId());
        verify(repository).findAllByVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
                CourseVisibility.PUBLIC, CourseStatus.ACTIVE, PageRequest.of(0, 20));
    }

    @Test
    void privateAndArchivedCoursesAreExcludedByPublicFeedQuery() {
        when(repository.findAllByVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
                CourseVisibility.PUBLIC, CourseStatus.ACTIVE, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.feed(null, 0, 20);

        verify(repository).findAllByVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
                CourseVisibility.PUBLIC, CourseStatus.ACTIVE, PageRequest.of(0, 20));
    }
}
