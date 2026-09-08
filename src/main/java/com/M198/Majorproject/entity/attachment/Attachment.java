package com.M198.Majorproject.entity.attachment;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
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
@Document(collection = "attachments")
@CompoundIndex(name = "resource_status", def = "{'resource_type': 1, 'resource_id': 1, 'status': 1}")
@CompoundIndex(name = "course_resource", def = "{'course_id': 1, 'resource_type': 1, 'resource_id': 1}")
public class Attachment {

    @Id
    private String id;

    @Indexed
    @Field("course_id")
    private String courseId;

    @Indexed
    @Field("resource_type")
    private AttachmentResourceType resourceType;

    @Indexed
    @Field("resource_id")
    private String resourceId;

    @Indexed
    @Field("owner_id")
    private String ownerId;

    @Field("storage_key")
    private String storageKey;

    @Field("original_filename")
    private String originalFilename;

    @Field("content_type")
    private String contentType;

    @Field("size_bytes")
    private Long sizeBytes;

    private String checksum;

    @Builder.Default
    private AttachmentStatus status = AttachmentStatus.PENDING;

    @Field("created_at")
    @CreatedDate
    private Instant createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Field("deleted_at")
    private Instant deletedAt;
}
