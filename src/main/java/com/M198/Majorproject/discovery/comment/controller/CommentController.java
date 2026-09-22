package com.M198.Majorproject.discovery.comment.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.M198.Majorproject.discovery.comment.dto.CommentResponse;
import com.M198.Majorproject.discovery.comment.dto.CreateCommentRequest;
import com.M198.Majorproject.discovery.comment.service.CommentService;

@RestController
@RequestMapping("/v1")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @PostMapping("/coursework/{courseworkId}/comments")
    public CommentResponse addCoursework(@PathVariable String courseworkId, @Valid @RequestBody CreateCommentRequest request, Authentication a) {
        return service.addCourseworkComment(courseworkId, a, request);
    }

    @GetMapping("/coursework/{courseworkId}/comments")
    public List<CommentResponse> coursework(@PathVariable String courseworkId, Authentication a) {
        return service.courseworkComments(courseworkId, a);
    }

    @PostMapping("/submissions/{submissionId}/comments")
    public CommentResponse addSubmission(@PathVariable String submissionId, @Valid @RequestBody CreateCommentRequest request, Authentication a) {
        return service.addSubmissionComment(submissionId, a, request);
    }

    @GetMapping("/submissions/{submissionId}/comments")
    public List<CommentResponse> submission(@PathVariable String submissionId, Authentication a) {
        return service.submissionComments(submissionId, a);
    }
}
