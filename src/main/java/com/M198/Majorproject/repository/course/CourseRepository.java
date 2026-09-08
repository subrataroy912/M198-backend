package com.M198.Majorproject.repository.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.course.Course;
import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;

public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findAllByOwnerIdAndStatus(String ownerId, CourseStatus status);

    List<Course> findAllByVisibilityAndStatus(CourseVisibility visibility, CourseStatus status);

    Optional<Course> findByIdAndStatus(String id, CourseStatus status);
}
