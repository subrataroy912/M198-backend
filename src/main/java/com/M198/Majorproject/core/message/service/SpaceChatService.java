package com.M198.Majorproject.core.message.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import com.M198.Majorproject.core.course.service.CourseService;
import com.M198.Majorproject.core.message.dto.SendSpaceMessageRequest;
import com.M198.Majorproject.core.message.dto.SpaceChatEventDto;
import com.M198.Majorproject.core.message.dto.SpaceChatHistoryResponse;
import com.M198.Majorproject.core.message.dto.SpaceChatRoomDto;
import com.M198.Majorproject.core.message.dto.SpaceMessageReactionRequest;
import com.M198.Majorproject.core.message.entity.SpaceChatEventType;
import com.M198.Majorproject.core.message.entity.SpaceChatReadState;
import com.M198.Majorproject.core.message.entity.SpaceMessage;
import com.M198.Majorproject.core.message.entity.SpaceMessageAttachment;
import com.M198.Majorproject.core.message.repository.SpaceChatReadStateRepository;
import com.M198.Majorproject.core.message.repository.SpaceMessageRepository;
import com.M198.Majorproject.user.profile.entity.UserProfile;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import com.M198.Majorproject.user.profile.service.MediaStorageService;

@Service
@RequiredArgsConstructor
public class SpaceChatService {

        private static final long MIN_SEND_INTERVAL_MS = 800L;

        private final SpaceMessageRepository messageRepository;
        private final SpaceChatReadStateRepository readStateRepository;
        private final CourseRepository courseRepository;
        private final CourseMembershipRepository membershipRepository;
        private final CourseProfilePort profilePort;
        private final CourseAccessPolicy accessPolicy;
        private final SimpMessagingTemplate messagingTemplate;
        private final java.util.concurrent.ConcurrentHashMap<String, Long> lastSentAtByUser = new java.util.concurrent.ConcurrentHashMap<>();

        @Autowired(required = false)
        private MediaStorageService mediaStorageService;

        @Autowired(required = false)
        private com.M198.Majorproject.common.presence.UserPresenceService userPresenceService;

        public List<SpaceChatRoomDto> listMySpaceRooms(Authentication authentication) {
                String userId = accessPolicy.authenticatedUserId(
                                authentication,
                                () -> new CourseService.CourseAccessException("Authentication required"));

                if (userPresenceService != null) {
                        userPresenceService.markUserActive(userId);
                }

                List<CourseMembership> memberships = membershipRepository.findAllByUserIdAndStatus(
                                userId, MembershipStatus.ACTIVE);
                if (memberships.isEmpty()) {
                        return Collections.emptyList();
                }

                Set<String> courseIds = memberships.stream()
                                .map(CourseMembership::getCourseId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                List<Course> activeCourses = courseRepository.findAllByIdInAndStatus(courseIds, CourseStatus.ACTIVE);
                if (activeCourses.isEmpty()) {
                        return Collections.emptyList();
                }

                Map<String, Course> courseMap = activeCourses.stream()
                                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a));

                Set<String> activeIds = courseMap.keySet();

                List<CourseMembership> allRoomMemberships = membershipRepository
                                .findAllByCourseIdInAndStatus(activeIds, MembershipStatus.ACTIVE);

                Map<String, Long> memberCounts = allRoomMemberships.stream()
                                .collect(Collectors.groupingBy(CourseMembership::getCourseId, Collectors.counting()));

                Map<String, Set<String>> memberIdsBySpace = allRoomMemberships.stream()
                                .filter(m -> m.getCourseId() != null && m.getUserId() != null)
                                .collect(Collectors.groupingBy(
                                                CourseMembership::getCourseId,
                                                Collectors.mapping(CourseMembership::getUserId, Collectors.toSet())));

                Map<String, Instant> lastReadMap = readStateRepository
                                .findAllByUserIdAndSpaceIdIn(userId, activeIds)
                                .stream()
                                .filter(s -> s.getSpaceId() != null && s.getLastReadAt() != null)
                                .collect(Collectors.toMap(
                                                SpaceChatReadState::getSpaceId,
                                                SpaceChatReadState::getLastReadAt,
                                                (a, b) -> a.isAfter(b) ? a : b));

                List<SpaceChatRoomDto> rooms = new ArrayList<>();
                for (CourseMembership membership : memberships) {
                        Course course = courseMap.get(membership.getCourseId());
                        if (course == null) {
                                continue;
                        }
                        String spaceId = course.getId();
                        Instant cutoff = resolveHistoryCutoff(membership);

                        var latestMsgOpt = (cutoff != null)
                                        ? messageRepository
                                                        .findFirstBySpaceIdAndDeletedFalseAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                                                                        spaceId, cutoff)
                                        : messageRepository
                                                        .findFirstBySpaceIdAndDeletedFalseOrderByCreatedAtDesc(spaceId);

                        Instant lastReadAt = lastReadMap.get(spaceId);
                        Instant effectiveReadFloor = lastReadAt;
                        if (cutoff != null) {
                                effectiveReadFloor = (lastReadAt != null && lastReadAt.isAfter(cutoff))
                                                ? lastReadAt
                                                : cutoff;
                        }

                        long unreadCount;
                        if (effectiveReadFloor != null) {
                                unreadCount = messageRepository
                                                .countBySpaceIdAndDeletedFalseAndCreatedAtAfterAndSenderIdNot(
                                                                spaceId, effectiveReadFloor, userId);
                        } else {
                                unreadCount = messageRepository.countBySpaceIdAndDeletedFalseAndSenderIdNot(spaceId,
                                                userId);
                        }

                        SpaceMessage latestMsg = latestMsgOpt.orElse(null);
                        String lastText = latestMsg != null
                                        ? (latestMsg.getContent() != null && !latestMsg.getContent().isBlank()
                                                        ? latestMsg.getContent()
                                                        : "Shared an attachment")
                                        : null;

                        Set<String> spaceMemberIds = memberIdsBySpace.getOrDefault(spaceId, Set.of(userId));
                        Set<String> onlineIds = new java.util.HashSet<>(
                                        userPresenceService != null
                                                        ? userPresenceService.getOnlineUserIds(spaceMemberIds)
                                                        : Collections.emptySet());
                        onlineIds.add(userId);

                        rooms.add(SpaceChatRoomDto.builder()
                                        .spaceId(spaceId)
                                        .title(course.getTitle())
                                        .section(course.getSection())
                                        .subject(course.getSubject())
                                        .logoUrl(course.getLogoUrl())
                                        .theme(course.getTheme())
                                        .myRole(membership.getRole() != null ? membership.getRole().name()
                                                        : MembershipRole.MEMBER.name())
                                        .memberCount(memberCounts.getOrDefault(spaceId, 1L))
                                        .onlineCount(onlineIds.size())
                                        .onlineUserIds(onlineIds)
                                        .unreadCount(unreadCount)
                                        .lastMessageText(lastText)
                                        .lastMessageSender(latestMsg != null ? latestMsg.getSenderUsername() : null)
                                        .lastMessageAt(latestMsg != null
                                                        ? latestMsg.getCreatedAt()
                                                        : (cutoff != null ? cutoff : course.getUpdatedAt()))
                                        .joinedAt(cutoff)
                                        .build());
                }

                rooms.sort(Comparator.comparing(
                                (SpaceChatRoomDto r) -> r.getLastMessageAt() != null ? r.getLastMessageAt()
                                                : Instant.EPOCH)
                                .reversed());
                return rooms;
        }

        public SpaceChatHistoryResponse getHistory(
                        String spaceId,
                        String beforeCursor,
                        int limit,
                        Authentication authentication) {
                String userId = accessPolicy.authenticatedUserId(
                                authentication,
                                () -> new CourseService.CourseAccessException("Authentication required"));

                CourseMembership membership = requireActiveSpaceAndMembership(spaceId, userId);
                Instant cutoff = resolveHistoryCutoff(membership);

                int safeLimit = Math.max(1, Math.min(limit, 50));
                PageRequest pageable = PageRequest.of(0, safeLimit + 1);

                List<SpaceMessage> fetched;
                boolean hasCursor = beforeCursor != null && !beforeCursor.isBlank();
                if (cutoff != null) {
                        fetched = hasCursor
                                        ? messageRepository
                                                        .findBySpaceIdAndDeletedFalseAndIdLessThanAndCreatedAtGreaterThanEqualOrderByIdDesc(
                                                                        spaceId, beforeCursor.trim(), cutoff, pageable)
                                        : messageRepository
                                                        .findBySpaceIdAndDeletedFalseAndCreatedAtGreaterThanEqualOrderByIdDesc(
                                                                        spaceId, cutoff, pageable);
                } else {
                        fetched = hasCursor
                                        ? messageRepository.findBySpaceIdAndDeletedFalseAndIdLessThanOrderByIdDesc(
                                                        spaceId, beforeCursor.trim(), pageable)
                                        : messageRepository.findBySpaceIdAndDeletedFalseOrderByIdDesc(spaceId,
                                                        pageable);
                }

                boolean hasMore = fetched.size() > safeLimit;
                List<SpaceMessage> slice = hasMore ? new ArrayList<>(fetched.subList(0, safeLimit))
                                : new ArrayList<>(fetched);

                // Reverse so chronological order (oldest -> newest) is returned to the UI
                Collections.reverse(slice);

                String nextCursor = !slice.isEmpty() ? slice.get(0).getId() : null;
                List<SpaceChatEventDto> items = slice.stream()
                                .map(this::toEventDto)
                                .toList();

                // Automatically update read cursor when loading latest history
                if (!hasCursor) {
                        updateReadCursor(spaceId, userId);
                }

                return SpaceChatHistoryResponse.builder()
                                .spaceId(spaceId)
                                .items(items)
                                .nextCursor(nextCursor)
                                .hasMore(hasMore)
                                .joinedAt(cutoff)
                                .build();
        }

        public SpaceMessageAttachment uploadChatImage(
                        String spaceId,
                        String userId,
                        MultipartFile file) {
                requireActiveSpaceAndMembership(spaceId, userId);
                if (file == null || file.isEmpty()) {
                        throw new CourseService.CourseBadRequestException("Image file is required");
                }
                String contentType = file.getContentType() != null ? file.getContentType() : "image/webp";
                if (!contentType.startsWith("image/")) {
                        throw new CourseService.CourseBadRequestException("Only image files are supported");
                }
                String uploadedUrl;
                if (mediaStorageService != null) {
                        uploadedUrl = mediaStorageService.uploadImage(file, "space_chat");
                } else {
                        throw new CourseService.CourseBadRequestException("Media storage is unavailable");
                }
                return SpaceMessageAttachment.builder()
                                .attachmentId("att_" + UUID.randomUUID().toString().substring(0, 8))
                                .name(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.webp")
                                .type("IMAGE")
                                .mimeType(contentType)
                                .url(uploadedUrl)
                                .sizeBytes(file.getSize())
                                .build();
        }

        public SpaceChatEventDto sendMessage(
                        String spaceId,
                        String userId,
                        SendSpaceMessageRequest request) {
                CourseMembership membership = requireActiveSpaceAndMembership(spaceId, userId);

                String rateKey = spaceId + ":" + userId;
                long nowMs = System.currentTimeMillis();
                Long previousSentMs = lastSentAtByUser.get(rateKey);
                if (previousSentMs != null && (nowMs - previousSentMs) < MIN_SEND_INTERVAL_MS) {
                        throw new CourseService.CourseBadRequestException(
                                        "Please wait a moment before sending another message");
                }
                lastSentAtByUser.put(rateKey, nowMs);

                String rawContent = request != null && request.getContent() != null ? request.getContent() : "";
                String content = rawContent
                                .replace("\r\n", "\n")
                                .replaceAll("\n{3,}", "\n\n")
                                .trim();
                if (content.length() > 2000) {
                        content = content.substring(0, 2000).trim();
                }
                List<SpaceMessageAttachment> attachments = request != null && request.getAttachments() != null
                                ? request.getAttachments()
                                : Collections.emptyList();

                if (content.isEmpty() && attachments.isEmpty()) {
                        throw new CourseService.CourseBadRequestException("Message content or attachment is required");
                }

                UserProfile profile = profilePort != null
                                ? profilePort.findByUserId(userId).orElse(null)
                                : null;
                String username = resolveUsername(profile, userId);
                String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
                String roleName = membership.getRole() != null ? membership.getRole().name()
                                : MembershipRole.MEMBER.name();

                SpaceChatEventType eventType = !attachments.isEmpty()
                                ? SpaceChatEventType.MEDIA_MESSAGE
                                : SpaceChatEventType.TEXT_MESSAGE;

                List<SpaceMessageAttachment> normalizedAttachments = attachments.stream()
                                .map(att -> {
                                        String rawUrl = att.getUrl() != null ? att.getUrl().trim() : "";
                                        String resolvedUrl = rawUrl;
                                        if (rawUrl.startsWith("data:image/") && mediaStorageService != null) {
                                                resolvedUrl = mediaStorageService.uploadImage(rawUrl, "space_chat");
                                        }
                                        boolean isImage = "IMAGE".equalsIgnoreCase(att.getType())
                                                        || (att.getMimeType() != null && att.getMimeType().toLowerCase()
                                                                        .startsWith("image/"))
                                                        || rawUrl.startsWith("data:image/")
                                                        || resolvedUrl.matches(
                                                                        "(?i).*\\.(png|jpe?g|gif|webp|svg)(\\?.*)?$");
                                        String resolvedType = isImage ? "IMAGE"
                                                        : (att.getType() != null ? att.getType() : "LINK");
                                        String resolvedMime = att.getMimeType() != null && !att.getMimeType().isBlank()
                                                        ? att.getMimeType()
                                                        : (isImage ? "image/webp" : "application/octet-stream");
                                        return SpaceMessageAttachment.builder()
                                                        .attachmentId(att.getAttachmentId() != null
                                                                        ? att.getAttachmentId()
                                                                        : "att_" + UUID.randomUUID().toString()
                                                                                        .substring(0, 8))
                                                        .name(att.getName() != null && !att.getName().isBlank()
                                                                        ? att.getName()
                                                                        : "Attachment")
                                                        .type(resolvedType)
                                                        .mimeType(resolvedMime)
                                                        .url(resolvedUrl)
                                                        .sizeBytes(att.getSizeBytes())
                                                        .build();
                                })
                                .toList();

                SpaceMessage saved = messageRepository.save(SpaceMessage.builder()
                                .spaceId(spaceId)
                                .type(eventType)
                                .senderId(userId)
                                .senderUsername(username)
                                .senderAvatarUrl(avatarUrl)
                                .senderRole(roleName)
                                .content(content)
                                .attachments(normalizedAttachments)
                                .reactions(new HashMap<>())
                                .deleted(false)
                                .createdAt(Instant.now())
                                .build());

                updateReadCursor(spaceId, userId);

                SpaceChatEventDto eventDto = toEventDto(saved);
                messagingTemplate.convertAndSend("/topic/spaces/" + spaceId, eventDto);
                return eventDto;
        }

        public SpaceChatEventDto toggleReaction(
                        String spaceId,
                        String userId,
                        SpaceMessageReactionRequest request) {
                CourseMembership membership = requireActiveSpaceAndMembership(spaceId, userId);
                Instant cutoff = resolveHistoryCutoff(membership);

                if (request == null || request.getTargetMessageId() == null || request.getEmoji() == null
                                || request.getEmoji().isBlank()) {
                        throw new CourseService.CourseBadRequestException("targetMessageId and emoji are required");
                }

                SpaceMessage message = messageRepository.findById(request.getTargetMessageId())
                                .filter(m -> !m.isDeleted() && spaceId.equals(m.getSpaceId()))
                                .filter(m -> cutoff == null
                                                || (m.getCreatedAt() != null && !m.getCreatedAt().isBefore(cutoff)))
                                .orElseThrow(CourseService.CourseNotFoundException::new);

                Map<String, Set<String>> reactions = message.getReactions();
                if (reactions == null) {
                        reactions = new HashMap<>();
                        message.setReactions(reactions);
                }

                String emoji = request.getEmoji().trim();
                Set<String> users = reactions.computeIfAbsent(emoji, k -> new HashSet<>());
                boolean added;
                if (users.contains(userId)) {
                        users.remove(userId);
                        if (users.isEmpty()) {
                                reactions.remove(emoji);
                        }
                        added = false;
                } else {
                        users.add(userId);
                        added = true;
                }

                messageRepository.save(message);

                SpaceChatEventDto patchEvent = SpaceChatEventDto.builder()
                                .eventId("evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                                .type(added ? SpaceChatEventType.REACTION_ADDED : SpaceChatEventType.REACTION_REMOVED)
                                .spaceId(spaceId)
                                .targetMessageId(message.getId())
                                .senderId(userId)
                                .emoji(emoji)
                                .reactions(message.getReactions())
                                .timestamp(Instant.now())
                                .build();

                messagingTemplate.convertAndSend("/topic/spaces/" + spaceId, patchEvent);
                return patchEvent;
        }

        public void broadcastTypingStatus(String spaceId, String userId, boolean isTyping) {
                // Verify membership without writing to MongoDB
                boolean isMember = membershipRepository.existsByCourseIdAndUserIdAndStatus(
                                spaceId, userId, MembershipStatus.ACTIVE);
                if (!isMember) {
                        return;
                }

                UserProfile profile = profilePort != null
                                ? profilePort.findByUserId(userId).orElse(null)
                                : null;
                String username = resolveUsername(profile, userId);

                SpaceChatEventDto typingEvent = SpaceChatEventDto.builder()
                                .type(SpaceChatEventType.TYPING_STATUS)
                                .spaceId(spaceId)
                                .userId(userId)
                                .username(username)
                                .isTyping(isTyping)
                                .timestamp(Instant.now())
                                .build();

                messagingTemplate.convertAndSend("/topic/spaces/" + spaceId, typingEvent);
        }

        public void deleteMessage(String spaceId, String messageId, Authentication authentication) {
                String userId = accessPolicy.authenticatedUserId(
                                authentication,
                                () -> new CourseService.CourseAccessException("Authentication required"));

                CourseMembership membership = requireActiveSpaceAndMembership(spaceId, userId);
                SpaceMessage message = messageRepository.findById(messageId)
                                .filter(m -> !m.isDeleted() && spaceId.equals(m.getSpaceId()))
                                .orElseThrow(CourseService.CourseNotFoundException::new);

                boolean isStaff = membership.getRole() == MembershipRole.OWNER
                                || membership.getRole() == MembershipRole.ADMIN;
                boolean isAuthor = userId.equals(message.getSenderId());

                if (!isStaff && !isAuthor) {
                        throw new CourseService.CourseAccessException(
                                        "Only space staff or the message author can delete this message");
                }

                message.setDeleted(true);
                messageRepository.save(message);

                SpaceChatEventDto deletedEvent = SpaceChatEventDto.builder()
                                .eventId("evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                                .type(SpaceChatEventType.MESSAGE_DELETED)
                                .spaceId(spaceId)
                                .targetMessageId(messageId)
                                .senderId(userId)
                                .timestamp(Instant.now())
                                .build();

                messagingTemplate.convertAndSend("/topic/spaces/" + spaceId, deletedEvent);
        }

        public void markRead(String spaceId, Authentication authentication) {
                String userId = accessPolicy.authenticatedUserId(
                                authentication,
                                () -> new CourseService.CourseAccessException("Authentication required"));
                requireActiveSpaceAndMembership(spaceId, userId);
                updateReadCursor(spaceId, userId);
        }

        private void updateReadCursor(String spaceId, String userId) {
                String deterministicId = spaceId + ":" + userId;
                SpaceChatReadState state;
                try {
                        state = readStateRepository.findBySpaceIdAndUserId(spaceId, userId)
                                        .orElseGet(() -> SpaceChatReadState.builder()
                                                        .id(deterministicId)
                                                        .spaceId(spaceId)
                                                        .userId(userId)
                                                        .build());
                } catch (RuntimeException ex) {
                        state = SpaceChatReadState.builder()
                                        .id(deterministicId)
                                        .spaceId(spaceId)
                                        .userId(userId)
                                        .build();
                }
                if (state.getId() == null) {
                        state.setId(deterministicId);
                }
                state.setLastReadAt(Instant.now());
                readStateRepository.save(state);
        }

        private Instant resolveHistoryCutoff(CourseMembership membership) {
                if (membership == null) {
                        return Instant.now();
                }
                MembershipRole role = membership.getRole();
                if (role == MembershipRole.OWNER || role == MembershipRole.ADMIN) {
                        return null;
                }
                if (membership.getJoinedAt() != null) {
                        return membership.getJoinedAt();
                }
                return membership.getCreatedAt();
        }

        private CourseMembership requireActiveSpaceAndMembership(String spaceId, String userId) {
                courseRepository.findByIdAndStatus(spaceId, CourseStatus.ACTIVE)
                                .orElseThrow(CourseService.CourseNotFoundException::new);
                return membershipRepository.findByCourseIdAndUserIdAndStatus(spaceId, userId, MembershipStatus.ACTIVE)
                                .orElseThrow(() -> new CourseService.CourseAccessException(
                                                "Active membership is required to access this space chat"));
        }

        private String resolveUsername(UserProfile profile, String userId) {
                if (profile != null) {
                        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
                                return profile.getDisplayName();
                        }
                        if (profile.getHandle() != null && !profile.getHandle().isBlank()) {
                                return profile.getHandle();
                        }
                }
                return "Member";
        }

        public void broadcastSpacePresence(String spaceId, String triggerUserId, boolean isOnline) {
                if (spaceId == null || spaceId.isBlank()) {
                        return;
                }
                List<CourseMembership> memberships = membershipRepository.findAllByCourseIdAndStatus(
                                spaceId, MembershipStatus.ACTIVE);
                Set<String> memberIds = memberships.stream()
                                .map(CourseMembership::getUserId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                Set<String> onlineUserIds = userPresenceService != null
                                ? userPresenceService.getOnlineUserIds(memberIds)
                                : Collections.emptySet();

                SpaceChatEventDto event = SpaceChatEventDto.builder()
                                .type(SpaceChatEventType.PRESENCE_UPDATE)
                                .spaceId(spaceId)
                                .userId(triggerUserId)
                                .online(isOnline)
                                .lastActiveAt(userPresenceService != null
                                                ? userPresenceService.getLastActiveAt(triggerUserId, Instant.now())
                                                : Instant.now())
                                .onlineCount((long) onlineUserIds.size())
                                .onlineUserIds(onlineUserIds)
                                .timestamp(Instant.now())
                                .build();
                messagingTemplate.convertAndSend("/topic/spaces/" + spaceId, event);
        }

        private SpaceChatEventDto toEventDto(SpaceMessage msg) {
                String senderId = msg.getSenderId();
                boolean senderOnline = userPresenceService != null && userPresenceService.isOnline(senderId);
                Instant senderLastActive = userPresenceService != null
                                ? userPresenceService.getLastActiveAt(senderId, msg.getCreatedAt())
                                : msg.getCreatedAt();

                return SpaceChatEventDto.builder()
                                .eventId(msg.getId())
                                .type(msg.getType() != null ? msg.getType() : SpaceChatEventType.TEXT_MESSAGE)
                                .spaceId(msg.getSpaceId())
                                .sender(SpaceChatEventDto.SenderInfo.builder()
                                                .userId(senderId)
                                                .username(msg.getSenderUsername())
                                                .avatarUrl(msg.getSenderAvatarUrl())
                                                .role(msg.getSenderRole())
                                                .online(senderOnline)
                                                .lastActiveAt(senderLastActive)
                                                .build())
                                .content(msg.getContent())
                                .attachments(msg.getAttachments())
                                .reactions(msg.getReactions() != null ? msg.getReactions() : Collections.emptyMap())
                                .timestamp(msg.getCreatedAt())
                                .build();
        }
}
