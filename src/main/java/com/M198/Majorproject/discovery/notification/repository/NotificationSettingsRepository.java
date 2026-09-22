package com.M198.Majorproject.discovery.notification.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.discovery.notification.entity.NotificationSettings;

public interface NotificationSettingsRepository extends MongoRepository<NotificationSettings, String> {

    Optional<NotificationSettings> findByUserId(String userId);
}
