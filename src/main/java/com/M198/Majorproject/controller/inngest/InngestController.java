package com.M198.Majorproject.controller.inngest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the application Inngest serve endpoint.
 *
 * <p>The Inngest Spring adapter registers the function handler from
 * {@code InngestConfig}; this controller ensures that the configured serve URL
 * is also a Spring MVC route.  Inngest uses GET while discovering functions and
 * POST/PUT while invoking them, so all three methods must reach this endpoint.
 */
@RestController
@RequestMapping("/api/inngest")
public class InngestController {

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT })
    public ResponseEntity<Void> serve() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
