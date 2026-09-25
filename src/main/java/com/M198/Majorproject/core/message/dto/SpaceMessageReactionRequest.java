package com.M198.Majorproject.core.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceMessageReactionRequest {
    private String targetMessageId;
    private String emoji;
}
