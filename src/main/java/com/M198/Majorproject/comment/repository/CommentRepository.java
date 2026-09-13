/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : CommentRepository
 * PURPOSE    : Retrieves comments and discussion entries tied to course, assignment, or submission objects.
 *
 * Comment visibility and deletion rules are enforced through query filters so the app can keep
 * discussion data private, public, or course-scoped as needed.
 */
package com.M198.Majorproject.comment.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.comment.entity.Comment;
import com.M198.Majorproject.comment.entity.CommentTargetType;
import com.M198.Majorproject.comment.entity.CommentVisibility;

public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findAllByTargetTypeAndTargetIdAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
            CommentTargetType targetType, String targetId, CommentVisibility visibility);

    List<Comment> findAllByTargetTypeAndTargetIdAndAuthorIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            CommentTargetType targetType, String targetId, String authorId);

    List<Comment> findAllByCourseIdAndTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            String courseId, CommentTargetType targetType, String targetId);
}
