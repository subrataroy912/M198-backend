package com.M198.Majorproject.core.course.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.M198.Majorproject.common.dto.PageResponse;
import com.M198.Majorproject.core.course.dto.*;
import com.M198.Majorproject.core.course.service.SubmissionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/coursework/{courseworkId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public SubmissionResponse start(@PathVariable String courseworkId, Authentication authentication) {
        return submissionService.start(courseworkId, authentication);
    }

    @GetMapping
    public PageResponse<SubmissionResponse> list(
            @PathVariable String courseworkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return PageResponse.from(submissionService.list(courseworkId, authentication, page, size));
    }

    @GetMapping("/me")
    public SubmissionResponse mine(@PathVariable String courseworkId, Authentication authentication) {
        return submissionService.mine(courseworkId, authentication);
    }

    @PatchMapping("/me")
    public SubmissionResponse update(
            @PathVariable String courseworkId,
            @Valid @RequestBody UpdateSubmissionRequest request,
            Authentication authentication) {
        return submissionService.update(courseworkId, authentication, request);
    }

    @PatchMapping("/{submissionId}/grade")
    public SubmissionResponse grade(
            @PathVariable String courseworkId,
            @PathVariable String submissionId,
            @Valid @RequestBody GradeSubmissionRequest request,
            Authentication authentication) {
        return submissionService.grade(courseworkId, submissionId, authentication, request);
    }
}
