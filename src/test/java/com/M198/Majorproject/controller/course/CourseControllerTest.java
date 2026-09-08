package com.M198.Majorproject.controller.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.M198.Majorproject.controller.auth.AuthExceptionHandler;
import com.M198.Majorproject.dto.CourseResponse;
import com.M198.Majorproject.dto.CreateCourseRequest;
import com.M198.Majorproject.service.course.CourseService;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(courseService))
                .setControllerAdvice(new AuthExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createRejectsMissingTitle() throws Exception {
        mockMvc.perform(post("/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMapsAccessFailureToForbidden() throws Exception {
        when(courseService.createCourse(org.mockito.ArgumentMatchers.nullable(Authentication.class), any(CreateCourseRequest.class)))
                .thenThrow(new CourseService.CourseAccessException("Teacher role required"));

        mockMvc.perform(post("/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Physics\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMapsMissingCourseToNotFound() throws Exception {
        when(courseService.getCourse(eq("missing"), org.mockito.ArgumentMatchers.nullable(Authentication.class)))
                .thenThrow(new CourseService.CourseNotFoundException());

        mockMvc.perform(get("/v1/courses/missing"))
                .andExpect(status().isNotFound());
    }
}
