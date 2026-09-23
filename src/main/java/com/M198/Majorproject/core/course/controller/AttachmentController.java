package com.M198.Majorproject.core.course.controller;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.M198.Majorproject.core.course.dto.AttachmentResponse;
import com.M198.Majorproject.core.course.dto.CreateAttachmentRequest;
import com.M198.Majorproject.core.course.dto.CompleteAttachmentRequest;
import com.M198.Majorproject.core.course.entity.AttachmentResourceType;
import com.M198.Majorproject.core.course.service.AttachmentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> list(
            @RequestParam String resourceId,
            @RequestParam AttachmentResourceType resourceType,
            Authentication authentication) {

        return ResponseEntity.ok(
                attachmentService.list(resourceId, resourceType, authentication));
    }

    @PostMapping("/upload-url")
    public ResponseEntity<AttachmentResponse> create(
            @Valid @RequestBody CreateAttachmentRequest request,
            Authentication authentication) {

        AttachmentResponse response = attachmentService.create(authentication, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{attachmentId}/complete")
    public ResponseEntity<AttachmentResponse> complete(
            @PathVariable String attachmentId,
            @Valid @RequestBody CompleteAttachmentRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                attachmentService.complete(
                        attachmentId,
                        authentication,
                        request));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(
            @PathVariable String attachmentId,
            Authentication authentication) throws IOException {

        attachmentService.delete(attachmentId, authentication);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attachmentId}/download-url")
    public ResponseEntity<AttachmentResponse> downloadUrl(
            @PathVariable String attachmentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attachmentService.downloadUrl(attachmentId, authentication));
    }
}