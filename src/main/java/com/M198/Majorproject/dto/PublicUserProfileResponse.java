package com.M198.Majorproject.dto;

import com.M198.Majorproject.entity.identity.ProfileVisibility;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicUserProfileResponse {

    private String id;
    private String handle;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;
    private String bannerUrl;
    private String headline;
    private String about;
    private String city;
    private String country;
    private ProfileVisibility profileVisibility;
    private java.util.List<com.M198.Majorproject.entity.identity.ProfileLink> links;
    private com.M198.Majorproject.entity.identity.AccountType accountType;
    private boolean canCreateCourses;

    public String getName() {
        return displayName != null && !displayName.isBlank()
                ? displayName
                : ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    public String getAvatar() {
        return avatarUrl;
    }

    public String getDepartment() {
        return headline != null && !headline.isBlank() ? headline : "";
    }
}
