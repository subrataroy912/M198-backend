package com.M198.Majorproject.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserProfileRequest {

    @Size(max = 30)
    @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "must contain only letters, numbers, or underscores")
    private String handle;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @JsonAlias("name")
    @Size(max = 150)
    private String displayName;

    @Size(max = 200)
    private String headline;

    @Size(max = 5000)
    @JsonAlias("bio")
    private String about;

    @JsonAlias("avatar")
    @Size(max = 10000000)
    private String avatarUrl;

    @JsonAlias("banner")
    @Size(max = 10000000)
    private String bannerUrl;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String phone;

    @Size(max = 50)
    private String gender;

    @Size(max = 50)
    @JsonAlias({"dob", "birthday"})
    private String dateOfBirth;

    @Size(max = 250)
    private String address;

    private com.M198.Majorproject.entity.identity.ProfileVisibility profileVisibility;

    private java.util.List<com.M198.Majorproject.entity.identity.ProfileLink> links;
}
