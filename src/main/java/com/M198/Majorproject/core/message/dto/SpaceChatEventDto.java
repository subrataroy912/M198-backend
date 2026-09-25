package com.M198.Majorproject.core.message.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.M198.Majorproject.core.message.entity.SpaceChatEventType;
import com.M198.Majorproject.core.message.entity.SpaceMessageAttachment;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpaceChatEventDto {
    private String eventId;
    private SpaceChatEventType type;
    private String spaceId;

    // For TEXT_MESSAGE / MEDIA_MESSAGE / USER_JOINED / USER_LEFT
    private SenderInfo sender;
    private String content;
    private List<SpaceMessageAttachment> attachments;
    private Map<String, Set<String>> reactions;

    // For REACTION_ADDED / REACTION_REMOVED / MESSAGE_DELETED
    private String targetMessageId;
    private String senderId;
    private String emoji;

    // For TYPING_STATUS
    private String userId;
    private String username;
    private Boolean isTyping;

    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        private String userId;
        private String username;
        private String avatarUrl;
        private String role;
    }
}
