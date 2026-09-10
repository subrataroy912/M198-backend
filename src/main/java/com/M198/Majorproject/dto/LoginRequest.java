/**
 * CREATED BY : SUBRATA ROY
 * DTO        : LoginRequest
 * PURPOSE    : Carries the email and password used for credential-based sign-in.
 *
 * This request is the entry point for authentication verification before issuing JWT tokens.
 */
package com.M198.Majorproject.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter

@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
