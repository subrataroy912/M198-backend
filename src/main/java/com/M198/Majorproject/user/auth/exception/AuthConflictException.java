package com.M198.Majorproject.user.auth.exception;

public class AuthConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AuthConflictException(String message) {
        super(message);
    }

    public AuthConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
