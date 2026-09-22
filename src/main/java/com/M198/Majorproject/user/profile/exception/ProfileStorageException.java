package com.M198.Majorproject.user.profile.exception;

public class ProfileStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProfileStorageException(String message) {
        super(message);
    }

    public ProfileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
