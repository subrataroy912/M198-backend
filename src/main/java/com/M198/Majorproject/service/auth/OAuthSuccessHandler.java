package com.M198.Majorproject.service.auth;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.M198.Majorproject.dto.AuthResponse;
import com.M198.Majorproject.entity.identity.OAuthProvider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final OAuthFailureHandler failureHandler;

    public OAuthSuccessHandler(
            AuthService authService,
            ObjectMapper objectMapper,
            OAuthFailureHandler failureHandler) {
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.failureHandler = failureHandler;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauth.getPrincipal();
        Map<String, Object> attributes = principal.getAttributes();
        OAuthProvider provider = OAuthProvider.valueOf(oauth.getAuthorizedClientRegistrationId().toUpperCase());
        String email = value(attributes, "email");
        String providerUserId = value(attributes, "sub");
        if (providerUserId == null) {
            providerUserId = value(attributes, "id");
        }
        String displayName = value(attributes, "name");
        if (displayName == null) {
            displayName = value(attributes, "login");
        }
        String avatarUrl = value(attributes, "picture");
        if (avatarUrl == null) {
            avatarUrl = value(attributes, "avatar_url");
        }
        boolean emailVerified = Boolean.TRUE.equals(attributes.get("email_verified"));

        AuthResponse result;
        try {
            result = authService.authenticateOAuth(
                provider, providerUserId, email, emailVerified, displayName, avatarUrl);
        } catch (RuntimeException exception) {
            failureHandler.onAuthenticationFailure(request, response,
                new org.springframework.security.authentication.AuthenticationServiceException(
                    "OAuth account provisioning failed", exception));
            return;
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private String value(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : value.toString();
    }
}