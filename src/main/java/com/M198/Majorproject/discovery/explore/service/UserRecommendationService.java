package com.M198.Majorproject.discovery.explore.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.discovery.explore.dto.RecommendedUserResponse;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;
import com.M198.Majorproject.user.profile.security.AuthenticatedUserResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRecommendationService {

    public static final String REASON_SHARED_SPACES = "SHARED_SPACES";
    public static final String REASON_MUTUAL_SPACE_PEERS = "MUTUAL_SPACE_PEERS";

    private final UserProfileRepository userProfileRepository;
    private final CourseMembershipRepository courseMembershipRepository;
    private final CourseRepository courseRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public Page<RecommendedUserResponse> getRecommendedUsers(Authentication authentication, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        String currentUserId = authenticatedUserResolver.resolveAuthenticatedUserIdSafe(authentication);
        if (currentUserId == null || currentUserId.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<CourseMembership> myMemberships = courseMembershipRepository
                .findAllByUserIdAndStatus(currentUserId, MembershipStatus.ACTIVE);

        List<String> myCourseIds = myMemberships.stream()
                .map(CourseMembership::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (myCourseIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<ScoredRecommendation> scoredCandidates = getSpaceConnectedRecommendations(currentUserId, myCourseIds);

        int total = scoredCandidates.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        List<RecommendedUserResponse> pageContent = scoredCandidates.subList(fromIndex, toIndex).stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(pageContent, pageable, total);
    }

    private List<ScoredRecommendation> getSpaceConnectedRecommendations(String currentUserId,
            List<String> myCourseIds) {
        Map<String, String> courseTitleMap = new HashMap<>();
        courseRepository.findAllByIdInAndStatus(myCourseIds, CourseStatus.ACTIVE)
                .forEach(c -> courseTitleMap.put(c.getId(), c.getTitle()));

        // 1st-degree: direct space classmates
        List<CourseMembership> directMemberships = courseMembershipRepository
                .findAllByCourseIdInAndStatus(myCourseIds, MembershipStatus.ACTIVE);

        Map<String, List<CourseMembership>> directPeerCourseMap = new HashMap<>();
        for (CourseMembership m : directMemberships) {
            if (m.getUserId() != null && !m.getUserId().equals(currentUserId)) {
                directPeerCourseMap.computeIfAbsent(m.getUserId(), k -> new ArrayList<>()).add(m);
            }
        }

        Set<String> directPeerIds = directPeerCourseMap.keySet();

        // 2nd-degree: mutual space peers (peers who share spaces with your classmates)
        Map<String, Set<String>> mutualPeersMap = new HashMap<>();
        if (!directPeerIds.isEmpty()) {
            List<CourseMembership> peerOtherMemberships = courseMembershipRepository
                    .findAllByUserIdInAndStatus(directPeerIds, MembershipStatus.ACTIVE);

            Set<String> otherCourseIds = peerOtherMemberships.stream()
                    .map(CourseMembership::getCourseId)
                    .filter(id -> id != null && !myCourseIds.contains(id))
                    .collect(Collectors.toSet());

            if (!otherCourseIds.isEmpty()) {
                Map<String, Set<String>> otherCourseToDirectPeers = new HashMap<>();
                for (CourseMembership m : peerOtherMemberships) {
                    if (otherCourseIds.contains(m.getCourseId()) && m.getUserId() != null) {
                        otherCourseToDirectPeers.computeIfAbsent(m.getCourseId(), k -> new HashSet<>())
                                .add(m.getUserId());
                    }
                }

                List<CourseMembership> secondDegreeMemberships = courseMembershipRepository
                        .findAllByCourseIdInAndStatus(otherCourseIds, MembershipStatus.ACTIVE);

                for (CourseMembership m : secondDegreeMemberships) {
                    String candidateId = m.getUserId();
                    if (candidateId != null && !candidateId.equals(currentUserId)
                            && !directPeerIds.contains(candidateId)) {
                        Set<String> mutualPeers = otherCourseToDirectPeers.getOrDefault(m.getCourseId(),
                                Collections.emptySet());
                        if (!mutualPeers.isEmpty()) {
                            mutualPeersMap.computeIfAbsent(candidateId, k -> new HashSet<>()).addAll(mutualPeers);
                        }
                    }
                }
            }
        }

        Set<String> allCandidateIds = new HashSet<>(directPeerIds);
        allCandidateIds.addAll(mutualPeersMap.keySet());

        if (allCandidateIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserProfile> candidateProfiles = userProfileRepository
                .findAllByUserIdInAndDeletedAtIsNull(allCandidateIds);

        List<ScoredRecommendation> results = new ArrayList<>();

        for (UserProfile profile : candidateProfiles) {
            if (profile == null || profile.getUserId() == null
                    || profile.getProfileVisibility() != ProfileVisibility.PUBLIC) {
                continue;
            }

            String candidateId = profile.getUserId();

            if (directPeerIds.contains(candidateId)) {
                // Direct space classmate
                List<String> sharedTitles = directPeerCourseMap.getOrDefault(candidateId, List.of()).stream()
                        .map(m -> courseTitleMap.get(m.getCourseId()))
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                long sharedCount = sharedTitles.size();
                int score = (int) (sharedCount * 10);

                results.add(new ScoredRecommendation(
                        profile,
                        sharedCount,
                        sharedTitles,
                        0,
                        REASON_SHARED_SPACES,
                        score));
            } else if (mutualPeersMap.containsKey(candidateId)) {
                // 2nd-degree space-joined mutual friend
                long mutualCount = mutualPeersMap.get(candidateId).size();
                int score = (int) (mutualCount * 3);

                results.add(new ScoredRecommendation(
                        profile,
                        0,
                        List.of(),
                        mutualCount,
                        REASON_MUTUAL_SPACE_PEERS,
                        score));
            }
        }

        results.sort(Comparator.comparingInt(ScoredRecommendation::score).reversed());
        return results;
    }

    private RecommendedUserResponse toResponse(ScoredRecommendation rec) {
        UserProfile p = rec.profile();
        return RecommendedUserResponse.builder()
                .id(p.getUserId() != null ? p.getUserId() : p.getId())
                .handle(p.getHandle())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .displayName(p.getDisplayName())
                .avatarUrl(p.getAvatarUrl())
                .bannerUrl(p.getBannerUrl())
                .headline(p.getHeadline())
                .department(p.getHeadline() != null ? p.getHeadline() : "")
                .canCreateCourses(p.isCanCreateCourses())
                .sharedCoursesCount(rec.sharedCount())
                .sharedCourseTitles(rec.sharedTitles())
                .mutualPeersCount(rec.mutualPeersCount())
                .recommendationReason(rec.reason())
                .build();
    }

    private record ScoredRecommendation(
            UserProfile profile,
            long sharedCount,
            List<String> sharedTitles,
            long mutualPeersCount,
            String reason,
            int score) {
    }
}
