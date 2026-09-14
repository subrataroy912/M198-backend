package com.M198.Majorproject.profile.service;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockMultipartFile;

import com.M198.Majorproject.profile.exception.ProfileStorageException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

class ProfileMediaStorageServiceTest {

    private final Cloudinary cloudinary = mock(Cloudinary.class);
    private final Uploader uploader = mock(Uploader.class);
    private ProfileMediaStorageService mediaStorageService;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
        mediaStorageService = new ProfileMediaStorageService(cloudinary, true);
    }

    @Test
    void hasContent_ChecksNullAndEmpty() {
        assertFalse(mediaStorageService.hasContent(null));
        assertFalse(mediaStorageService.hasContent(new MockMultipartFile("f", new byte[0])));
        assertTrue(mediaStorageService.hasContent(new MockMultipartFile("f", new byte[]{1, 2})));
    }

    @Test
    void uploadImage_PassthroughNonDataUri() {
        String regularUrl = "https://cdn.example.com/my-pic.png";
        assertEquals(regularUrl, mediaStorageService.uploadImage(regularUrl, "user_avatars"));
    }

    @Test
    void uploadImage_ValidDataUriUploadsToCloudinary() throws Exception {
        byte[] originalBytes = new byte[]{10, 20, 30};
        String base64Data = Base64.getEncoder().encodeToString(originalBytes);
        String dataUri = "data:image/png;base64," + base64Data;

        when(uploader.upload(any(byte[].class), argThat(options -> "user_avatars".equals(options.get("folder")))))
                .thenReturn(Map.of("secure_url", "https://cloudinary.com/avatar123.png"));

        String result = mediaStorageService.uploadImage(dataUri, "user_avatars");

        assertEquals("https://cloudinary.com/avatar123.png", result);
        verify(uploader).upload(any(byte[].class), argThat(options -> "user_avatars".equals(options.get("folder"))));
    }

    @Test
    void uploadImage_InvalidDataUriThrowsProfileStorageException() {
        // Missing ;base64
        assertThrows(ProfileStorageException.class,
                () -> mediaStorageService.uploadImage("data:image/png,somedata", "user_avatars"));

        // Non image contentType
        assertThrows(ProfileStorageException.class,
                () -> mediaStorageService.uploadImage("data:application/pdf;base64,AAAA", "user_avatars"));

        // Invalid base64
        assertThrows(ProfileStorageException.class,
                () -> mediaStorageService.uploadImage("data:image/png;base64,@@@@!!", "user_avatars"));
    }

    @Test
    void upload_FallbackToBase64WhenCloudinaryFailsAndFallbackEnabled() throws Exception {
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("Cloudinary timeout"));

        byte[] bytes = new byte[]{1, 2, 3};
        String fallbackResult = mediaStorageService.upload(bytes, "image/png", "user_avatars");

        assertTrue(fallbackResult.startsWith("data:image/png;base64,"));
    }

    @Test
    void upload_ThrowsWhenFallbackDisabledAndCloudinaryFails() throws Exception {
        ProfileMediaStorageService strictService = new ProfileMediaStorageService(cloudinary, false);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("Cloudinary timeout"));

        byte[] bytes = new byte[]{1, 2, 3};
        assertThrows(ProfileStorageException.class,
                () -> strictService.upload(bytes, "image/png", "user_avatars"));
    }
}
