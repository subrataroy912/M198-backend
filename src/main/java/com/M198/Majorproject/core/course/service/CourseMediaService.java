package com.M198.Majorproject.core.course.service;

import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.core.course.dto.CourseCoverUploadResponse;
import com.M198.Majorproject.core.course.dto.CreateCourseRequest;
import com.M198.Majorproject.core.course.dto.UpdateCourseRequest;
import com.M198.Majorproject.core.course.port.CourseMediaPort;
import com.M198.Majorproject.core.course.port.CourseMediaUploadSignature;

/**
 * Provider-neutral application service for course media workflows.
 * Provider signing and storage implementation remain behind {@link CourseMediaPort}.
 */
@Service
public class CourseMediaService {
    public enum Asset {
        COVER("course_covers"),
        LOGO("course_logos");

        private final String folder;

        Asset(String folder) {
            this.folder = folder;
        }

        String folder() {
            return folder;
        }
    }

    private final CourseMediaPort storage;

    public CourseMediaService(CourseMediaPort storage) {
        this.storage = storage;
    }

    public CourseCoverUploadResponse requestUpload(Asset asset) {
        CourseMediaUploadSignature signedUpload = storage.requestImageUpload(asset.folder());
        CourseCoverUploadResponse response = new CourseCoverUploadResponse();
        response.setUploadUrl(signedUpload.uploadUrl());
        response.setPublicId(signedUpload.publicId());
        response.setUploadApiKey(signedUpload.apiKey());
        response.setUploadSignature(signedUpload.signature());
        response.setUploadTimestamp(signedUpload.timestamp());
        return response;
    }

    public void applyMultipartAssets(CreateCourseRequest request, MultipartFile coverFile, MultipartFile logoFile) {
        applyMultipartAssets(coverFile, logoFile, request::setCoverUrl, request::setLogoUrl);
    }

    public void applyMultipartAssets(UpdateCourseRequest request, MultipartFile coverFile, MultipartFile logoFile) {
        applyMultipartAssets(coverFile, logoFile, request::setCoverUrl, request::setLogoUrl);
    }

    public String resolveAsset(String value, Asset asset) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return storage.uploadImage(value.trim(), asset.folder());
    }

    private void applyMultipartAssets(
            MultipartFile coverFile,
            MultipartFile logoFile,
            Consumer<String> setCover,
            Consumer<String> setLogo) {
        uploadIfPresent(coverFile, Asset.COVER, setCover);
        uploadIfPresent(logoFile, Asset.LOGO, setLogo);
    }

    private void uploadIfPresent(MultipartFile file, Asset asset, Consumer<String> setAsset) {
        if (storage.hasContent(file)) {
            setAsset.accept(storage.uploadImage(file, asset.folder()));
        }
    }
}
