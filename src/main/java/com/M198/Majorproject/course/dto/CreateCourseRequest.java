package com.M198.Majorproject.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import com.M198.Majorproject.course.entity.CourseAccessType;
import com.M198.Majorproject.course.entity.CourseVisibility;
import com.M198.Majorproject.course.entity.MeetingType;
import com.M198.Majorproject.course.entity.SpaceLink;
import com.M198.Majorproject.course.entity.SpaceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    private SpaceType spaceType;

    @Size(max = 80)
    private String section;

    @Size(max = 80)
    private String subject;

    @Size(max = 2000)
    private String description;

    private CourseVisibility visibility;

    private CourseAccessType accessType;

    private MeetingType meetingType;

    @Size(max = 500)
    private String location;

    private List<String> tags;

    private List<SpaceLink> links;

    @Size(max = 2048)
    private String coverUrl;

    @Size(max = 2048)
    private String logoUrl;

    @Size(max = 64)
    private String theme;
}
