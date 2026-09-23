package com.M198.Majorproject.core.course.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.M198.Majorproject.core.course.dto.CourseworkResponse;
import com.M198.Majorproject.core.course.dto.CreateCourseworkRequest;
import com.M198.Majorproject.common.dto.PageResponse;
import com.M198.Majorproject.core.course.dto.UpdateCourseworkRequest;
import com.M198.Majorproject.core.course.service.CourseworkService;

@RestController
@RequestMapping("/v1/courses/{courseId}/coursework")
@RequiredArgsConstructor
public class CourseworkController {

    private final CourseworkService courseworkService;

    @PostMapping
    public CourseworkResponse create(
            @PathVariable String courseId,
            @Valid @RequestBody CreateCourseworkRequest request,
            Authentication authentication) {
        return courseworkService.create(courseId, authentication, request);
    }

    @GetMapping
    public PageResponse<CourseworkResponse> list(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return PageResponse.from(courseworkService.list(courseId, authentication, page, size));
    }

    @GetMapping("/{courseworkId}")
    public CourseworkResponse get(
            @PathVariable String courseId,
            @PathVariable String courseworkId,
            Authentication authentication) {
        return courseworkService.get(courseId, courseworkId, authentication);
    }

    @PatchMapping("/{courseworkId}")
    public CourseworkResponse update(
            @PathVariable String courseId,
            @PathVariable String courseworkId,
            @Valid @RequestBody UpdateCourseworkRequest request,
            Authentication authentication) {
        return courseworkService.update(courseId, courseworkId, authentication, request);
    }

    @DeleteMapping("/{courseworkId}")
    public void archive(
            @PathVariable String courseId,
            @PathVariable String courseworkId,
            Authentication authentication) {
        courseworkService.archive(courseId, courseworkId, authentication);
    }
}
