package com.M198.Majorproject.controller.explore;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.M198.Majorproject.dto.CourseDiscoveryResponse;
import com.M198.Majorproject.dto.PageResponse;
import com.M198.Majorproject.dto.PublicCourseResponse;
import com.M198.Majorproject.service.course.CourseService;
import com.M198.Majorproject.service.explore.ExploreService;

@RestController
@RequestMapping("/v1/explore")
public class ExploreApiController {

    private final ExploreService service;
    private final CourseService courseService;

    public ExploreApiController(ExploreService service, CourseService courseService) {
        this.service = service;
        this.courseService = courseService;
    }

    @GetMapping("/feed")
    public PageResponse<CourseDiscoveryResponse> feed(@RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.feed(subject, page, size));
    }

    @GetMapping("/courses/search")
    public PageResponse<CourseDiscoveryResponse> search(@RequestParam String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.search(q, page, size));
    }

    @GetMapping("/courses/{courseId}")
    public PublicCourseResponse course(@PathVariable String courseId) {
        return courseService.getPublicCourse(courseId);
    }

    @GetMapping("/recommendations")
    public PageResponse<CourseDiscoveryResponse> recommendations(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.recommendations(page, size));
    }
}
