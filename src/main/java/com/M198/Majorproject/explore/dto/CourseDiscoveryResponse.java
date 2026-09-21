package com.M198.Majorproject.explore.dto;

import java.time.Instant;
import java.util.List;
import com.M198.Majorproject.course.entity.CourseAccessType;
import com.M198.Majorproject.course.entity.SpaceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseDiscoveryResponse {
    private String courseId;
    private String title;
    private SpaceType spaceType;
    private String subject;
    private List<String> tags;
    private String coverUrl;
    private String logoUrl;
    private String theme;
    private CourseAccessType accessType;
    private long enrollmentCount;
    private double popularityScore;
    private Instant lastActivityAt;

    public String getId() {
        return courseId;
    }

    public String getName() {
        return title;
    }

    public String getCover() {
        return coverUrl;
    }

    public String getLogo() {
        return logoUrl;
    }
}
