package com.M198.Majorproject.core.course.service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.core.course.dto.AttachmentResponse;
import com.M198.Majorproject.core.course.dto.CompleteAttachmentRequest;
import com.M198.Majorproject.core.course.dto.CreateAttachmentRequest;
import com.M198.Majorproject.core.course.entity.Attachment;
import com.M198.Majorproject.core.course.entity.AttachmentResourceType;
import com.M198.Majorproject.core.course.entity.AttachmentStatus;
import com.M198.Majorproject.core.course.repository.AttachmentRepository;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.repository.CourseworkRepository;
import com.M198.Majorproject.core.course.security.CourseAccessPolicy;
import com.M198.Majorproject.core.course.repository.SubmissionRepository;
import com.cloudinary.Cloudinary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final CourseworkRepository courseworkRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    public AttachmentResponse create(Authentication authentication, CreateAttachmentRequest request) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, AttachmentAccessException::new);
        String courseId = resourceCourseId(request.getResourceType(), request.getResourceId());
        CourseMembership membership = courseAccessPolicy.requireActiveMember(courseId, userId,
                AttachmentAccessException::new);
        if (request.getResourceType() == AttachmentResourceType.COURSEWORK && !courseAccessPolicy.isStaff(membership)) {
            throw new AttachmentAccessException();
        }
        if (request.getResourceType() == AttachmentResourceType.SUBMISSION
                && !submissionRepository.findById(request.getResourceId())
                        .map(submission -> submission.getStudentId().equals(userId)).orElse(false)) {
            throw new AttachmentAccessException();
        }
        Attachment attachment = Attachment.builder()
                .courseId(courseId)
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .ownerId(userId)
                .storageKey("pending/" + UUID.randomUUID())
                .originalFilename(request.getOriginalFilename().trim())
                .contentType(request.getContentType().trim())
                .sizeBytes(request.getSizeBytes())
                .status(AttachmentStatus.PENDING)
                .build();
        AttachmentResponse response = toResponse(attachmentRepository.save(attachment));
        UploadParameters upload = uploadParameters(attachment);
        response.setUploadUrl(upload.url());
        response.setPublicId(attachment.getStorageKey());
        response.setUploadApiKey(apiKey);
        response.setUploadSignature(upload.signature());
        response.setUploadTimestamp(upload.timestamp());
        return response;
    }

    public AttachmentResponse complete(
            String attachmentId, Authentication authentication, CompleteAttachmentRequest request) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, AttachmentAccessException::new);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .filter(value -> value.getStatus() == AttachmentStatus.PENDING)
                .orElseThrow(AttachmentNotFoundException::new);
        CourseMembership membership = hasRole(authentication, "ROLE_ADMIN")
                ? null
                : courseAccessPolicy.requireActiveMember(attachment.getCourseId(), userId,
                        AttachmentAccessException::new);
        if (!attachment.getOwnerId().equals(userId) && !hasRole(authentication, "ROLE_ADMIN")
                && !courseAccessPolicy.isStaff(membership)) {
            throw new AttachmentAccessException();
        }
        if (!attachment.getStorageKey().equals(request.getPublicId().trim())) {
            throw new AttachmentConflictException("Cloudinary public ID does not match the upload");
        }
        if (request.getSizeBytes() != null && !request.getSizeBytes().equals(attachment.getSizeBytes())) {
            throw new AttachmentConflictException("Uploaded file size does not match the request");
        }
        attachment.setStatus(AttachmentStatus.UPLOADED);
        return toResponse(attachmentRepository.save(attachment));
    }

    public List<AttachmentResponse> list(
            String resourceId, AttachmentResourceType resourceType, Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, AttachmentAccessException::new);
        String courseId = resourceCourseId(resourceType, resourceId);
        courseAccessPolicy.requireActiveMember(courseId, userId, AttachmentAccessException::new);
        return attachmentRepository.findAllByResourceTypeAndResourceIdAndStatus(
                resourceType, resourceId, AttachmentStatus.UPLOADED).stream().map(this::toResponse).toList();
    }

    public void delete(String attachmentId, Authentication authentication) throws IOException {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, AttachmentAccessException::new);
        Attachment attachment = attachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.UPLOADED)
                .orElseThrow(AttachmentNotFoundException::new);
        CourseMembership membership = courseAccessPolicy.requireActiveMember(attachment.getCourseId(), userId,
                AttachmentAccessException::new);
        if (!attachment.getOwnerId().equals(userId) && !courseAccessPolicy.isStaff(membership)) {
            throw new AttachmentAccessException();
        }
        requireCloudinaryConfiguration();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().destroy(
                    attachment.getStorageKey(), Map.of("resource_type", "auto", "invalidate", true));
            Object resultStatus = result.get("result");
            if (resultStatus != null && !"ok".equals(resultStatus) && !"not found".equals(resultStatus)) {
                throw new AttachmentStorageException("Cloudinary rejected the delete request");
            }
        } catch (AttachmentStorageException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AttachmentStorageException("Cloudinary delete failed");
        }
        attachment.setStatus(AttachmentStatus.DELETED);
        attachment.setDeletedAt(Instant.now());
        attachmentRepository.save(attachment);
    }

    public AttachmentResponse downloadUrl(String attachmentId, Authentication authentication) {
        String userId = courseAccessPolicy.authenticatedUserId(authentication, AttachmentAccessException::new);
        Attachment attachment = attachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.UPLOADED)
                .orElseThrow(AttachmentNotFoundException::new);
        CourseMembership membership = courseAccessPolicy.requireActiveMember(attachment.getCourseId(), userId,
                AttachmentAccessException::new);
        if (!attachment.getOwnerId().equals(userId) && !courseAccessPolicy.isStaff(membership)) {
            throw new AttachmentAccessException();
        }
        AttachmentResponse response = toResponse(attachment);
        requireCloudinaryConfiguration();
        response.setDownloadUrl(cloudinary.url().secure(true).resourceType("auto")
                .generate(attachment.getStorageKey()));
        return response;
    }

    private UploadParameters uploadParameters(Attachment attachment) {
        requireCloudinaryConfiguration();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("public_id", attachment.getStorageKey());
        parameters.put("timestamp", timestamp);
        String signature = cloudinary.apiSignRequest(parameters, apiSecret, 0);
        return new UploadParameters(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload", signature, timestamp);
    }

    private void requireCloudinaryConfiguration() {
        if (cloudName.isBlank() || apiSecret.isBlank()) {
            throw new AttachmentConfigurationException();
        }
    }

    private String resourceCourseId(AttachmentResourceType type, String resourceId) {
        if (type == AttachmentResourceType.COURSEWORK) {
            return courseworkRepository.findById(resourceId)
                    .map(coursework -> coursework.getCourseId())
                    .orElseThrow(AttachmentNotFoundException::new);
        }
        return submissionRepository.findById(resourceId)
                .map(submission -> submission.getCourseId())
                .orElseThrow(AttachmentNotFoundException::new);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(value -> role.equals(value.getAuthority()));
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setCourseId(attachment.getCourseId());
        response.setResourceType(attachment.getResourceType());
        response.setResourceId(attachment.getResourceId());
        response.setOwnerId(attachment.getOwnerId());
        response.setOriginalFilename(attachment.getOriginalFilename());
        response.setContentType(attachment.getContentType());
        response.setSizeBytes(attachment.getSizeBytes());
        response.setStatus(attachment.getStatus());
        response.setCreatedAt(attachment.getCreatedAt());
        return response;
    }

    private record UploadParameters(String url, String signature, long timestamp) {

    }

    public static class AttachmentNotFoundException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class AttachmentAccessException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class AttachmentConfigurationException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    public static class AttachmentConflictException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public AttachmentConflictException(String message) {
            super(message);
        }
    }

    public static class AttachmentStorageException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public AttachmentStorageException(String message) {
            super(message);
        }
    }
}
