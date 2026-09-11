package com.M198.Majorproject.dto;

import com.M198.Majorproject.entity.identity.AccountType;
import com.M198.Majorproject.entity.identity.ProfileVisibility;
import lombok.Data;

@Data
public class UserProfileResponse {

    private String id; // The User ID
    private String email;
    private AccountType accountType;

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
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String address;
    private ProfileVisibility profileVisibility;
    private java.util.List<com.M198.Majorproject.entity.identity.ProfileLink> links;
    private boolean canCreateCourses;
}
