package com.M198.Majorproject.service.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OAuthUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final ObjectMapper objectMapper;
        private final RestClient restClient;

        public OAuthUserService(
            ObjectMapper objectMapper,
            @Value("${app.oauth2.github-api-base-url:https://api.github.com}") String githubApiBaseUrl) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl(githubApiBaseUrl)
            .defaultHeader("Accept", "application/vnd.github+json")
            .build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User user = delegate.loadUser(userRequest);
        if (!"github".equals(userRequest.getClientRegistration().getRegistrationId())) {
            return user;
        }

        Map<String, Object> attributes = new HashMap<>(user.getAttributes());
        addVerifiedGithubEmail(userRequest, attributes);
        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails()
            .getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(
            AuthorityUtils.createAuthorityList("ROLE_USER"), attributes, nameAttributeKey);
    }

    private void addVerifiedGithubEmail(OAuth2UserRequest userRequest, Map<String, Object> attributes) {
        try {
            String response = restClient.get()
                    .uri("/user/emails")
                    .headers(headers -> headers.setBearerAuth(userRequest.getAccessToken().getTokenValue()))
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                return;
            }
            JsonNode emails = objectMapper.readTree(response);
            if (emails.isArray()) {
                String verifiedPrimaryEmail = null;
                String verifiedBackupEmail = null;

                for (JsonNode email : emails) {
                    boolean isVerified = email.path("verified").asBoolean(false);
                    boolean isPrimary = email.path("primary").asBoolean(false);
                    String emailAddress = email.path("email").asText(null);

                    if (isVerified && emailAddress != null && !emailAddress.isBlank()) {
                        if (isPrimary && verifiedPrimaryEmail == null) {
                            verifiedPrimaryEmail = emailAddress;
                        } else if (verifiedBackupEmail == null) {
                            verifiedBackupEmail = emailAddress;
                        }
                    }
                }

                String chosenEmail = verifiedPrimaryEmail != null ? verifiedPrimaryEmail : verifiedBackupEmail;
                if (chosenEmail != null) {
                    attributes.put("email", chosenEmail);
                    attributes.put("email_verified", true);
                } else {
                    attributes.put("email_verified", false);
                }
            }
        } catch (RestClientException | java.io.IOException ignored) {
            // The success handler will reject the login when verification is unavailable.
        }
    }
}