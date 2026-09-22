package com.M198.Majorproject.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AnalyticsAccessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public AnalyticsAccessException() {
        super("You do not have permission to view this analytics data");
    }
}