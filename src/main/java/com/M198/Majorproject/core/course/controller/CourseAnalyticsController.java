package com.M198.Majorproject.core.course.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.M198.Majorproject.core.course.dto.CourseAnalyticsResponse;
import com.M198.Majorproject.core.course.dto.CourseGradebookResponse;
import com.M198.Majorproject.core.course.dto.GradebookEntryResponse;
import com.M198.Majorproject.core.course.service.AnalyticsService;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
public class CourseAnalyticsController {
    private final AnalyticsService service;


    @GetMapping("/courses/{courseId}/summary")
    public CourseAnalyticsResponse summary(@PathVariable String courseId, Authentication a) {
        return service.summary(courseId, a);
    }

    @GetMapping("/courses/{courseId}/gradebook")
    public List<CourseGradebookResponse> courseGradebook(@PathVariable String courseId, Authentication a) {
        return service.courseGradebook(courseId, a);
    }

    @GetMapping({"/courses/{courseId}/members/{memberId}/gradebook", "/courses/{courseId}/students/{memberId}/gradebook"})
    public List<GradebookEntryResponse> gradebook(@PathVariable String courseId, @PathVariable String memberId, Authentication a) {
        return service.gradebook(courseId, memberId, a);
    }
}
