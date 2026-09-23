package com.M198.Majorproject.core.course.dto;

import java.util.List;

import com.M198.Majorproject.core.course.entity.CourseAccessType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicCourseResponse {

  private String id;
  private String title;
  private String section;
  private String subject;
  private String description;
  private String coverUrl;
  private String logoUrl;
  private String theme;
  private List<String> tags;
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
