package com.M198.Majorproject.core.message.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.port.CourseProfilePort;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import com.M198.Majorproject.core.course.security.CourseMembershipResolver;
import com.M198.Majorproject.core.course.service.CourseService;
import com.M198.Majorproject.core.message.dto.SendSpaceMessageRequest;
import com.M198.Majorproject.core.message.dto.SpaceChatEventDto;
import com.M198.Majorproject.core.message.dto.SpaceMessageReactionRequest;
import com.M198.Majorproject.core.message.entity.SpaceChatEventType;
import com.M198.Majorproject.core.message.entity.SpaceChatReadState;
import com.M198.Majorproject.core.message.entity.SpaceMessage;
import com.M198.Majorproject.core.message.repository.SpaceChatReadStateRepository;
import com.M198.Majorproject.core.message.repository.SpaceMessageRepository;
import com.M198.Majorproject.user.profile.entity.UserProfile;

class SpaceChatServiceTest {

        private final SpaceMessageRepository messageRepository = mock(SpaceMessageRepository.class);
        private final SpaceChatReadStateRepository readStateRepository = mock(SpaceChatReadStateRepository.class);
        private final CourseRepository courseRepository = mock(CourseRepository.class);
        private final CourseMembershipRepository membershipRepository = mock(CourseMembershipRepository.class);
        private final CourseProfilePort profilePort = mock(CourseProfilePort.class);
        private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        private final CourseAccessPolicy accessPolicy = new CourseAccessPolicy(
                        new CourseMembershipResolver(membershipRepository));

        private final SpaceChatService spaceChatService = new SpaceChatService(
                        messageRepository,
                        readStateRepository,
                        courseRepository,
                        membershipRepository,
                        profilePort,
                        accessPolicy,
                        messagingTemplate);

        private final Authentication memberAuth = mock(Authentication.class);
        private final Authentication ownerAuth = mock(Authentication.class);

        @BeforeEach
        void setUp() {
                when(memberAuth.isAuthenticated()).thenReturn(true);
                when(memberAuth.getName()).thenReturn("user-member");
                when(ownerAuth.isAuthenticated()).thenReturn(true);
                when(ownerAuth.getName()).thenReturn("user-owner");

                Course activeCourse = Course.builder()
                                .id("space-1")
                                .title("DSA Lab")
                                .status(CourseStatus.ACTIVE)
                                .build();
                when(courseRepository.findByIdAndStatus("space-1", CourseStatus.ACTIVE))
                                .thenReturn(Optional.of(activeCourse));

                when(membershipRepository.findByCourseIdAndUserIdAndStatus("space-1", "user-member",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(CourseMembership.builder()
                                                .courseId("space-1")
                                                .userId("user-member")
                                                .role(MembershipRole.MEMBER)
                                                .status(MembershipStatus.ACTIVE)
                                                .build()));

                when(membershipRepository.findByCourseIdAndUserIdAndStatus("space-1", "user-owner",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(CourseMembership.builder()
                                                .courseId("space-1")
                                                .userId("user-owner")
                                                .role(MembershipRole.OWNER)
                                                .status(MembershipStatus.ACTIVE)
                                                .build()));

                when(membershipRepository.existsByCourseIdAndUserIdAndStatus("space-1", "user-member",
                                MembershipStatus.ACTIVE))
                                .thenReturn(true);

                when(profilePort.findByUserId("user-member"))
                                .thenReturn(Optional.of(UserProfile.builder()
                                                .userId("user-member")
                                                .displayName("Rohan Das")
                                                .build()));

                when(readStateRepository.findBySpaceIdAndUserId(any(), any()))
                                .thenReturn(Optional.empty());
                when(readStateRepository.save(any(SpaceChatReadState.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void sendMessagePersistsAndBroadcastsToSpaceTopic() {
                when(messageRepository.save(any(SpaceMessage.class))).thenAnswer(inv -> {
                        SpaceMessage msg = inv.getArgument(0);
                        msg.setId("msg-101");
                        return msg;
                });

                SendSpaceMessageRequest request = SendSpaceMessageRequest.builder()
                                .content("Hello DSA Lab!")
                                .build();

                SpaceChatEventDto result = spaceChatService.sendMessage("space-1", "user-member", request);

                assertEquals("msg-101", result.getEventId());
                assertEquals(SpaceChatEventType.TEXT_MESSAGE, result.getType());
                assertEquals("Hello DSA Lab!", result.getContent());
                assertEquals("Rohan Das", result.getSender().getUsername());
                assertEquals("MEMBER", result.getSender().getRole());
                verify(messagingTemplate).convertAndSend(eq("/topic/spaces/space-1"), any(SpaceChatEventDto.class));
        }

        @Test
        void typingStatusBroadcastsDirectlyWithoutTouchingDatabase() {
                spaceChatService.broadcastTypingStatus("space-1", "user-member", true);

                verifyNoInteractions(messageRepository);
                verify(messagingTemplate).convertAndSend(eq("/topic/spaces/space-1"), any(SpaceChatEventDto.class));
        }

        @Test
        void toggleReactionAddsAndRemovesEmojiAndBroadcastsPatch() {
                SpaceMessage existing = SpaceMessage.builder()
                                .id("msg-101")
                                .spaceId("space-1")
                                .senderId("user-owner")
                                .content("Welcome!")
                                .reactions(new HashMap<>())
                                .deleted(false)
                                .createdAt(Instant.now())
                                .build();
                when(messageRepository.findById("msg-101")).thenReturn(Optional.of(existing));
                when(messageRepository.save(any(SpaceMessage.class))).thenAnswer(inv -> inv.getArgument(0));

                SpaceMessageReactionRequest req = new SpaceMessageReactionRequest("msg-101", "🚀");

                // 1st toggle -> REACTION_ADDED
                SpaceChatEventDto addedEvent = spaceChatService.toggleReaction("space-1", "user-member", req);
                assertEquals(SpaceChatEventType.REACTION_ADDED, addedEvent.getType());
                assertTrue(addedEvent.getReactions().get("🚀").contains("user-member"));

                // 2nd toggle -> REACTION_REMOVED
                SpaceChatEventDto removedEvent = spaceChatService.toggleReaction("space-1", "user-member", req);
                assertEquals(SpaceChatEventType.REACTION_REMOVED, removedEvent.getType());
                assertTrue(removedEvent.getReactions().isEmpty());
        }

        @Test
        void moderationAllowsOwnerToDeleteAnyMessageAndBlocksMemberFromDeletingOthers() {
                SpaceMessage otherMemberMsg = SpaceMessage.builder()
                                .id("msg-202")
                                .spaceId("space-1")
                                .senderId("user-other")
                                .content("Test message")
                                .deleted(false)
                                .build();
                when(messageRepository.findById("msg-202")).thenReturn(Optional.of(otherMemberMsg));

                // Regular member cannot delete another user's message
                assertThrows(CourseService.CourseAccessException.class,
                                () -> spaceChatService.deleteMessage("space-1", "msg-202", memberAuth));
                verify(messageRepository, never()).save(any());

                // Space Owner can delete any message in the space
                spaceChatService.deleteMessage("space-1", "msg-202", ownerAuth);
                assertTrue(otherMemberMsg.isDeleted());
                verify(messagingTemplate).convertAndSend(eq("/topic/spaces/space-1"), any(SpaceChatEventDto.class));
        }

        @Test
        void getHistoryEnforcesJoinedAtCutoffForMemberButFullHistoryForOwner() {
                Instant joinedAt = Instant.parse("2026-09-25T03:00:00Z");
                when(membershipRepository.findByCourseIdAndUserIdAndStatus("space-1", "user-member",
                                MembershipStatus.ACTIVE))
                                .thenReturn(Optional.of(CourseMembership.builder()
                                                .courseId("space-1")
                                                .userId("user-member")
                                                .role(MembershipRole.MEMBER)
                                                .status(MembershipStatus.ACTIVE)
                                                .joinedAt(joinedAt)
                                                .build()));

                SpaceMessage postJoinMsg = SpaceMessage.builder()
                                .id("msg-post-join")
                                .spaceId("space-1")
                                .senderId("user-owner")
                                .senderUsername("Prof. Roy")
                                .content("Sent after join")
                                .createdAt(Instant.parse("2026-09-25T03:05:00Z"))
                                .deleted(false)
                                .build();

                SpaceMessage preJoinMsg = SpaceMessage.builder()
                                .id("msg-pre-join")
                                .spaceId("space-1")
                                .senderId("user-owner")
                                .senderUsername("Prof. Roy")
                                .content("Sent before join")
                                .createdAt(Instant.parse("2026-09-25T02:00:00Z"))
                                .deleted(false)
                                .build();

                when(messageRepository.findBySpaceIdAndDeletedFalseAndCreatedAtGreaterThanEqualOrderByIdDesc(
                                eq("space-1"), eq(joinedAt), any()))
                                .thenReturn(java.util.List.of(postJoinMsg));

                when(messageRepository.findBySpaceIdAndDeletedFalseOrderByIdDesc(eq("space-1"), any()))
                                .thenReturn(java.util.List.of(postJoinMsg, preJoinMsg));

                var memberHistory = spaceChatService.getHistory("space-1", null, 30, memberAuth);
                assertEquals(1, memberHistory.getItems().size());
                assertEquals("msg-post-join", memberHistory.getItems().get(0).getEventId());
                assertEquals(joinedAt, memberHistory.getJoinedAt());

                var ownerHistory = spaceChatService.getHistory("space-1", null, 30, ownerAuth);
                assertEquals(2, ownerHistory.getItems().size());
                assertEquals(null, ownerHistory.getJoinedAt());
        }
}
