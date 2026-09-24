package com.M198.Majorproject.discovery.explore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.discovery.explore.entity.CourseDiscovery;

@Repository
public interface CourseDiscoveryRepository extends MongoRepository<CourseDiscovery, String> {

        Optional<CourseDiscovery> findByCourseId(String courseId);

        void deleteByCourseId(String courseId);

        // Just find public/private active courses
        @Query("{ 'visibility' : { $in: ?0 }, 'status' : ?1 }")
        Page<CourseDiscovery> findFeed(
                        List<CourseVisibility> allowedVisibilities,
                        CourseStatus status,
                        Pageable pageable);

        // Filter by subject
        @Query("{ 'subject' : ?0, 'visibility' : { $in: ?1 }, 'status' : ?2 }")
        Page<CourseDiscovery> findFeedBySubject(
                        String subject,
                        List<CourseVisibility> allowedVisibilities,
                        CourseStatus status,
                        Pageable pageable);

        // Search by title (regex with 'i' for ignore case)
        @Query("{ 'title' : { $regex: ?0, $options: 'i' }, 'visibility' : { $in: ?1 }, 'status' : ?2 }")
        Page<CourseDiscovery> searchFeedByTitle(
                        String title,
                        List<CourseVisibility> visibilities,
                        CourseStatus status,
                        Pageable pageable);
}
