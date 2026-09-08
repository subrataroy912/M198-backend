package com.M198.Majorproject.repository.attachment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.attachment.Attachment;
import com.M198.Majorproject.entity.attachment.AttachmentResourceType;
import com.M198.Majorproject.entity.attachment.AttachmentStatus;

public interface AttachmentRepository extends MongoRepository<Attachment, String> {

    List<Attachment> findAllByResourceTypeAndResourceIdAndStatus(
            AttachmentResourceType resourceType, String resourceId, AttachmentStatus status);

    List<Attachment> findAllByOwnerIdAndStatus(String ownerId, AttachmentStatus status);

    Optional<Attachment> findByIdAndStatus(String id, AttachmentStatus status);
}
