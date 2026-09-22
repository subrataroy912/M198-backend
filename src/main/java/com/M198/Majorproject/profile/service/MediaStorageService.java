package com.M198.Majorproject.profile.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service contract for storing profile and media assets.
 */
public interface MediaStorageService {

    /**
     * Uploads an image file to the designated folder.
     *
     * @param file the multipart file to upload
     * @param folder target folder name
     * @return the secure asset URL
     */
    String uploadImage(MultipartFile file, String folder);

    /**
     * Handles data URI images or passthrough URLs.
     * If the string is a data URI, it decodes and uploads the asset.
     * If it is a normal URL, it returns it as-is.
     *
     * @param dataUriOrUrl data URI string or regular URL
     * @param folder target folder name
     * @return the secure asset URL or original URL
     */
    String uploadImage(String dataUriOrUrl, String folder);

    /**
     * Checks whether a multipart file has actual content.
     *
     * @param file the multipart file
     * @return true if non-null and not empty
     */
    boolean hasContent(MultipartFile file);

    /**
     * Deletes an image from cloud storage if hosted by the media provider.
     * Best-effort execution: should not throw exceptions on failure.
     *
     * @param assetUrl URL of the asset to delete
     */
    void deleteImage(String assetUrl);
}
