package com.M198.Majorproject.core.message.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.core.message.entity.SpaceMessage;

public interface SpaceMessageRepository extends MongoRepository<SpaceMessage, String> {

    List<SpaceMessage> findBySpaceIdAndDeletedFalseOrderByIdDesc(String spaceId, Pageable pageable);

    List<SpaceMessage> findBySpaceIdAndDeletedFalseAndIdLessThanOrderByIdDesc(
            String spaceId, String beforeId, Pageable pageable);

    List<SpaceMessage> findBySpaceIdAndDeletedFalseAndCreatedAtGreaterThanEqualOrderByIdDesc(
            String spaceId, Instant since, Pageable pageable);

    List<SpaceMessage> findBySpaceIdAndDeletedFalseAndIdLessThanAndCreatedAtGreaterThanEqualOrderByIdDesc(
            String spaceId, String beforeId, Instant since, Pageable pageable);

    Optional<SpaceMessage> findFirstBySpaceIdAndDeletedFalseOrderByCreatedAtDesc(String spaceId);

    Optional<SpaceMessage> findFirstBySpaceIdAndDeletedFalseAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String spaceId, Instant since);

    long countBySpaceIdAndDeletedFalseAndCreatedAtAfterAndSenderIdNot(
            String spaceId, Instant after, String senderId);

    long countBySpaceIdAndDeletedFalseAndSenderIdNot(String spaceId, String senderId);
}
