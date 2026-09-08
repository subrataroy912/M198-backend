package com.M198.Majorproject.controller.coursework;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.M198.Majorproject.controller.auth.AuthExceptionHandler;
import com.M198.Majorproject.dto.CourseworkResponse;
import com.M198.Majorproject.service.coursework.CourseworkService;

@ExtendWith(MockitoExtension.class)
class CourseworkApiControllerTest {

    @Mock
    private CourseworkService courseworkService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseworkApiController(courseworkService))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void listForwardsPaginationAndReturnsPageMetadata() throws Exception {
        CourseworkResponse response = new CourseworkResponse();
        response.setId("work-1");
        when(courseworkService.list(
                eq("course-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class), eq(2), eq(10)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/v1/courses/course-1/coursework?page=2&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("work-1"))
                .andExpect(jsonPath("$.page").value(0));

        verify(courseworkService).list(
                eq("course-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class), eq(2), eq(10));
    }

    @Test
    void listMapsCourseworkAccessFailureToForbidden() throws Exception {
        when(courseworkService.list(
                eq("course-1"), org.mockito.ArgumentMatchers.nullable(Authentication.class), eq(0), eq(20)))
                .thenThrow(new CourseworkService.CourseworkAccessException("Course staff role required"));

        mockMvc.perform(get("/v1/courses/course-1/coursework"))
                .andExpect(status().isForbidden());
    }
}
