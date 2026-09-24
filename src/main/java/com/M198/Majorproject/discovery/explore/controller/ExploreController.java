package com.M198.Majorproject.discovery.explore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.M198.Majorproject.discovery.explore.dto.CourseDiscoveryResponse;
import com.M198.Majorproject.common.dto.PageResponse;
import com.M198.Majorproject.core.course.dto.PublicCourseResponse;
import com.M198.Majorproject.core.course.service.CourseService;
import com.M198.Majorproject.discovery.explore.service.ExploreService;

import com.M198.Majorproject.discovery.explore.dto.RecommendedUserResponse;
import com.M198.Majorproject.discovery.explore.service.UserRecommendationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/explore")
public class ExploreController {

    private final ExploreService service;
    private final CourseService courseService;
    private final UserRecommendationService userRecommendationService;

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

    @GetMapping("/people/recommendations")
    public PageResponse<RecommendedUserResponse> peopleRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return PageResponse.from(userRecommendationService.getRecommendedUsers(authentication, page, size));
    }
}
