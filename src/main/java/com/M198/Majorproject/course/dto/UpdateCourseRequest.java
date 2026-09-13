package com.M198.Majorproject.course.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

import com.M198.Majorproject.course.entity.CourseAccessType;
import com.M198.Majorproject.course.entity.CourseVisibility;
import com.M198.Majorproject.course.entity.MeetingType;
import com.M198.Majorproject.course.entity.SpaceLink;
import com.M198.Majorproject.course.entity.SpaceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseRequest {
    @Size(max = 120)
    private String title;
    private SpaceType spaceType;
    @Size(max = 80)
    private String section;
    @Size(max = 80)
    private String subject;
    @Size(max = 2000)
    private String description;
    @Size(max = 2048)
    private String coverUrl;
    @Size(max = 2048)
    private String logoUrl;
    private CourseAccessType accessType;
    private CourseVisibility visibility;
    private MeetingType meetingType;
    @Size(max = 500)
    private String location;
    private List<String> tags;
    private List<SpaceLink> links;
    private Boolean enrollmentEnabled;
    @Size(max = 64)
    private String theme;
}
