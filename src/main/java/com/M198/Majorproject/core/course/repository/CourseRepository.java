/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : CourseRepository
 * PURPOSE    : Provides MongoDB access for course records and active course queries.
 *
 * This repository keeps the service layer focused on business rules while handling
 * course lookup and status-based filtering directly against MongoDB.
 */
package com.M198.Majorproject.core.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseStatus;

public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findAllByOwnerIdAndStatus(String ownerId, CourseStatus status);

    Optional<Course> findByIdAndStatus(String id, CourseStatus status);

    List<Course> findAllByIdInAndStatus(java.util.Collection<String> ids, CourseStatus status);
 
    long countByOwnerId(String ownerId);

    long countByOwnerIdAndStatus(String ownerId, CourseStatus status);
}
