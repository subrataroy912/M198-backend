package com.M198.Majorproject.core.message.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import com.M198.Majorproject.core.course.service.CourseService;
import com.M198.Majorproject.core.message.dto.SendSpaceMessageRequest;
import com.M198.Majorproject.core.message.dto.SpaceChatEventDto;
import com.M198.Majorproject.core.message.dto.SpaceChatHistoryResponse;
import com.M198.Majorproject.core.message.dto.SpaceChatRoomDto;
import com.M198.Majorproject.core.message.dto.SpaceMessageReactionRequest;
import com.M198.Majorproject.core.message.service.SpaceChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/messages/spaces")
@RequiredArgsConstructor
public class SpaceChatRestController {

    private final SpaceChatService spaceChatService;
    private final CourseAccessPolicy accessPolicy;

    @GetMapping
    public ResponseEntity<List<SpaceChatRoomDto>> listSpaceRooms(Authentication authentication) {
        return ResponseEntity.ok(spaceChatService.listMySpaceRooms(authentication));
    }

    @GetMapping("/{spaceId}")
    public ResponseEntity<SpaceChatHistoryResponse> getSpaceChatHistory(
            @PathVariable String spaceId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "30") int limit,
            Authentication authentication) {
        return ResponseEntity.ok(spaceChatService.getHistory(spaceId, before, limit, authentication));
    }

    @PostMapping("/{spaceId}")
    public ResponseEntity<SpaceChatEventDto> sendSpaceMessage(
            @PathVariable String spaceId,
            @RequestBody SendSpaceMessageRequest request,
            Authentication authentication) {
        String userId = accessPolicy.authenticatedUserId(
                authentication,
                () -> new CourseService.CourseAccessException("Authentication required"));
        return ResponseEntity.ok(spaceChatService.sendMessage(spaceId, userId, request));
    }

    @PostMapping("/{spaceId}/reactions")
    public ResponseEntity<SpaceChatEventDto> toggleReaction(
            @PathVariable String spaceId,
            @RequestBody SpaceMessageReactionRequest request,
            Authentication authentication) {
        String userId = accessPolicy.authenticatedUserId(
                authentication,
                () -> new CourseService.CourseAccessException("Authentication required"));
        return ResponseEntity.ok(spaceChatService.toggleReaction(spaceId, userId, request));
    }

    @PostMapping("/{spaceId}/read")
    public ResponseEntity<Void> markSpaceChatRead(
            @PathVariable String spaceId,
            Authentication authentication) {
        spaceChatService.markRead(spaceId, authentication);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{spaceId}/{messageId}")
    public ResponseEntity<Void> deleteSpaceMessage(
            @PathVariable String spaceId,
            @PathVariable String messageId,
            Authentication authentication) {
        spaceChatService.deleteMessage(spaceId, messageId, authentication);
        return ResponseEntity.noContent().build();
    }
}
