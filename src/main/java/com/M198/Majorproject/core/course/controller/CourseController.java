/**
 * CREATED BY : SUBRATA ROY
 * CONTROLLER : CourseController
 * PURPOSE    : Handles course operations such as creation, listing, updating, enrollment,
 *              membership checks, and course archiving.
 *
 * This controller routes all teaching and student course interactions into the service layer,
 * keeping HTTP concerns separate from the business logic of course management.
 */
package com.M198.Majorproject.core.course.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.M198.Majorproject.core.course.dto.CourseResponse;
import com.M198.Majorproject.core.course.dto.CourseCoverUploadResponse;
import com.M198.Majorproject.core.course.dto.CreateCourseRequest;
import com.M198.Majorproject.core.course.dto.EnrollCourseRequest;
import com.M198.Majorproject.core.course.dto.CourseMemberResponse;
import com.M198.Majorproject.core.course.dto.UpdateCourseRequest;
import com.M198.Majorproject.core.course.service.CourseService;

@RestController
@RequestMapping("/v1/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CourseResponse createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication) {
        return courseService.createCourse(authentication, request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourseResponse createCourseWithAssets(
            @Valid @RequestPart(value = "course", required = false) CreateCourseRequest request,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            Authentication authentication) {
        CreateCourseRequest effectiveRequest = request != null ? request : new CreateCourseRequest();
        return courseService.createCourse(authentication, effectiveRequest, coverFile, logoFile);
    }

    @PostMapping("/cover-upload")
    public CourseCoverUploadResponse requestCoverUpload(Authentication authentication) {
        return courseService.requestCoverUpload(authentication);
    }

    @PostMapping("/logo-upload")
    public CourseCoverUploadResponse requestLogoUpload(Authentication authentication) {
        return courseService.requestLogoUpload(authentication);
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> listCourses(Authentication authentication) {
        List<CourseResponse> courses = courseService.listMyCourses(authentication);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(courses);
    }

    @GetMapping("/{courseId}")
    public CourseResponse getCourse(
            @PathVariable String courseId,
            Authentication authentication) {
        return courseService.getCourse(courseId, authentication);
    }

    @PatchMapping(value = "/{courseId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CourseResponse updateCourse(
            @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseRequest request,
            Authentication authentication) {
        return courseService.update(courseId, authentication, request);
    }

    @PatchMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourseResponse updateCourseWithAssets(
            @PathVariable String courseId,
            @Valid @RequestPart(value = "course", required = false) UpdateCourseRequest request,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            Authentication authentication) {
        UpdateCourseRequest effectiveRequest = request != null ? request : new UpdateCourseRequest();
        return courseService.update(courseId, authentication, effectiveRequest, coverFile, logoFile);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable String courseId, Authentication authentication) {
        courseService.deleteCourse(courseId, authentication);
    }

    @PostMapping("/{courseId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCourse(@PathVariable String courseId, Authentication authentication) {
        courseService.archive(courseId, authentication);
    }

    @GetMapping("/{courseId}/roster")
    public List<CourseMemberResponse> roster(
            @PathVariable String courseId, Authentication authentication) {
        return courseService.roster(courseId, authentication);
    }

    @PostMapping("/join")
    public CourseResponse join(
            @Valid @RequestBody EnrollCourseRequest request,
            Authentication authentication) {
        return courseService.enrollByCode(authentication, request.getCode());
    }

    @PostMapping("/{courseId}/enrollment")
    public CourseResponse enroll(
            @PathVariable String courseId,
            @Valid @RequestBody(required = false) EnrollCourseRequest request,
            Authentication authentication) {
        return courseService.enroll(courseId, authentication, request);
    }

    @DeleteMapping("/{courseId}/enrollment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(
            @PathVariable String courseId,
            Authentication authentication) {
        courseService.leave(courseId, authentication);
    }

    @DeleteMapping("/{courseId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable String courseId,
            @PathVariable String userId,
            Authentication authentication) {
        courseService.removeMember(courseId, userId, authentication);
    }
}
