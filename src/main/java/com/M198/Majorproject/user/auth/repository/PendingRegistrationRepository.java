package com.M198.Majorproject.user.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.M198.Majorproject.user.auth.entity.PendingRegistration;

@Repository
public interface PendingRegistrationRepository extends MongoRepository<PendingRegistration, String> {

    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);
}
