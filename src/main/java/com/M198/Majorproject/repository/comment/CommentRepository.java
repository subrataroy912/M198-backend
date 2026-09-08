package com.M198.Majorproject.repository.comment;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.comment.Comment;
import com.M198.Majorproject.entity.comment.CommentTargetType;
import com.M198.Majorproject.entity.comment.CommentVisibility;

public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findAllByTargetTypeAndTargetIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
            CommentTargetType targetType, String targetId, CommentVisibility visibility);

    List<Comment> findAllByTargetTypeAndTargetIdAndAuthorIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            CommentTargetType targetType, String targetId, String authorId);

    List<Comment> findAllByCourseIdAndTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            String courseId, CommentTargetType targetType, String targetId);
}
