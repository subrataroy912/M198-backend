package com.M198.Majorproject.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AnalyticsNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public AnalyticsNotFoundException() {
        super("Analytics data not found");
    }
}