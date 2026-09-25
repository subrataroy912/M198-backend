package com.M198.Majorproject.core.message.dto;

import java.util.List;

import com.M198.Majorproject.core.message.entity.SpaceMessageAttachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendSpaceMessageRequest {
    private String content;
    private List<SpaceMessageAttachment> attachments;
}
