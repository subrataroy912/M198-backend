package com.M198.Majorproject.user.profile.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.user.profile.exception.ProfileStorageException;
import com.cloudinary.Cloudinary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfileMediaStorageService implements MediaStorageService {

    private final Cloudinary cloudinary;
    private final boolean allowBase64Fallback;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    @org.springframework.beans.factory.annotation.Autowired
    public ProfileMediaStorageService(
            Cloudinary cloudinary,
            @Value("${app.media.allow-base64-fallback:true}") boolean allowBase64Fallback,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = cloudinary;
        this.allowBase64Fallback = allowBase64Fallback;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public ProfileMediaStorageService(Cloudinary cloudinary, boolean allowBase64Fallback) {
        this(cloudinary, allowBase64Fallback, "", "", "");
    }

    public ProfileMediaStorageService(Cloudinary cloudinary) {
        this(cloudinary, true);
    }

    @Override
    public MediaUploadSignature requestImageUpload(String folder) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ProfileStorageException("Media storage is not configured");
        }
        String publicId = folder + "/" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("public_id", publicId);
        parameters.put("timestamp", timestamp);
        return new MediaUploadSignature(
                "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                publicId,
                apiKey,
                cloudinary.apiSignRequest(parameters, apiSecret, 0),
                timestamp);
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
        String safeType = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", folder);
            options.put("resource_type", "image");
            options.put("quality", "auto:good");
            options.put("fetch_format", "webp");
            applyFolderTransformationBounds(options, folder);
            if (contentType != null && !contentType.isBlank()) {
                options.put("context", "content_type=" + contentType);
            }
            Map<?, ?> result = cloudinary.uploader().upload(bytes, options);
            Object secureUrl = result != null ? result.get("secure_url") : null;
            if (!(secureUrl instanceof String url) || url.isBlank()) {
                throw new ProfileStorageException("Cloudinary did not return a secure asset URL");
            }
            return url;
        } catch (ProfileStorageException exception) {
            if (allowBase64Fallback && bytes != null && bytes.length > 0) {
                log.warn("Cloudinary upload returned invalid URL for folder {}. Falling back to inline base64.", folder);
                return "data:" + safeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
            throw exception;
        } catch (Exception exception) {
            if (allowBase64Fallback && bytes != null && bytes.length > 0) {
                log.warn("Cloudinary upload failed for folder {}. Falling back to inline base64 representation.", folder, exception);
                return "data:" + safeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
            throw new ProfileStorageException("Could not upload profile asset", exception);
        }
    }

    private void applyFolderTransformationBounds(Map<String, Object> options, String folder) {
        if (folder == null) {
            return;
        }
        String normalized = folder.toLowerCase();
        if (normalized.contains("avatar") || normalized.contains("logo")) {
            options.put("transformation", "c_fill,g_center,w_400,h_400,q_auto:eco,f_webp");
        } else if (normalized.contains("banner") || normalized.contains("cover")) {
            options.put("transformation", "c_limit,w_1920,h_1080,q_auto:good,f_webp");
        } else if (normalized.contains("chat") || normalized.contains("feed") || normalized.contains("post")) {
            options.put("transformation", "c_limit,w_1200,h_900,q_auto:good,f_webp");
        } else if (normalized.contains("scan") || normalized.contains("document") || normalized.contains("submission")) {
            options.put("transformation", "c_limit,w_2000,h_1500,q_auto:good,f_webp");
        }
    }

    @Override
    public void deleteImage(String assetUrl) {
        if (assetUrl == null || !assetUrl.contains("cloudinary.com") || !assetUrl.contains("/upload/")) {
            return;
        }
        try {
            String publicId = extractPublicId(assetUrl);
            if (publicId != null && !publicId.isBlank()) {
                Map<String, Object> options = new HashMap<>();
                options.put("invalidate", true);
                cloudinary.uploader().destroy(publicId, options);
                log.info("Successfully deleted profile asset from Cloudinary: {}", publicId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete asset from Cloudinary (non-fatal): {}", assetUrl, e);
        }
    }

    public String extractPublicId(String url) {
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }
        String path = url.substring(uploadIndex + "/upload/".length());
        String[] parts = path.split("/");
        int startIndex = 0;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].matches("v[0-9]+")) {
                startIndex = i + 1;
                break;
            } else if (parts[i].contains(",") || parts[i].startsWith("w_") || parts[i].startsWith("h_") || parts[i].startsWith("c_")) {
                startIndex = i + 1;
            }
        }
        if (startIndex >= parts.length) {
            startIndex = parts.length - 1;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < parts.length; i++) {
            if (!sb.isEmpty()) {
                sb.append("/");
            }
            sb.append(parts[i]);
        }
        String idWithExt = sb.toString();
        int dotIndex = idWithExt.lastIndexOf('.');
        return dotIndex != -1 ? idWithExt.substring(0, dotIndex) : idWithExt;
    }
}
