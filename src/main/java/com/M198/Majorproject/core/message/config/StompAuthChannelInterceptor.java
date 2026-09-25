package com.M198.Majorproject.core.message.config;

import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.M198.Majorproject.common.security.AppUserDetailsService;
import com.M198.Majorproject.common.security.JwtService;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final CourseMembershipRepository membershipRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Missing or invalid Authorization header on STOMP CONNECT");
            }

            try {
                String token = authHeader.substring(7).trim();
                Claims claims = jwtService.parseAndValidate(token, "access");
                String userId = claims.getSubject();
                var userDetails = userDetailsService.loadById(userId);
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);
            } catch (Exception ex) {
                log.warn("STOMP CONNECT authentication failed: {}", ex.getMessage());
                throw new MessageDeliveryException("STOMP authentication failed");
            }
        } else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            Principal user = accessor.getUser();
            if (user == null || user.getName() == null) {
                throw new MessageDeliveryException("Unauthenticated STOMP session");
            }

            String destination = accessor.getDestination();
            String targetUserId = extractUserIdFromDestination(destination);
            if (targetUserId != null && !targetUserId.equals(user.getName())) {
                throw new MessageDeliveryException("Cannot subscribe to another user's private topic");
            }
            String spaceId = extractSpaceIdFromDestination(destination);
            if (spaceId != null) {
                boolean isMember = membershipRepository.existsByCourseIdAndUserIdAndStatus(
                        spaceId, user.getName(), MembershipStatus.ACTIVE);
                if (!isMember) {
                    throw new MessageDeliveryException("Active space membership required for chat room " + spaceId);
                }
            }
        }

        return message;
    }

    private String extractSpaceIdFromDestination(String destination) {
        if (destination == null) {
            return null;
        }
        // Matches /topic/spaces/{spaceId} or /app/spaces/{spaceId}.send
        if (destination.startsWith("/topic/spaces/")) {
            String rest = destination.substring("/topic/spaces/".length());
            int slashIdx = rest.indexOf('/');
            return slashIdx > 0 ? rest.substring(0, slashIdx) : rest;
        }
        if (destination.startsWith("/app/spaces/")) {
            String rest = destination.substring("/app/spaces/".length());
            int dotIdx = rest.indexOf('.');
            int slashIdx = rest.indexOf('/');
            int endIdx = dotIdx > 0 ? dotIdx : slashIdx;
            return endIdx > 0 ? rest.substring(0, endIdx) : rest;
        }
        return null;
    }

    private String extractUserIdFromDestination(String destination) {
        if (destination == null) {
            return null;
        }
        if (destination.startsWith("/topic/users/")) {
            String rest = destination.substring("/topic/users/".length());
            int slashIdx = rest.indexOf('/');
            return slashIdx > 0 ? rest.substring(0, slashIdx) : rest;
        }
        return null;
    }
}
