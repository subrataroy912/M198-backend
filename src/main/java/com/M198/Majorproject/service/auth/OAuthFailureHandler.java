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
        log.warn("OAuth authentication failed for {}: {}", request.getRequestURI(), exception.getMessage(), exception);
        String errorCode = "oauth_failed";
        if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oauth2Ex
                && "unverified_email".equals(oauth2Ex.getError().getErrorCode())) {
            errorCode = "unverified_email";
        }
        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendCallbackUrl)
                .fragment(UriComponentsBuilder.newInstance()
                        .queryParam("error", errorCode)
                        .build()
                        .getQuery())
                .build()
                .toUriString();
        response.sendRedirect(targetUrl);
    }
}
