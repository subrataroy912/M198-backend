package com.M198.Majorproject.profile.service;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.profile.exception.ProfileStorageException;
import com.cloudinary.Cloudinary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfileMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;
    private final boolean allowBase64Fallback;

    @org.springframework.beans.factory.annotation.Autowired
    public ProfileMediaStorageService(
            Cloudinary cloudinary,
            @Value("${app.media.allow-base64-fallback:true}") boolean allowBase64Fallback) {
        this.cloudinary = cloudinary;
        this.allowBase64Fallback = allowBase64Fallback;
    }

    public ProfileMediaStorageService(Cloudinary cloudinary) {
        this(cloudinary, true);
    }

    @Override
    public boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        try {
            return upload(file.getBytes(), file.getContentType(), folder);
        } catch (ProfileStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ProfileStorageException("Could not read uploaded profile asset", exception);
        }
    }

    @Override
    public String uploadImage(String dataUriOrUrl, String folder) {
        if (dataUriOrUrl == null || !dataUriOrUrl.startsWith("data:")) {
            return dataUriOrUrl;
        }
        int separator = dataUriOrUrl.indexOf(',');
        if (separator < 0 || !dataUriOrUrl.substring(0, separator).contains(";base64")) {
            throw new ProfileStorageException("Invalid profile image data");
        }
        String metadata = dataUriOrUrl.substring(5, separator);
        String contentType = metadata.substring(0, metadata.indexOf(';'));
        if (!contentType.startsWith("image/")) {
            throw new ProfileStorageException("Profile image must be an image file");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUriOrUrl.substring(separator + 1));
            return upload(bytes, contentType, folder);
        } catch (IllegalArgumentException exception) {
            throw new ProfileStorageException("Invalid profile image data", exception);
        }
    }

    public String upload(byte[] bytes, String contentType, String folder) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", folder);
            options.put("resource_type", "image");
            if (contentType != null && !contentType.isBlank()) {
                options.put("context", "content_type=" + contentType);
            }
            Map<?, ?> result = cloudinary.uploader().upload(bytes, options);
            Object secureUrl = result.get("secure_url");
            if (!(secureUrl instanceof String url) || url.isBlank()) {
                throw new ProfileStorageException("Cloudinary did not return a secure asset URL");
            }
            return url;
        } catch (ProfileStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            if (allowBase64Fallback && bytes != null && bytes.length > 0) {
                log.warn("Cloudinary upload failed for folder {}. Falling back to inline base64 representation.", folder, exception);
                String type = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
                return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
            throw new ProfileStorageException("Could not upload profile asset", exception);
        }
    }
}
