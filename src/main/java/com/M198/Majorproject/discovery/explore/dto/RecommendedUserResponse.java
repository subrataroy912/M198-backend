package com.M198.Majorproject.discovery.explore.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedUserResponse {

    private String id;
    private String handle;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;
    private String bannerUrl;
    private String headline;
    private String department;
    private boolean canCreateCourses;
    private long sharedCoursesCount;
    private List<String> sharedCourseTitles;
    private long mutualPeersCount;
    private boolean sameDepartment;
    private String recommendationReason;

    public String getName() {
        return displayName != null && !displayName.isBlank()
                ? displayName
                : ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    public String getAvatar() {
        return avatarUrl;
    }
}
