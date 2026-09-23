package com.M198.Majorproject.core.course.adapter;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.core.course.port.CourseMediaPort;
import com.M198.Majorproject.core.course.port.CourseMediaUploadSignature;
import com.M198.Majorproject.user.profile.service.MediaStorageService;
import com.M198.Majorproject.user.profile.service.MediaUploadSignature;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CourseMediaStorageAdapter implements CourseMediaPort {

    private final MediaStorageService mediaStorageService;

    @Override
    public CourseMediaUploadSignature requestImageUpload(String folder) {
        MediaUploadSignature signature = mediaStorageService.requestImageUpload(folder);
        return new CourseMediaUploadSignature(
                signature.uploadUrl(),
                signature.publicId(),
                signature.apiKey(),
                signature.signature(),
                signature.timestamp());
    }

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        return mediaStorageService.uploadImage(file, folder);
    }

    @Override
    public String uploadImage(String dataUriOrUrl, String folder) {
        return mediaStorageService.uploadImage(dataUriOrUrl, folder);
    }

    @Override
    public boolean hasContent(MultipartFile file) {
        return mediaStorageService.hasContent(file);
    }

    @Override
    public void deleteImage(String assetUrl) {
        mediaStorageService.deleteImage(assetUrl);
    }
}
