package com.M198.Majorproject.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {

    @Bean
    Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        Map<String, String> values = new HashMap<>();
        values.put("cloud_name", cloudName);
        values.put("api_key", apiKey);
        values.put("api_secret", apiSecret);
        return new Cloudinary(values);
    }
}
