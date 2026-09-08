package com.M198.Majorproject.dto;

import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import lombok.Data;

@Data
public class UserProfileResponse {

    private String id; // The User ID
    private String email;
    private AccountType accountType;

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
    private String gradeLevel;
}
