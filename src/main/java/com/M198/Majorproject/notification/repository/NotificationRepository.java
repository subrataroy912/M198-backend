package com.M198.Majorproject.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.notification.entity.Notification;
import com.M198.Majorproject.notification.entity.NotificationResourceType;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Page<Notification> findAllByRecipientIdAndReadFalseOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    void deleteAllByResourceTypeAndResourceId(NotificationResourceType resourceType, String resourceId);

    void deleteAllByResourceTypeAndResourceIdIn(NotificationResourceType resourceType, Collection<String> resourceIds);
}
