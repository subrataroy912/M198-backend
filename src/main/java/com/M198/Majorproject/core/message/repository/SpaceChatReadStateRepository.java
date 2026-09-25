package com.M198.Majorproject.core.message.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.core.message.entity.SpaceChatReadState;

public interface SpaceChatReadStateRepository extends MongoRepository<SpaceChatReadState, String> {

    Optional<SpaceChatReadState> findBySpaceIdAndUserId(String spaceId, String userId);

    List<SpaceChatReadState> findAllByUserIdAndSpaceIdIn(String userId, Collection<String> spaceIds);
}
