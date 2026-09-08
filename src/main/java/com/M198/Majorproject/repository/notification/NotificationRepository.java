package com.M198.Majorproject.repository.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.notification.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Page<Notification> findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc(String recipientId, Pageable pageable);
}
