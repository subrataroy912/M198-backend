package com.M198.Majorproject.core.message.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@Document(collection = "space_messages")
@CompoundIndex(name = "space_cursor_idx", def = "{'space_id': 1, 'deleted': 1, '_id': -1}")
@CompoundIndex(name = "space_created_idx", def = "{'space_id': 1, 'deleted': 1, 'created_at': -1}")
public class SpaceMessage {

    @Id
    private String id;

    @Indexed
    @Field("space_id")
    private String spaceId;

    @Builder.Default
    private SpaceChatEventType type = SpaceChatEventType.TEXT_MESSAGE;

    @Field("sender_id")
    private String senderId;

    @Field("sender_username")
    private String senderUsername;

    @Field("sender_avatar_url")
    private String senderAvatarUrl;

    @Field("sender_role")
    private String senderRole;

    private String content;

    @Builder.Default
    private List<SpaceMessageAttachment> attachments = new ArrayList<>();

    @Builder.Default
    private Map<String, Set<String>> reactions = new HashMap<>();

    @Builder.Default
    private boolean deleted = false;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;
}
