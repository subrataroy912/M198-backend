package com.M198.Majorproject.controller.attachment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.M198.Majorproject.controller.auth.AuthExceptionHandler;
import com.M198.Majorproject.dto.CompleteAttachmentRequest;
import com.M198.Majorproject.dto.CreateAttachmentRequest;
import com.M198.Majorproject.service.attachment.AttachmentService;

@ExtendWith(MockitoExtension.class)
class AttachmentApiControllerTest {

    @Mock
    private AttachmentService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AttachmentApiController(service))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void missingStorageConfigurationReturnsServiceUnavailable() throws Exception {
        when(service.create(org.mockito.ArgumentMatchers.nullable(Authentication.class), any(CreateAttachmentRequest.class)))
                .thenThrow(new AttachmentService.AttachmentConfigurationException());

        mockMvc.perform(post("/v1/attachments/upload-url")
                .contentType("application/json")
                .content("{\"resourceType\":\"COURSEWORK\",\"resourceId\":\"work-1\",\"originalFilename\":\"file.pdf\",\"contentType\":\"application/pdf\",\"sizeBytes\":100}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void invalidCompletionReturnsConflict() throws Exception {
        when(service.complete(eq("attachment-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class), any(CompleteAttachmentRequest.class)))
                .thenThrow(new AttachmentService.AttachmentConflictException("Cloudinary public ID does not match the upload"));

        mockMvc.perform(post("/v1/attachments/attachment-1/complete")
                .contentType("application/json")
                .content("{\"publicId\":\"wrong\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void storageDeleteFailureReturnsBadGateway() throws Exception {
        doThrow(new AttachmentService.AttachmentStorageException("Cloudinary delete failed"))
                .when(service).delete(eq("attachment-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class));

        mockMvc.perform(delete("/v1/attachments/attachment-1"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void unauthorizedAttachmentReturnsForbidden() throws Exception {
        doThrow(new AttachmentService.AttachmentAccessException())
                .when(service).delete(eq("attachment-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class));

        mockMvc.perform(delete("/v1/attachments/attachment-1"))
                .andExpect(status().isForbidden());
    }
}
