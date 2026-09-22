package com.M198.Majorproject.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final HttpStatus status = HttpStatus.UNAUTHORIZED;

    public UnauthorizedException() {
        super("User is not authenticated");
    }

    public UnauthorizedException(String message) {
        super(message);
    }

}
