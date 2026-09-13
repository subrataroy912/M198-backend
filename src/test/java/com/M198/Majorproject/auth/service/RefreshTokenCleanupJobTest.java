package com.M198.Majorproject.auth.service;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.M198.Majorproject.identity.repository.RefreshTokenRepository;

class RefreshTokenCleanupJobTest {

    @Test
    void cleanupStaleTokensInvokesRepositoryDelete() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.deleteStaleOrRevokedTokens(any(Instant.class))).thenReturn(5L);

        RefreshTokenCleanupJob job = new RefreshTokenCleanupJob(repository);
        job.cleanupStaleTokens();

        verify(repository).deleteStaleOrRevokedTokens(any(Instant.class));
    }
}
