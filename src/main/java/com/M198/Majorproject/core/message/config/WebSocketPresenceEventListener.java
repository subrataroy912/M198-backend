package com.M198.Majorproject.core.message.config;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.M198.Majorproject.common.presence.UserPresenceService;
import com.M198.Majorproject.core.message.service.SpaceChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceEventListener {

    private final UserPresenceService userPresenceService;
    private final SpaceChatService spaceChatService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        if (sessionId != null && user != null && user.getName() != null) {
            userPresenceService.registerWebSocketSession(sessionId, user.getName());
        }
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (sessionId == null || user == null || user.getName() == null || destination == null) {
            return;
        }
        String userId = user.getName();
        if (destination.startsWith("/topic/spaces/")) {
            String rest = destination.substring("/topic/spaces/".length());
            int slashIdx = rest.indexOf('/');
            String spaceId = slashIdx > 0 ? rest.substring(0, slashIdx) : rest;
            if (!spaceId.isBlank()) {
                userPresenceService.registerSpaceSubscription(sessionId, userId, spaceId);
                spaceChatService.broadcastSpacePresence(spaceId, userId, true);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }
        UserPresenceService.DisconnectInfo disconnectInfo = userPresenceService.unregisterWebSocketSession(sessionId);
        if (disconnectInfo == null || disconnectInfo.userId() == null) {
            return;
        }
        String userId = disconnectInfo.userId();
        boolean stillOnline = userPresenceService.isOnline(userId);
        for (String spaceId : disconnectInfo.spaceIds()) {
            spaceChatService.broadcastSpacePresence(spaceId, userId, stillOnline);
        }
    }
}
