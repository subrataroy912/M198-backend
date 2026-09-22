package com.M198.Majorproject.discovery.comment.dto;

import java.time.Instant;

import com.M198.Majorproject.discovery.comment.entity.CommentVisibility;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentResponse {
    private String id;
    private String courseId;
    private String authorId;
    private String targetId;
    private CommentVisibility visibility;
    private String body;
    private Instant createdAt;
    private Instant editedAt;
}
