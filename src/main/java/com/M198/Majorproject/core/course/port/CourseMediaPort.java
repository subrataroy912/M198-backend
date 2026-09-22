package com.M198.Majorproject.core.course.port;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage port for course media assets including cover images and logos.
 */
public interface CourseMediaPort {

    /**
     * Creates direct upload credentials for client-side uploads to a specified folder.
     *
     * @param folder target folder name
     * @return signed upload metadata
     */
    CourseMediaUploadSignature requestImageUpload(String folder);

    /**
     * Uploads a multipart image file to the designated folder.
     *
     * @param file image file to upload
     * @param folder target folder name
     * @return secure asset URL
     */
    String uploadImage(MultipartFile file, String folder);

    /**
     * Resolves data URIs or returns regular URLs unchanged.
     *
     * @param dataUriOrUrl data URI or existing URL
     * @param folder target folder name
     * @return resolved secure URL
     */
    String uploadImage(String dataUriOrUrl, String folder);

    /**
     * Checks if a multipart file contains actual content.
     *
     * @param file multipart file
     * @return true if non-null and not empty
     */
    boolean hasContent(MultipartFile file);

    /**
     * Deletes an image from cloud storage if hosted by the provider.
     *
     * @param assetUrl URL of the asset to delete
     */
    void deleteImage(String assetUrl);
}
