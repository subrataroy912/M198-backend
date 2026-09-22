package com.M198.Majorproject.core.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.M198.Majorproject.core.course.dto.CreateCourseRequest;
import com.M198.Majorproject.core.course.dto.UpdateCourseRequest;
import com.M198.Majorproject.user.profile.service.MediaStorageService;
import com.M198.Majorproject.user.profile.service.MediaUploadSignature;

class CourseMediaServiceTest {
    private final MediaStorageService storage = mock(MediaStorageService.class);
    private final CourseMediaService mediaService = new CourseMediaService(storage);

    @Test
    void signsCoverAndLogoUploadsThroughTheStorageAbstraction() {
        when(storage.requestImageUpload("course_covers"))
                .thenReturn(new MediaUploadSignature("https://uploads.example/cover", "course_covers/1", "key", "cover-signature", 100L));
        when(storage.requestImageUpload("course_logos"))
                .thenReturn(new MediaUploadSignature("https://uploads.example/logo", "course_logos/2", "key", "logo-signature", 200L));

        var cover = mediaService.requestUpload(CourseMediaService.Asset.COVER);
        var logo = mediaService.requestUpload(CourseMediaService.Asset.LOGO);

        assertEquals("course_covers/1", cover.getPublicId());
        assertEquals("cover-signature", cover.getUploadSignature());
        assertEquals("course_logos/2", logo.getPublicId());
        assertEquals("logo-signature", logo.getUploadSignature());
        verify(storage).requestImageUpload("course_covers");
        verify(storage).requestImageUpload("course_logos");
    }

    @Test
    void resolvesCoverAndLogoMultipartFilesThroughTheSharedStorageAbstraction() {
        MockMultipartFile cover = new MockMultipartFile("cover", "cover.png", "image/png", new byte[] {1});
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[] {2});
        when(storage.hasContent(cover)).thenReturn(true);
        when(storage.hasContent(logo)).thenReturn(true);
        when(storage.uploadImage(cover, "course_covers")).thenReturn("https://media.example/cover.png");
        when(storage.uploadImage(logo, "course_logos")).thenReturn("https://media.example/logo.png");

        CreateCourseRequest createRequest = new CreateCourseRequest();
        UpdateCourseRequest updateRequest = new UpdateCourseRequest();
        mediaService.applyMultipartAssets(createRequest, cover, logo);
        mediaService.applyMultipartAssets(updateRequest, cover, logo);

        assertEquals("https://media.example/cover.png", createRequest.getCoverUrl());
        assertEquals("https://media.example/logo.png", createRequest.getLogoUrl());
        assertEquals("https://media.example/cover.png", updateRequest.getCoverUrl());
        assertEquals("https://media.example/logo.png", updateRequest.getLogoUrl());
        verify(storage, org.mockito.Mockito.times(2)).uploadImage(cover, "course_covers");
        verify(storage, org.mockito.Mockito.times(2)).uploadImage(logo, "course_logos");
    }
}
