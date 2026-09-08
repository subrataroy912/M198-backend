package com.M198.Majorproject.repository.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.course.EnrollmentCode;

public interface EnrollmentCodeRepository extends MongoRepository<EnrollmentCode, String> {

    Optional<EnrollmentCode> findByCodeAndActiveTrue(String code);

    Optional<EnrollmentCode> findByCourseIdAndActiveTrue(String courseId);

    List<EnrollmentCode> findAllByCourseIdOrderByCreatedAtDesc(String courseId);
}
