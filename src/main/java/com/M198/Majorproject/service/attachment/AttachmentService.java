package com.M198.Majorproject.service.attachment;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.M198.Majorproject.dto.AttachmentResponse;
import com.M198.Majorproject.dto.CreateAttachmentRequest;
import com.M198.Majorproject.entity.attachment.Attachment;
import com.M198.Majorproject.entity.attachment.AttachmentResourceType;
import com.M198.Majorproject.entity.attachment.AttachmentStatus;
import com.M198.Majorproject.entity.course.CourseMembership;
import com.M198.Majorproject.entity.course.MembershipRole;
import com.M198.Majorproject.entity.course.MembershipStatus;
import com.M198.Majorproject.repository.attachment.AttachmentRepository;
import com.M198.Majorproject.repository.course.CourseMembershipRepository;
import com.M198.Majorproject.repository.coursework.CourseworkRepository;
import com.M198.Majorproject.repository.submission.SubmissionRepository;
import com.cloudinary.Cloudinary;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final CourseworkRepository courseworkRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseMembershipRepository membershipRepository;
    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            CourseworkRepository courseworkRepository,
            SubmissionRepository submissionRepository,
            CourseMembershipRepository membershipRepository,
            Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.attachmentRepository = attachmentRepository;
        this.courseworkRepository = courseworkRepository;
        this.submissionRepository = submissionRepository;
        this.membershipRepository = membershipRepository;
        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public AttachmentResponse create(Authentication authentication, CreateAttachmentRequest request) {
        String userId = authenticatedUserId(authentication);
        String courseId = resourceCourseId(request.getResourceType(), request.getResourceId());
        CourseMembership membership = membership(courseId, userId);
        if (request.getResourceType() == AttachmentResourceType.COURSEWORK && !staff(membership)) {
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
            String attachmentId, Authentication authentication, com.M198.Majorproject.dto.CompleteAttachmentRequest request) {
        String userId = authenticatedUserId(authentication);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .filter(value -> value.getStatus() == AttachmentStatus.PENDING)
                .orElseThrow(AttachmentNotFoundException::new);
        CourseMembership membership = hasRole(authentication, "ROLE_ADMIN")
                ? null : membership(attachment.getCourseId(), userId);
        if (!attachment.getOwnerId().equals(userId) && !hasRole(authentication, "ROLE_ADMIN")
                && !staff(membership)) {
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
        String userId = authenticatedUserId(authentication);
        String courseId = resourceCourseId(resourceType, resourceId);
        membership(courseId, userId);
        return attachmentRepository.findAllByResourceTypeAndResourceIdAndStatus(
                resourceType, resourceId, AttachmentStatus.UPLOADED).stream().map(this::toResponse).toList();
    }

    public void delete(String attachmentId, Authentication authentication) throws IOException {
        String userId = authenticatedUserId(authentication);
        Attachment attachment = attachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.UPLOADED)
                .orElseThrow(AttachmentNotFoundException::new);
        CourseMembership membership = membership(attachment.getCourseId(), userId);
        if (!attachment.getOwnerId().equals(userId) && !staff(membership)) {
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
        String userId = authenticatedUserId(authentication);
        Attachment attachment = attachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.UPLOADED)
                .orElseThrow(AttachmentNotFoundException::new);
        CourseMembership membership = membership(attachment.getCourseId(), userId);
        if (!attachment.getOwnerId().equals(userId) && !staff(membership)) {
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

    private CourseMembership membership(String courseId, String userId) {
        return membershipRepository.findByCourseIdAndUserIdAndStatus(
                courseId, userId, MembershipStatus.ACTIVE).orElseThrow(AttachmentAccessException::new);
    }

    private boolean staff(CourseMembership membership) {
        return membership.getRole() == MembershipRole.OWNER || membership.getRole() == MembershipRole.TEACHER
                || membership.getRole() == MembershipRole.ASSISTANT;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(value -> role.equals(value.getAuthority()));
    }

    private String authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AttachmentAccessException();
        }
        return authentication.getName();
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
