/**
 * CREATED BY : SUBRATA ROY
 * JOB        : RefreshTokenCleanupJob
 * PURPOSE    : Scheduled background task to purge expired and revoked refresh tokens
 *              from MongoDB, acting as a safety net alongside Mongo TTL index.
 */
package com.M198.Majorproject.auth.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.M198.Majorproject.identity.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.auth.cleanup-cron:0 0 3 * * ?}")
    public void cleanupStaleTokens() {
        logger.info("Executing scheduled cleanup of stale and revoked refresh tokens");
        long deletedCount = refreshTokenRepository.deleteStaleOrRevokedTokens(Instant.now());
        logger.info("Scheduled refresh token cleanup completed: {} tokens deleted", deletedCount);
    }
}
