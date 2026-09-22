/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : UserRepository
 * PURPOSE    : Provides MongoDB access for user identity lookup and account validation.
 *
 * This repository handles user existence checks, active-account queries,
 * and the core authentication lookups used by the security and auth services.
 */
package com.M198.Majorproject.user.identity.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.user.identity.entity.AccountStatus;
import com.M198.Majorproject.user.identity.entity.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, AccountStatus status);

    Optional<User> findByEmailAndActiveTrueAndStatus(String email, AccountStatus status);

    Optional<User> findByIdAndActiveTrueAndStatus(String id, AccountStatus status);
}
