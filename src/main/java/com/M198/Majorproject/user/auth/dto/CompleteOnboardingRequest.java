package com.M198.Majorproject.user.auth.dto;

import java.util.List;

import com.M198.Majorproject.user.profile.entity.ProfileLink;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteOnboardingRequest {

    @NotBlank(message = "Handle cannot be blank")
    @Size(min = 3, max = 31, message = "Handle must be between 3 and 31 characters")
    @Pattern(regexp = "^@?[A-Za-z0-9_]+$", message = "Handle must contain only letters, numbers, or underscores (with an optional leading @)")
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
    @JsonAlias({ "dob", "birthday" })
    private String dateOfBirth;

    @Size(max = 250)
    private String address;

    private ProfileVisibility profileVisibility;

    private List<ProfileLink> links;

    private List<String> tags;
}
