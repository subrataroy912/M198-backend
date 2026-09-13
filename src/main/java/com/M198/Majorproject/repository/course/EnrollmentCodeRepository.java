/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : EnrollmentCodeRepository
 * PURPOSE    : Stores and validates enrollment codes used for student course admission.
 *
 * This repository controls course access by checking whether an enrollment code is active,
 * valid for a specific course, and still within its allowed time window.
 */
package com.M198.Majorproject.repository.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.course.EnrollmentCode;

public interface EnrollmentCodeRepository extends MongoRepository<EnrollmentCode, String> {

    Optional<EnrollmentCode> findByCodeAndActiveTrue(String code);

    Optional<EnrollmentCode> findByCourseIdAndActiveTrue(String courseId);

    List<EnrollmentCode> findAllByCourseIdOrderByCreatedAtDesc(String courseId);

    List<EnrollmentCode> findAllByCourseIdInAndActiveTrue(java.util.Collection<String> courseIds);
}
