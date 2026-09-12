package com.M198.Majorproject.controller.comment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.M198.Majorproject.config.GlobalExceptionHandler;
import com.M198.Majorproject.dto.CreateCommentRequest;
import com.M198.Majorproject.service.comment.CommentService;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void blankCommentReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/coursework/work-1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deniedCommentAccessReturnsForbidden() throws Exception {
        when(service.addCourseworkComment(eq("work-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class), any(CreateCommentRequest.class)))
                .thenThrow(new CommentService.CommentAccessException());

        mockMvc.perform(post("/v1/coursework/work-1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Question\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingCommentTargetReturnsNotFound() throws Exception {
        when(service.addCourseworkComment(eq("missing"), org.mockito.ArgumentMatchers.nullable(Authentication.class), any(CreateCommentRequest.class)))
                .thenThrow(new CommentService.CommentNotFoundException());

        mockMvc.perform(post("/v1/coursework/missing/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Question\"}"))
                .andExpect(status().isNotFound());
    }
}
