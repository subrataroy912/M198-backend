package com.M198.Majorproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteAttachmentRequest {

    @NotBlank
    private String publicId;

    @Positive
    private Long sizeBytes;
}
