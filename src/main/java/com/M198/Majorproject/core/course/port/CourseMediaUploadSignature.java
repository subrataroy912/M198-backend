package com.M198.Majorproject.core.course.port;

/**
 * Upload signature details returned to clients for direct provider uploads.
 */
public record CourseMediaUploadSignature(
        String uploadUrl,
        String publicId,
        String apiKey,
        String signature,
        long timestamp
) {}
