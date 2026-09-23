package com.M198.Majorproject.user.auth.exception;

public class RefreshTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RefreshTokenException(String message) {
        super(message);
    }

    public RefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
