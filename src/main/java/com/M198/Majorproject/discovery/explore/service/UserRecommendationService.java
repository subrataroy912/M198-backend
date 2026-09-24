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
    public static final String REASON_SAME_DEPARTMENT = "SAME_DEPARTMENT";
    public static final String REASON_FEATURED_CREATOR = "FEATURED_CREATOR";

    private final UserProfileRepository userProfileRepository;
    private final CourseMembershipRepository courseMembershipRepository;
    private final CourseRepository courseRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public Page<RecommendedUserResponse> getRecommendedUsers(Authentication authentication, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        String currentUserId = authenticatedUserResolver.resolveAuthenticatedUserIdSafe(authentication);

        List<ScoredRecommendation> scoredCandidates;
        if (currentUserId == null || currentUserId.isBlank()) {
            scoredCandidates = getGuestRecommendations(safePage, safeSize);
        } else {
            scoredCandidates = getPersonalizedRecommendations(currentUserId, safePage, safeSize);
        }

        int total = scoredCandidates.size();
        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        List<RecommendedUserResponse> pageContent = scoredCandidates.subList(fromIndex, toIndex).stream()
                .map(this::toResponse)
                .toList();

        Pageable pageable = PageRequest.of(safePage, safeSize);
        return new PageImpl<>(pageContent, pageable, total);
    }

    private List<ScoredRecommendation> getGuestRecommendations(int page, int size) {
        int targetCount = (page + 1) * size;
        Pageable fetchPageable = PageRequest.of(0, Math.max(targetCount, 20));

        List<UserProfile> creators = userProfileRepository
                .findAllByCanCreateCoursesTrueAndProfileVisibilityAndDeletedAtIsNull(ProfileVisibility.PUBLIC,
                        fetchPageable);

        Set<String> seenUserIds = new HashSet<>();
        List<ScoredRecommendation> results = new ArrayList<>();

        if (creators != null) {
            for (UserProfile p : creators) {
                if (p != null && p.getUserId() != null && seenUserIds.add(p.getUserId())) {
                    results.add(new ScoredRecommendation(p, 0, List.of(), false, REASON_FEATURED_CREATOR, 10));
                }
            }
        }

        if (results.size() < targetCount) {
            Page<UserProfile> publicProfiles = userProfileRepository
                    .findAllByProfileVisibilityAndDeletedAtIsNull(ProfileVisibility.PUBLIC, fetchPageable);
            if (publicProfiles != null) {
                for (UserProfile p : publicProfiles) {
                    if (p != null && p.getUserId() != null && seenUserIds.add(p.getUserId())) {
                        results.add(new ScoredRecommendation(p, 0, List.of(), false, REASON_FEATURED_CREATOR, 1));
                    }
                }
            }
        }

        return results;
    }

    private List<ScoredRecommendation> getPersonalizedRecommendations(String currentUserId, int page, int size) {
        int targetCount = (page + 1) * size;

        UserProfile myProfile = userProfileRepository.findByUserIdAndDeletedAtIsNull(currentUserId).orElse(null);
        String myDepartment = myProfile != null && myProfile.getHeadline() != null
                ? myProfile.getHeadline().trim().toLowerCase()
                : "";

        List<CourseMembership> myMemberships = courseMembershipRepository
                .findAllByUserIdAndStatus(currentUserId, MembershipStatus.ACTIVE);

        List<String> myCourseIds = myMemberships.stream()
                .map(CourseMembership::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, String> courseTitleMap = new HashMap<>();
        Map<String, List<CourseMembership>> peerCourseMap = new HashMap<>();

        if (!myCourseIds.isEmpty()) {
            courseRepository.findAllByIdInAndStatus(myCourseIds, CourseStatus.ACTIVE)
                    .forEach(c -> courseTitleMap.put(c.getId(), c.getTitle()));

            List<CourseMembership> peerMemberships = courseMembershipRepository
                    .findAllByCourseIdInAndStatus(myCourseIds, MembershipStatus.ACTIVE);

            for (CourseMembership m : peerMemberships) {
                if (m.getUserId() != null && !m.getUserId().equals(currentUserId)) {
                    peerCourseMap.computeIfAbsent(m.getUserId(), k -> new ArrayList<>()).add(m);
                }
            }
        }

        Set<String> candidateUserIds = new HashSet<>(peerCourseMap.keySet());
        List<UserProfile> candidateProfiles = candidateUserIds.isEmpty()
                ? Collections.emptyList()
                : userProfileRepository.findAllByUserIdInAndDeletedAtIsNull(candidateUserIds);

        Set<String> seenUserIds = new HashSet<>();
        seenUserIds.add(currentUserId);

        List<ScoredRecommendation> results = new ArrayList<>();

        for (UserProfile profile : candidateProfiles) {
            if (profile.getUserId() == null || profile.getProfileVisibility() != ProfileVisibility.PUBLIC) {
                continue;
            }

            seenUserIds.add(profile.getUserId());

            List<String> sharedTitles = peerCourseMap.getOrDefault(profile.getUserId(), List.of()).stream()
                    .map(m -> courseTitleMap.get(m.getCourseId()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            long sharedCount = sharedTitles.size();
            boolean sameDept = !myDepartment.isEmpty()
                    && profile.getHeadline() != null
                    && profile.getHeadline().trim().toLowerCase().equals(myDepartment);

            int score = (int) (sharedCount * 10) + (sameDept ? 5 : 0) + (profile.isCanCreateCourses() ? 2 : 0);
            String reason = sharedCount > 0
                    ? REASON_SHARED_SPACES
                    : (sameDept ? REASON_SAME_DEPARTMENT : REASON_FEATURED_CREATOR);

            results.add(new ScoredRecommendation(profile, sharedCount, sharedTitles, sameDept, reason, score));
        }

        // If candidates are fewer than target, supplement with creators
        if (results.size() < targetCount) {
            Pageable supplementPageable = PageRequest.of(0, Math.max(targetCount, 20));
            List<UserProfile> creators = userProfileRepository
                    .findAllByCanCreateCoursesTrueAndProfileVisibilityAndDeletedAtIsNull(ProfileVisibility.PUBLIC,
                            supplementPageable);

            if (creators != null) {
                for (UserProfile creator : creators) {
                    if (creator != null && creator.getUserId() != null && seenUserIds.add(creator.getUserId())) {
                        boolean sameDept = !myDepartment.isEmpty()
                                && creator.getHeadline() != null
                                && creator.getHeadline().trim().toLowerCase().equals(myDepartment);
                        int score = (sameDept ? 5 : 0) + 2;
                        String reason = sameDept ? REASON_SAME_DEPARTMENT : REASON_FEATURED_CREATOR;
                        results.add(new ScoredRecommendation(creator, 0, List.of(), sameDept, reason, score));
                    }
                }
            }
        }

        // If still fewer than target, supplement with general public profiles
        if (results.size() < targetCount) {
            Pageable supplementPageable = PageRequest.of(0, Math.max(targetCount, 20));
            Page<UserProfile> publicProfiles = userProfileRepository
                    .findAllByProfileVisibilityAndDeletedAtIsNull(ProfileVisibility.PUBLIC, supplementPageable);

            if (publicProfiles != null) {
                for (UserProfile p : publicProfiles) {
                    if (p != null && p.getUserId() != null && seenUserIds.add(p.getUserId())) {
                        boolean sameDept = !myDepartment.isEmpty()
                                && p.getHeadline() != null
                                && p.getHeadline().trim().toLowerCase().equals(myDepartment);
                        int score = sameDept ? 5 : 1;
                        String reason = sameDept ? REASON_SAME_DEPARTMENT : REASON_FEATURED_CREATOR;
                        results.add(new ScoredRecommendation(p, 0, List.of(), sameDept, reason, score));
                    }
                }
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
                .sameDepartment(rec.sameDept())
                .recommendationReason(rec.reason())
                .build();
    }

    private record ScoredRecommendation(
            UserProfile profile,
            long sharedCount,
            List<String> sharedTitles,
            boolean sameDept,
            String reason,
            int score) {
    }
}
