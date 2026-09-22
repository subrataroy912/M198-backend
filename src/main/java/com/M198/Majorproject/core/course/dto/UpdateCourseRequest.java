package com.M198.Majorproject.core.course.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.core.course.entity.MeetingType;
import com.M198.Majorproject.core.course.entity.SpaceLink;
import com.M198.Majorproject.core.course.entity.SpaceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCourseRequest {

    @Size(max = 120)
    @JsonAlias("name")
    private String title;

    private SpaceType spaceType;

    @Size(max = 80)
    private String section;

    @Size(max = 80)
    private String subject;

    @Size(max = 2000)
    private String description;

    @JsonAlias({"cover", "banner", "bannerUrl"})
    @Size(max = 10000000)
    private String coverUrl;

    @JsonAlias({"logo", "avatar", "logoUrl"})
    @Size(max = 10000000)
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

    public String getName() {
        return title;
    }

    public void setName(String name) {
        if (this.title == null || this.title.isBlank()) {
            this.title = name;
        }
    }

    public String getCover() {
        return coverUrl;
    }

    public void setCover(String cover) {
        if (this.coverUrl == null || this.coverUrl.isBlank()) {
            this.coverUrl = cover;
        }
    }

    public String getLogo() {
        return logoUrl;
    }

    public void setLogo(String logo) {
        if (this.logoUrl == null || this.logoUrl.isBlank()) {
            this.logoUrl = logo;
        }
    }
}
