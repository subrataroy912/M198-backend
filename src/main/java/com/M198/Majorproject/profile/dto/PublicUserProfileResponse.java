package com.M198.Majorproject.profile.dto;

import java.util.List;

import com.M198.Majorproject.identity.entity.AccountType;
import com.M198.Majorproject.identity.entity.ProfileLink;
import com.M198.Majorproject.identity.entity.ProfileVisibility;
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
    private List<ProfileLink> links;
    private AccountType accountType;
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
