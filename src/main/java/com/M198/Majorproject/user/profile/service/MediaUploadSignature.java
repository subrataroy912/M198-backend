package com.M198.Majorproject.user.profile.service;

/** Provider-neutral details needed by a client to perform a signed media upload. */
public record MediaUploadSignature(
        String uploadUrl,
        String publicId,
        String apiKey,
        String signature,
        long timestamp) {
}
