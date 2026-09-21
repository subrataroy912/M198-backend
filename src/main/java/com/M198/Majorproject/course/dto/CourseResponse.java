package com.M198.Majorproject.course.dto;

import java.time.Instant;
import java.util.List;

import com.M198.Majorproject.course.entity.CourseAccessType;
import com.M198.Majorproject.course.entity.CourseStatus;
import com.M198.Majorproject.course.entity.CourseVisibility;
import com.M198.Majorproject.course.entity.MeetingType;
import com.M198.Majorproject.course.entity.SpaceLink;
import com.M198.Majorproject.course.entity.SpaceType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseResponse {

    private String id;
    private String ownerId;
    private String ownerName;
    private String ownerAvatarUrl;
    private long memberCount;
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
    private List<SpaceLink> links;
    private CourseVisibility visibility;
    private CourseAccessType accessType;
    private CourseStatus status;
    private boolean enrollmentEnabled;
    private String enrollmentCode;
    private String role;
    private boolean enrolled;
    private Instant createdAt;
    private Instant updatedAt;

    public String getName() {
        return title;
    }

    public String getCover() {
        return coverUrl;
    }

    public String getLogo() {
        return logoUrl;
    }

    public String getCode() {
        return enrollmentCode;
    }

    public TeacherSummary getTeacher() {
        return new TeacherSummary(ownerId, ownerName != null ? ownerName : "CampusMind Teacher", ownerAvatarUrl);
    }

    public static class TeacherSummary {
        private String id;
        private String name;
        private String avatarUrl;

        public TeacherSummary() {}

        public TeacherSummary(String id, String name, String avatarUrl) {
            this.id = id;
            this.name = name;
            this.avatarUrl = avatarUrl;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
}
