package com.M198.Majorproject.dto;

import java.time.Instant;

import com.M198.Majorproject.entity.attachment.AttachmentResourceType;
import com.M198.Majorproject.entity.attachment.AttachmentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachmentResponse {

    private String id;
    private String courseId;
    private AttachmentResourceType resourceType;
    private String resourceId;
    private String ownerId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private AttachmentStatus status;
    private String uploadUrl;
    private String uploadApiKey;
    private String uploadSignature;
    private Long uploadTimestamp;
    private String downloadUrl;
    private Instant createdAt;
}
