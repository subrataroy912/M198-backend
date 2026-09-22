package com.M198.Majorproject.core.attachment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.M198.Majorproject.core.attachment.entity.AttachmentResourceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAttachmentRequest {

    @NotNull
    private AttachmentResourceType resourceType;

    @NotBlank
    private String resourceId;

    @NotBlank
    @Size(max = 255)
    private String originalFilename;

    @NotBlank
    @Size(max = 160)
    private String contentType;

    @NotNull
    @Positive
    private Long sizeBytes;
}
