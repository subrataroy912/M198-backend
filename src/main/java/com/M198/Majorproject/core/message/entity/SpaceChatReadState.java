package com.M198.Majorproject.core.message.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "space_chat_read_states")
@CompoundIndex(name = "space_user_read_idx", def = "{'space_id': 1, 'user_id': 1}")
public class SpaceChatReadState {

    @Id
    private String id;

    @Field("space_id")
    private String spaceId;

    @Field("user_id")
    private String userId;

    @Field("last_read_at")
    private Instant lastReadAt;
}
