/**
 * CREATED BY : SUBRATA ROY
 * PROJECT     : Majorproject
 * PURPOSE     : Bootstraps the Spring Boot application and starts the backend server.
 *
 * This is the main entry point of the learning platform backend.
 * The application starts here, loads all Spring configuration, and activates
 * the REST API, security layer, MongoDB connectivity, OAuth login flow, JWT auth,
 * and all domain services for course, profile, notification, submission, and analytics.
 *
 * Written in a clean architecture style so the project remains easy to scale and extend.
 */
package com.M198.Majorproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MajorprojectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MajorprojectApplication.class, args);
    }

}
