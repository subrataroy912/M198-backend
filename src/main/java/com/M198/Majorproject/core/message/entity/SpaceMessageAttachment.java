package com.M198.Majorproject.core.message.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMessageAttachment {
    private String attachmentId;
    private String name;
    private String mimeType;
    private String url;
    private long sizeBytes;
}
