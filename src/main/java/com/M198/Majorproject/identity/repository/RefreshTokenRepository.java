/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : RefreshTokenRepository
 * PURPOSE    : Stores and validates refresh tokens used for rotation-based authentication.
 *
 * This repository is responsible for finding active refresh tokens and revoking them safely
 * whenever a session is refreshed or logged out.
 */
package com.M198.Majorproject.identity.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import com.M198.Majorproject.identity.entity.RefreshToken;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Query("{ 'token_hash': ?0, 'revoked_at': null }")
    @Update("{ '$set': { 'revoked_at': ?1 } }")
    long revokeIfActive(String tokenHash, java.time.Instant revokedAt);

    long deleteByTokenHash(String tokenHash);

    long deleteAllByUserId(String userId);

    java.util.List<RefreshToken> findAllByUserIdOrderByCreatedAtAsc(String userId);

    @Query(value = "{ '$or': [ { 'revoked_at': { '$ne': null } }, { 'expires_at': { '$lt': ?0 } } ] }", delete = true)
    long deleteStaleOrRevokedTokens(java.time.Instant now);
}