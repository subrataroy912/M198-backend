package com.M198.Majorproject.core.course.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseworkAttachment {

    private String id;
    private String type; // IMAGE, FILE, VIDEO, LINK
    private String title;
    private String url;
    private Long sizeBytes;
}
