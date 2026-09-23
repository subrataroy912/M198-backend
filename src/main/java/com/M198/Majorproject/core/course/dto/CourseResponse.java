package com.M198.Majorproject.core.course.dto;

import java.time.Instant;
import java.util.List;

import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.core.course.entity.MeetingType;
import com.M198.Majorproject.core.course.entity.SpaceLink;
import com.M198.Majorproject.core.course.entity.SpaceType;

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
    private com.M198.Majorproject.core.course.entity.MembershipStatus membershipStatus;
    private String inviteToken;
    private String inviteUrl;
    private Instant inviteExpiresAt;
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

    public OwnerSummary getOwner() {
        return new OwnerSummary(ownerId, ownerName != null ? ownerName : "Space Owner", ownerAvatarUrl);
    }

    public static class OwnerSummary {
        private String id;
        private String name;
        private String avatarUrl;

        public OwnerSummary() {}

        public OwnerSummary(String id, String name, String avatarUrl) {
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
