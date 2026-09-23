package com.M198.Majorproject.core.course.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.core.course.entity.Coursework;
import com.M198.Majorproject.core.course.entity.CourseworkStatus;
import com.M198.Majorproject.core.course.entity.CourseworkType;

public interface CourseworkRepository extends MongoRepository<Coursework, String> {

    /** Students: only PUBLISHED, pinned first, newest published first. */
    Page<Coursework> findAllByCourseIdAndStatusOrderByPinnedDescPublishedAtDesc(
            String courseId, CourseworkStatus status, Pageable pageable);

    /** Staff: everything except ARCHIVED, pinned first, newest published first, drafts by createdAt. */
    Page<Coursework> findAllByCourseIdAndStatusNotOrderByPinnedDescPublishedAtDescCreatedAtDesc(
            String courseId, CourseworkStatus status, Pageable pageable);

    /** Students: only PUBLISHED, newest published first. */
    Page<Coursework> findAllByCourseIdAndStatusOrderByPublishedAtDesc(
            String courseId, CourseworkStatus status, Pageable pageable);

    /** Staff: everything except ARCHIVED (PUBLISHED + DRAFT), newest published first, drafts by createdAt. */
    Page<Coursework> findAllByCourseIdAndStatusNotOrderByPublishedAtDescCreatedAtDesc(
            String courseId, CourseworkStatus status, Pageable pageable);

    List<Coursework> findAllByCourseIdAndTypeAndStatusOrderByPublishedAtDesc(
            String courseId, CourseworkType type, CourseworkStatus status);

    Optional<Coursework> findByIdAndCourseId(String id, String courseId);

        Optional<Coursework> findByIdAndStatus(String id, CourseworkStatus status);

    Optional<Coursework> findByIdAndCourseIdAndStatus(String id, String courseId, CourseworkStatus status);

    List<Coursework> findAllByCourseId(String courseId);

    void deleteAllByCourseId(String courseId);
}
