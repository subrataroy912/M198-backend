package com.M198.Majorproject.controller.analytics;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.M198.Majorproject.dto.CourseAnalyticsResponse;
import com.M198.Majorproject.dto.GradebookEntryResponse;
import com.M198.Majorproject.dto.TeacherGradebookResponse;
import com.M198.Majorproject.service.analytics.AnalyticsService;

@RestController
@RequestMapping("/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService service;
    public AnalyticsController(AnalyticsService service) { this.service = service; }
    @GetMapping("/courses/{courseId}/summary") public CourseAnalyticsResponse summary(@PathVariable String courseId, Authentication a) { return service.summary(courseId, a); }
    @GetMapping("/courses/{courseId}/gradebook") public List<TeacherGradebookResponse> teacherGradebook(@PathVariable String courseId, Authentication a) { return service.teacherGradebook(courseId, a); }
    @GetMapping("/courses/{courseId}/students/{studentId}/gradebook") public List<GradebookEntryResponse> gradebook(@PathVariable String courseId, @PathVariable String studentId, Authentication a) { return service.gradebook(courseId, studentId, a); }
}
