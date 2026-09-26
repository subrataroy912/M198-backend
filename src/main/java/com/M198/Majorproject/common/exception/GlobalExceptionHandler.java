package com.M198.Majorproject.common.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.M198.Majorproject.user.auth.exception.*;
import com.M198.Majorproject.user.profile.exception.*;
import com.M198.Majorproject.core.course.service.CourseService.*;
import com.M198.Majorproject.core.course.service.CourseworkService.*;
import com.M198.Majorproject.core.course.service.SubmissionService.*;
import com.M198.Majorproject.core.course.service.AttachmentService.*;
import com.M198.Majorproject.discovery.comment.service.CommentService.*;
import com.M198.Majorproject.discovery.notification.service.NotificationService.*;

import io.jsonwebtoken.JwtException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(BadCredentialsException.class)
        ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException exception) {
                logger.warn("Invalid credentials : {}", exception.getMessage());
                String message = exception.getMessage() != null && !exception.getMessage().isBlank()
                                ? exception.getMessage()
                                : "Invalid credentials";
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", message));
        }

        @ExceptionHandler(AuthenticationException.class)
        ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException exception) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid credentials"));
        }

        @ExceptionHandler(DuplicateKeyException.class)
        ResponseEntity<Map<String, String>> handleDuplicateKeyException(DuplicateKeyException exception) {
                String message = exception.getMessage() != null && !exception.getMessage().isBlank()
                                ? exception.getMessage()
                                : "Email is already registered";
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", message));
        }

        @ExceptionHandler(AuthConflictException.class)
        ResponseEntity<Map<String, String>> handleAuthConflictException(AuthConflictException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(RefreshTokenException.class)
        ResponseEntity<Map<String, String>> handleRefreshTokenException(RefreshTokenException exception) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(HandleConflictException.class)
        ResponseEntity<Map<String, String>> handleHandleConflictException(HandleConflictException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Handle is already taken"));
        }

        @ExceptionHandler(HandleRateLimitExceededException.class)
        ResponseEntity<Map<String, Object>> handleHandleRateLimitExceededException(HandleRateLimitExceededException exception) {
                java.util.Map<String, Object> body = new java.util.HashMap<>();
                body.put("error", exception.getMessage());
                if (exception.getResetsAt() != null) {
                        body.put("resetsAt", exception.getResetsAt().toString());
                }
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException exception) {
                return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(JwtException.class)
        ResponseEntity<Map<String, String>> handleJwtException(JwtException exception) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid token"));
        }

        @ExceptionHandler(ProfileNotFoundException.class)
        ResponseEntity<Map<String, String>> handleProfileNotFoundException(ProfileNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Profile not found"));
        }

        @ExceptionHandler(ProfileStorageException.class)
        ResponseEntity<Map<String, String>> handleProfileStorageException(ProfileStorageException exception) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(CourseNotFoundException.class)
        ResponseEntity<Map<String, String>> handleCourseNotFoundException(CourseNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Course not found"));
        }

        @ExceptionHandler(CourseBadRequestException.class)
        ResponseEntity<Map<String, String>> handleCourseBadRequestException(CourseBadRequestException exception) {
                return ResponseEntity.badRequest()
                                .body(Map.of("error", exception.getMessage() != null ? exception.getMessage()
                                                : "Bad request"));
        }

        @ExceptionHandler(CourseIdFormatException.class)
        ResponseEntity<Map<String, String>> handleCourseIdFormatException(CourseIdFormatException exception) {
                return ResponseEntity.badRequest()
                                .body(Map.of("error", "Invalid course ID format"));
        }

        @ExceptionHandler(CourseAccessException.class)
        ResponseEntity<Map<String, String>> handleCourseAccessException(CourseAccessException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(CourseConflictException.class)
        ResponseEntity<Map<String, String>> handleCourseConflictException(CourseConflictException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(CourseworkNotFoundException.class)
        ResponseEntity<Map<String, String>> handleCourseworkNotFoundException(CourseworkNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Coursework not found"));
        }

        @ExceptionHandler(CourseworkAccessException.class)
        ResponseEntity<Map<String, String>> handleCourseworkAccessException(CourseworkAccessException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(SubmissionNotFoundException.class)
        ResponseEntity<Map<String, String>> handleSubmissionNotFoundException(SubmissionNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Submission not found"));
        }

        @ExceptionHandler(SubmissionAccessException.class)
        ResponseEntity<Map<String, String>> handleSubmissionAccessException(SubmissionAccessException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Submission access denied"));
        }

        @ExceptionHandler(SubmissionConflictException.class)
        ResponseEntity<Map<String, String>> handleSubmissionConflictException(SubmissionConflictException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(AttachmentNotFoundException.class)
        ResponseEntity<Map<String, String>> handleAttachmentNotFoundException(AttachmentNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Attachment not found"));
        }

        @ExceptionHandler(AttachmentAccessException.class)
        ResponseEntity<Map<String, String>> handleAttachmentAccessException(AttachmentAccessException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Attachment access denied"));
        }

        @ExceptionHandler(AttachmentConfigurationException.class)
        ResponseEntity<Map<String, String>> handleAttachmentConfigurationException(
                        AttachmentConfigurationException exception) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(Map.of("error", "Cloudinary storage is not configured"));
        }

        @ExceptionHandler(AttachmentConflictException.class)
        ResponseEntity<Map<String, String>> handleAttachmentConflictException(AttachmentConflictException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(AttachmentStorageException.class)
        ResponseEntity<Map<String, String>> handleAttachmentStorageException(AttachmentStorageException exception) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(CommentNotFoundException.class)
        ResponseEntity<Map<String, String>> handleCommentNotFoundException(CommentNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Comment target not found"));
        }

        @ExceptionHandler(CommentAccessException.class)
        ResponseEntity<Map<String, String>> handleCommentAccessException(CommentAccessException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Comment access denied"));
        }

        @ExceptionHandler(NotificationNotFoundException.class)
        ResponseEntity<Map<String, String>> handleNotificationNotFoundException(
                        NotificationNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Notification not found"));
        }

        @ExceptionHandler(NotificationAccessException.class)
        ResponseEntity<Map<String, String>> handleNotificationAccessException(NotificationAccessException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Notification access denied"));
        }

        @ExceptionHandler(AnalyticsNotFoundException.class)
        ResponseEntity<Map<String, String>> handleAnalyticsNotFoundException(AnalyticsNotFoundException exception) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Analytics data not found"));
        }

        @ExceptionHandler(AnalyticsAccessException.class)
        ResponseEntity<Map<String, String>> handleAnalyticsAccessException(AnalyticsAccessException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Analytics access denied"));
        }

        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        ResponseEntity<Map<String, String>> handleAccessDeniedException(
                        org.springframework.security.access.AccessDeniedException exception) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception) {
                String message = exception.getBindingResult().getFieldErrors().stream()
                                .findFirst()
                                .map(error -> error.getField() + " " + error.getDefaultMessage())
                                .orElse("Request validation failed");
                return ResponseEntity.badRequest().body(Map.of("error", message));
        }
}
