package com.M198.Majorproject.repository.notification;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.notification.NotificationSettings;

public interface NotificationSettingsRepository extends MongoRepository<NotificationSettings, String> {

    Optional<NotificationSettings> findByUserId(String userId);
}
