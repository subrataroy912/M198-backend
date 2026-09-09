/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : RefreshTokenRepository
 * PURPOSE    : Stores and validates refresh tokens used for rotation-based authentication.
 *
 * This repository is responsible for finding active refresh tokens and revoking them safely
 * whenever a session is refreshed or logged out.
 */
package com.M198.Majorproject.repository.identity;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import com.M198.Majorproject.entity.identity.RefreshToken;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Query("{ 'token_hash': ?0, 'revoked_at': null }")
    @Update("{ '$set': { 'revoked_at': ?1 } }")
    long revokeIfActive(String tokenHash, java.time.Instant revokedAt);
}