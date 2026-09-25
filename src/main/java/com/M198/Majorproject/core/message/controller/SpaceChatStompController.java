package com.M198.Majorproject.core.message.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.M198.Majorproject.core.message.dto.SendSpaceMessageRequest;
import com.M198.Majorproject.core.message.dto.SpaceMessageReactionRequest;
import com.M198.Majorproject.core.message.dto.SpaceTypingStatusRequest;
import com.M198.Majorproject.core.message.service.SpaceChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SpaceChatStompController {

    private final SpaceChatService spaceChatService;

    @MessageMapping({ "/spaces/{spaceId}.send", "/spaces/{spaceId}/send" })
    public void handleSendMessage(
            @DestinationVariable String spaceId,
            @Payload SendSpaceMessageRequest request,
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        spaceChatService.sendMessage(spaceId, principal.getName(), request);
    }

    @MessageMapping({ "/spaces/{spaceId}.reaction", "/spaces/{spaceId}/reaction" })
    public void handleReaction(
            @DestinationVariable String spaceId,
            @Payload SpaceMessageReactionRequest request,
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        spaceChatService.toggleReaction(spaceId, principal.getName(), request);
    }

    @MessageMapping({ "/spaces/{spaceId}.typing", "/spaces/{spaceId}/typing" })
    public void handleTyping(
            @DestinationVariable String spaceId,
            @Payload SpaceTypingStatusRequest request,
            Principal principal) {
        if (principal == null || principal.getName() == null) {
            return;
        }
        spaceChatService.broadcastTypingStatus(
                spaceId,
                principal.getName(),
                request != null && request.isTyping());
    }
}
