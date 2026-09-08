package com.M198.Majorproject.entity.notification;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
@Document(collection = "notifications")
@CompoundIndex(name = "recipient_read_created", def = "{'recipient_id': 1, 'read': 1, 'created_at': -1}")
@CompoundIndex(name = "recipient_created", def = "{'recipient_id': 1, 'created_at': -1}")
public class Notification {

    @Id
    private String id;

    @Indexed
    @Field("recipient_id")
    private String recipientId;

    @Indexed
    private NotificationType type;

    private String title;
    private String message;

    @Field("resource_type")
    private NotificationResourceType resourceType;

    @Field("resource_id")
    private String resourceId;

    @Builder.Default
    private boolean read = false;

    @Field("read_at")
    private Instant readAt;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;
}
