package com.M198.Majorproject.service.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuthFailureHandler.class);

    private final String frontendCallbackUrl;

    public OAuthFailureHandler(
            @Value("${app.oauth2.frontend-callback-url:http://localhost:5173/auth/callback}") String frontendCallbackUrl) {
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        // Keep the browser-facing error generic so provider and account details
        // are not exposed in the URL, but retain the actual failure in server logs.
        log.warn("OAuth authentication failed for {}: {}", request.getRequestURI(), exception.getMessage(), exception);
        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendCallbackUrl)
                .queryParam("error", "oauth_failed")
                .build()
                .toUriString();
        response.sendRedirect(targetUrl);
    }
}
