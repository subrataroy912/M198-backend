package com.M198.Majorproject.course.dto;

import java.util.List;

import com.M198.Majorproject.course.entity.CourseAccessType;
import com.M198.Majorproject.course.entity.CourseVisibility;
import com.M198.Majorproject.course.entity.MeetingType;
import com.M198.Majorproject.course.entity.SpaceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicCourseResponse {

  private String id;
  private String title;
  private SpaceType spaceType;
  private String section;
  private String subject;
  private String description;
  private String coverUrl;
  private String logoUrl;
  private String theme;
  private MeetingType meetingType;
  private String location;
  private List<String> tags;
  private CourseVisibility visibility;
  private CourseAccessType accessType;
  private boolean enrollmentEnabled;
  private long memberCount;

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
