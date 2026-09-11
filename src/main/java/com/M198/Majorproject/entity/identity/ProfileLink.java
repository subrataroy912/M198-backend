package com.M198.Majorproject.entity.identity;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileLink {

    private String name;
    private String url;

    @JsonCreator
    public static ProfileLink fromJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return ProfileLink.builder()
                    .name(extractDomainName(trimmed))
                    .url(trimmed)
                    .build();
        }
        if (value instanceof Map<?, ?> map) {
            Object nameObj = map.get("name");
            Object urlObj = map.get("url");
            String url = urlObj != null ? urlObj.toString().trim() : "";
            if (url.isEmpty()) {
                return null;
            }
            String name = nameObj != null && !nameObj.toString().trim().isEmpty()
                    ? nameObj.toString().trim()
                    : extractDomainName(url);
            return ProfileLink.builder()
                    .name(name)
                    .url(url)
                    .build();
        }
        return null;
    }

    private static String extractDomainName(String url) {
        try {
            String cleaned = url.replaceFirst("^(https?://)?(www\\.)?", "");
            int slashIndex = cleaned.indexOf('/');
            if (slashIndex > 0) {
                cleaned = cleaned.substring(0, slashIndex);
            }
            int dotIndex = cleaned.lastIndexOf('.');
            if (dotIndex > 0) {
                String domain = cleaned.substring(0, dotIndex);
                int secondDot = domain.lastIndexOf('.');
                if (secondDot >= 0) {
                    domain = domain.substring(secondDot + 1);
                }
                return Character.toUpperCase(domain.charAt(0)) + domain.substring(1);
            }
            return cleaned.isEmpty() ? "Link" : cleaned;
        } catch (Exception e) {
            return "Link";
        }
    }
}
