package com.M198.Majorproject.core.course.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.M198.Majorproject.core.course.dto.CourseCoverUploadResponse;
import com.M198.Majorproject.user.profile.service.MediaStorageService;
import com.cloudinary.Cloudinary;

/** Owns course-cover/logo upload signing and storage normalization. */
@Service
public class CourseMediaService {
    private final Cloudinary cloudinary; private final MediaStorageService storage;
    private final String cloudName; private final String apiKey; private final String apiSecret;
    public CourseMediaService(Cloudinary cloudinary, MediaStorageService storage,
            @Value("${cloudinary.cloud-name:}") String cloudName, @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary=cloudinary; this.storage=storage; this.cloudName=cloudName; this.apiKey=apiKey; this.apiSecret=apiSecret;
    }
    public CourseCoverUploadResponse requestUpload(String folder) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) throw new CourseService.CourseAccessException("Cloudinary is not configured");
        String publicId=folder+"/"+UUID.randomUUID(); long timestamp=Instant.now().getEpochSecond(); Map<String,Object> params=new HashMap<>();
        params.put("public_id", publicId); params.put("timestamp", timestamp);
        CourseCoverUploadResponse r=new CourseCoverUploadResponse(); r.setUploadUrl("https://api.cloudinary.com/v1_1/"+cloudName+"/image/upload");
        r.setPublicId(publicId); r.setUploadApiKey(apiKey); r.setUploadSignature(cloudinary.apiSignRequest(params,apiSecret,0)); r.setUploadTimestamp(timestamp); return r;
    }
    public String upload(MultipartFile file, String folder) { return file == null || file.isEmpty() ? null : storage.uploadImage(file, folder); }
    public String resolve(String value, String folder) { if (value == null || value.trim().isEmpty()) return null; return storage.uploadImage(value.trim(), folder); }
}
