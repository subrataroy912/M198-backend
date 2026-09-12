package com.M198.Majorproject.controller.submission;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.dto.GradeSubmissionRequest;
import com.M198.Majorproject.dto.SubmissionResponse;
import com.M198.Majorproject.dto.PageResponse;
import com.M198.Majorproject.dto.UpdateSubmissionRequest;
import com.M198.Majorproject.service.submission.SubmissionService;

@RestController
@RequestMapping("/v1/coursework/{courseworkId}/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

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
