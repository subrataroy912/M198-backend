package com.M198.Majorproject.core.message.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceChatHistoryResponse {
    private String spaceId;
    private List<SpaceChatEventDto> items;
    private String nextCursor;
    private boolean hasMore;
    private Instant joinedAt;
}
