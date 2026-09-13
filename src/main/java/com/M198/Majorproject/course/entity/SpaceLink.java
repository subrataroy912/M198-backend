package com.M198.Majorproject.course.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceLink {
    private String id;
    private String title;
    private String url;
    private String description;
    private String category;
}
