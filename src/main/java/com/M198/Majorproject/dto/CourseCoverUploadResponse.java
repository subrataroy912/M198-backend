package com.M198.Majorproject.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseCoverUploadResponse {

    private String uploadUrl;
    private String publicId;
    private String uploadApiKey;
    private String uploadSignature;
    private Long uploadTimestamp;
}
