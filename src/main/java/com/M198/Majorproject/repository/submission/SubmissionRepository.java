/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : SubmissionRepository
 * PURPOSE    : Fetches student submissions, status-based records, and coursework submission history.
 *
 * This repository supports the grading workflow by retrieving student work in a structured manner,
 * grouped by coursework, student, and pass/fail or pending state.
 */
package com.M198.Majorproject.repository.submission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.submission.Submission;
import com.M198.Majorproject.entity.submission.SubmissionStatus;

public interface SubmissionRepository extends MongoRepository<Submission, String> {

    Optional<Submission> findByCourseworkIdAndStudentId(String courseworkId, String studentId);

    Optional<Submission> findByCourseworkIdAndStudentIdAndStatus(
            String courseworkId, String studentId, SubmissionStatus status);

        Page<Submission> findAllByCourseworkIdOrderByCreatedAtAsc(String courseworkId, Pageable pageable);

    List<Submission> findAllByCourseworkIdAndStatusOrderByCreatedAtAsc(
            String courseworkId, SubmissionStatus status);

    List<Submission> findAllByStudentIdAndCourseIdOrderByCreatedAtDesc(String studentId, String courseId);
}
