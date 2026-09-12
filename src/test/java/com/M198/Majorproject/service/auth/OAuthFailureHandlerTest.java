package com.M198.Majorproject.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;

class OAuthFailureHandlerTest {

    @Test
    void redirectsToFrontendCallbackWithGenericError() throws Exception {
        OAuthFailureHandler handler = new OAuthFailureHandler(
                "http://localhost:5173/auth/callback?source=oauth");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new AuthenticationServiceException("provider details"));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/callback?source=oauth#error=oauth_failed");
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void redirectsToFrontendCallbackWithUnverifiedEmailError() throws Exception {
        OAuthFailureHandler handler = new OAuthFailureHandler(
                "http://localhost:5173/auth/callback?source=oauth");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                        new org.springframework.security.oauth2.core.OAuth2Error("unverified_email"),
                        "Provider email is missing or unverified"));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/callback?source=oauth#error=unverified_email");
        assertThat(response.getContentAsString()).isEmpty();
    }
}
