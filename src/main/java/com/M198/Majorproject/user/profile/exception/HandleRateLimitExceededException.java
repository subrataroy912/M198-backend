package com.M198.Majorproject.user.profile.exception;

import java.time.Instant;
import lombok.Getter;

@Getter
public class HandleRateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Instant resetsAt;

    public HandleRateLimitExceededException() {
        super("You can only change your handle 3 times within a 14-day period.");
        this.resetsAt = null;
    }

    public HandleRateLimitExceededException(String message) {
        super(message);
        this.resetsAt = null;
    }

    public HandleRateLimitExceededException(String message, Instant resetsAt) {
        super(message);
        this.resetsAt = resetsAt;
    }
}
