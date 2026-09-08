package com.M198.Majorproject.service.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OAuthUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .build();

    public OAuthUserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        String response = restClient.get()
                .uri("/user/emails")
                .headers(headers -> headers.setBearerAuth(userRequest.getAccessToken().getTokenValue()))
                .retrieve()
                .body(String.class);
        if (response == null) {
            return;
        }
        try {
            JsonNode emails = objectMapper.readTree(response);
            for (JsonNode email : emails) {
                if (email.path("primary").asBoolean(false) && email.path("verified").asBoolean(false)) {
                    attributes.put("email", email.path("email").asText());
                    attributes.put("email_verified", true);
                    return;
                }
            }
        } catch (Exception ignored) {
            // The success handler will reject the login when verification is unavailable.
        }
    }
}