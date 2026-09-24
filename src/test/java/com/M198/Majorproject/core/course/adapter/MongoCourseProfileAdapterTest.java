package com.M198.Majorproject.core.course.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;

class MongoCourseProfileAdapterTest {

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final MongoCourseProfileAdapter adapter = new MongoCourseProfileAdapter(repository);

    @Test
    void findByUserIdReturnsEmptyForNullOrBlankUserIdWithoutRepositoryInteraction() {
        assertTrue(adapter.findByUserId(null).isEmpty());
        assertTrue(adapter.findByUserId("").isEmpty());
        assertTrue(adapter.findByUserId("   ").isEmpty());

        verifyNoInteractions(repository);
    }

    @Test
    void findByUserIdQueriesActiveProfilesOnly() {
        UserProfile profile = UserProfile.builder()
                .userId("user-1")
                .displayName("Alice")
                .build();

        when(repository.findByUserIdAndDeletedAtIsNull("user-1"))
                .thenReturn(Optional.of(profile));

        Optional<UserProfile> result = adapter.findByUserId("user-1");

        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getDisplayName());
        verify(repository).findByUserIdAndDeletedAtIsNull("user-1");
    }

    @Test
    void findAllByUserIdInReturnsEmptyForNullOrEmptyCollectionWithoutRepositoryInteraction() {
        assertEquals(List.of(), adapter.findAllByUserIdIn(null));
        assertEquals(List.of(), adapter.findAllByUserIdIn(Collections.emptyList()));

        verifyNoInteractions(repository);
    }

    @Test
    void findAllByUserIdInQueriesActiveProfilesOnly() {
        UserProfile profile1 = UserProfile.builder().userId("user-1").displayName("Alice").build();
        UserProfile profile2 = UserProfile.builder().userId("user-2").displayName("Bob").build();
        List<String> userIds = List.of("user-1", "user-2");

        when(repository.findAllByUserIdInAndDeletedAtIsNull(userIds))
                .thenReturn(List.of(profile1, profile2));

        List<UserProfile> result = adapter.findAllByUserIdIn(userIds);

        assertEquals(2, result.size());
        verify(repository).findAllByUserIdInAndDeletedAtIsNull(userIds);
    }
}
