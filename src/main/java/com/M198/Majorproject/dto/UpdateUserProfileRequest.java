package com.M198.Majorproject.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @Size(max = 30)
    @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "must contain only letters, numbers, or underscores")
    private String handle;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 150)
    private String displayName;

    @Size(max = 200)
    private String headline;

    @Size(max = 5000)
    private String about;

    @Size(max = 2048)
    private String avatarUrl;

    @Size(max = 2048)
    private String bannerUrl;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String gradeLevel;

    private com.M198.Majorproject.entity.identity.ProfileVisibility profileVisibility;
}
