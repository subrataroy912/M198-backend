package com.M198.Majorproject.controller.coursework;

import org.springframework.data.domain.Page;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.dto.CourseworkResponse;
import com.M198.Majorproject.dto.CreateCourseworkRequest;
import com.M198.Majorproject.dto.PageResponse;
import com.M198.Majorproject.dto.UpdateCourseworkRequest;
import com.M198.Majorproject.service.coursework.CourseworkService;

@RestController
@RequestMapping("/v1/courses/{courseId}/coursework")
public class CourseworkApiController {

    private final CourseworkService courseworkService;

    public CourseworkApiController(CourseworkService courseworkService) {
        this.courseworkService = courseworkService;
    }

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
