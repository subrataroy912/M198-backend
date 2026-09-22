package com.M198.Majorproject.user.profile.dto;

import java.util.List;

import com.M198.Majorproject.user.profile.entity.ProfileLink;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import lombok.Data;

@Data
public class UserProfileResponse {

    private String id; // The User ID
    private String email;
    private boolean isAdmin;

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
    private List<ProfileLink> links;
    private boolean canCreateCourses;
    private java.time.Instant joinedAt;
    private long coursesCreatedCount;
    private long coursesEnrolledCount;
    private List<String> badges;
}
