package com.M198.Majorproject.user.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserHandleRequest {

    @NotBlank(message = "Handle cannot be blank")
    @Size(min = 3, max = 31, message = "Handle must be between 3 and 31 characters")
    @Pattern(regexp = "^@?[A-Za-z0-9_]+$", message = "Handle must contain only letters, numbers, or underscores (with an optional leading @)")
    private String handle;
}
