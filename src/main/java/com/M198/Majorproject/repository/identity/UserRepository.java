package com.M198.Majorproject.repository.identity;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.identity.AccountStatus;
import com.M198.Majorproject.entity.identity.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, AccountStatus status);

    Optional<User> findByEmailAndActiveTrueAndStatus(String email, AccountStatus status);
}
