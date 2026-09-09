/**
 * CREATED BY : SUBRATA ROY
 * CONTROLLER : CourseController
 * PURPOSE    : Handles course operations such as creation, listing, updating, enrollment,
 *              membership checks, and course archiving.
 *
 * This controller routes all teaching and student course interactions into the service layer,
 * keeping HTTP concerns separate from the business logic of course management.
 */
package com.M198.Majorproject.controller.course;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.M198.Majorproject.dto.CourseResponse;
import com.M198.Majorproject.dto.CourseCoverUploadResponse;
import com.M198.Majorproject.dto.CreateCourseRequest;
import com.M198.Majorproject.dto.EnrollCourseRequest;
import com.M198.Majorproject.dto.CourseMemberResponse;
import com.M198.Majorproject.dto.UpdateCourseRequest;
import com.M198.Majorproject.service.course.CourseService;

@RestController
@RequestMapping("/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public CourseResponse createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication) {
        return courseService.createCourse(authentication, request);
    }

    @PostMapping("/cover-upload")
    public CourseCoverUploadResponse requestCoverUpload(Authentication authentication) {
        return courseService.requestCoverUpload(authentication);
    }

    @GetMapping
    public List<CourseResponse> listCourses(Authentication authentication) {
        return courseService.listMyCourses(authentication);
    }

    @GetMapping("/{courseId}")
    public CourseResponse getCourse(
            @PathVariable String courseId,
            Authentication authentication) {
        return courseService.getCourse(courseId, authentication);
    }

    @PatchMapping("/{courseId}")
    public CourseResponse updateCourse(
            @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseRequest request,
            Authentication authentication) {
        return courseService.update(courseId, authentication, request);
    }

    @DeleteMapping("/{courseId}")
    public void archiveCourse(@PathVariable String courseId, Authentication authentication) {
        courseService.archive(courseId, authentication);
    }

    @GetMapping("/{courseId}/roster")
    public List<CourseMemberResponse> roster(
            @PathVariable String courseId, Authentication authentication) {
        return courseService.roster(courseId, authentication);
    }

    @PostMapping("/{courseId}/enrollment")
    public CourseResponse enroll(
            @PathVariable String courseId,
            @Valid @RequestBody EnrollCourseRequest request,
            Authentication authentication) {
        return courseService.enroll(courseId, authentication, request);
    }

    @DeleteMapping("/{courseId}/enrollment")
    public void leave(
            @PathVariable String courseId,
            Authentication authentication) {
        courseService.leave(courseId, authentication);
    }
}
