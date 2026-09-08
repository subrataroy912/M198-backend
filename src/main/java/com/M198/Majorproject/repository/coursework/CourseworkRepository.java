package com.M198.Majorproject.repository.coursework;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.coursework.Coursework;
import com.M198.Majorproject.entity.coursework.CourseworkStatus;
import com.M198.Majorproject.entity.coursework.CourseworkType;

public interface CourseworkRepository extends MongoRepository<Coursework, String> {

    Page<Coursework> findAllByCourseIdAndStatusOrderByPublishedAtDesc(
            String courseId, CourseworkStatus status, Pageable pageable);

    List<Coursework> findAllByCourseIdAndTypeAndStatusOrderByPublishedAtDesc(
            String courseId, CourseworkType type, CourseworkStatus status);

    Optional<Coursework> findByIdAndCourseId(String id, String courseId);

        Optional<Coursework> findByIdAndStatus(String id, CourseworkStatus status);

    Optional<Coursework> findByIdAndCourseIdAndStatus(String id, String courseId, CourseworkStatus status);
}
