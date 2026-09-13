package com.M198.Majorproject.explore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.course.entity.CourseStatus;
import com.M198.Majorproject.course.entity.CourseVisibility;
import com.M198.Majorproject.explore.entity.CourseDiscovery;

public interface CourseDiscoveryRepository extends MongoRepository<CourseDiscovery, String> {

    Optional<CourseDiscovery> findByCourseId(String courseId);

    Page<CourseDiscovery> findAllByVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
            CourseVisibility visibility, CourseStatus status, Pageable pageable);

    Page<CourseDiscovery> findAllBySubjectAndVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
            String subject, CourseVisibility visibility, CourseStatus status, Pageable pageable);

    Page<CourseDiscovery> findAllByTitleContainingIgnoreCaseAndVisibilityAndStatusOrderByPopularityScoreDesc(
            String title, CourseVisibility visibility, CourseStatus status, Pageable pageable);

    void deleteByCourseId(String courseId);
}
