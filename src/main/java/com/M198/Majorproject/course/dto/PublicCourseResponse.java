package com.M198.Majorproject.course.dto;

import com.M198.Majorproject.course.entity.CourseAccessType;
import com.M198.Majorproject.course.entity.CourseVisibility;

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
  private CourseVisibility visibility;
  private CourseAccessType accessType;
  private boolean enrollmentEnabled;
  private long memberCount;
}
