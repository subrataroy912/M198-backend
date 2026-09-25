package com.M198.Majorproject.core.message.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceChatRoomDto {
    private String spaceId;
    private String title;
    private String section;
    private String subject;
    private String logoUrl;
    private String theme;
    private String myRole;
    private long memberCount;
    private long unreadCount;
    private String lastMessageText;
    private String lastMessageSender;
    private Instant lastMessageAt;
    private Instant joinedAt;
}
