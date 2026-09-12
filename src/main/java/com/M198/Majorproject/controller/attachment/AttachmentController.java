package com.M198.Majorproject.controller.attachment;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.dto.AttachmentResponse;
import com.M198.Majorproject.dto.CreateAttachmentRequest;
import com.M198.Majorproject.dto.CompleteAttachmentRequest;
import com.M198.Majorproject.entity.attachment.AttachmentResourceType;
import com.M198.Majorproject.service.attachment.AttachmentService;

@RestController
@RequestMapping("/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload-url")
    public AttachmentResponse create(
            @Valid @RequestBody CreateAttachmentRequest request,
            Authentication authentication) {
        return attachmentService.create(authentication, request);
    }

    @PostMapping("/{attachmentId}/complete")
    public AttachmentResponse complete(
            @PathVariable String attachmentId,
            @Valid @RequestBody CompleteAttachmentRequest request,
            Authentication authentication) {
        return attachmentService.complete(attachmentId, authentication, request);
    }

    @GetMapping
    public List<AttachmentResponse> list(
            @RequestParam String resourceId,
            @RequestParam AttachmentResourceType resourceType,
            Authentication authentication) {
        return attachmentService.list(resourceId, resourceType, authentication);
    }

    @DeleteMapping("/{attachmentId}")
    public void delete(@PathVariable String attachmentId, Authentication authentication) throws IOException {
        attachmentService.delete(attachmentId, authentication);
    }

    @GetMapping("/{attachmentId}/download-url")
    public AttachmentResponse downloadUrl(
            @PathVariable String attachmentId, Authentication authentication) {
        return attachmentService.downloadUrl(attachmentId, authentication);
    }
}
