package com.M198.Majorproject.profile.exception;

import java.io.Serial;

public class ProfileNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ProfileNotFoundException() {
        super("Profile not found");
    }

    public ProfileNotFoundException(String message) {
        super(message);
    }
}
