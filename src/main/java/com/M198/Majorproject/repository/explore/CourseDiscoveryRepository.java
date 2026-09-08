package com.M198.Majorproject.repository.explore;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.course.CourseStatus;
import com.M198.Majorproject.entity.course.CourseVisibility;
import com.M198.Majorproject.entity.explore.CourseDiscovery;

public interface CourseDiscoveryRepository extends MongoRepository<CourseDiscovery, String> {

    Optional<CourseDiscovery> findByCourseId(String courseId);

    Page<CourseDiscovery> findAllByVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
            CourseVisibility visibility, CourseStatus status, Pageable pageable);

    Page<CourseDiscovery> findAllBySubjectAndVisibilityAndStatusOrderByPopularityScoreDescLastActivityAtDesc(
            String subject, CourseVisibility visibility, CourseStatus status, Pageable pageable);

    Page<CourseDiscovery> findAllByTitleContainingIgnoreCaseAndVisibilityAndStatusOrderByPopularityScoreDesc(
            String title, CourseVisibility visibility, CourseStatus status, Pageable pageable);
}
