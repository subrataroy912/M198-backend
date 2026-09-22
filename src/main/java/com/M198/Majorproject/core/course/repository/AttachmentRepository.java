package com.M198.Majorproject.core.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.core.course.entity.Attachment;
import com.M198.Majorproject.core.course.entity.AttachmentResourceType;
import com.M198.Majorproject.core.course.entity.AttachmentStatus;

public interface AttachmentRepository extends MongoRepository<Attachment, String> {

    List<Attachment> findAllByResourceTypeAndResourceIdAndStatus(
            AttachmentResourceType resourceType, String resourceId, AttachmentStatus status);

    List<Attachment> findAllByOwnerIdAndStatus(String ownerId, AttachmentStatus status);

    Optional<Attachment> findByIdAndStatus(String id, AttachmentStatus status);

    void deleteAllByCourseId(String courseId);

    void deleteAllByResourceTypeAndResourceId(AttachmentResourceType resourceType, String resourceId);
}
