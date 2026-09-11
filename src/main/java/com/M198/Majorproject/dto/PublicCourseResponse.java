package com.M198.Majorproject.dto;

import com.M198.Majorproject.entity.course.CourseAccessType;
import com.M198.Majorproject.entity.course.CourseVisibility;

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
  private CourseVisibility visibility;
  private CourseAccessType accessType;
  private boolean enrollmentEnabled;
  private long memberCount;
}
