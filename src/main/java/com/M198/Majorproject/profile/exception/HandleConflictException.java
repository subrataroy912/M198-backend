package com.M198.Majorproject.profile.exception;

public class HandleConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HandleConflictException() {
        super("Handle is already taken");
    }

    public HandleConflictException(String message) {
        super(message);
    }
}
