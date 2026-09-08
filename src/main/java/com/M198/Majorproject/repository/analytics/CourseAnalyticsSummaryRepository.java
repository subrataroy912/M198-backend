package com.M198.Majorproject.repository.analytics;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.analytics.CourseAnalyticsSummary;

public interface CourseAnalyticsSummaryRepository extends MongoRepository<CourseAnalyticsSummary, String> {

    Optional<CourseAnalyticsSummary> findByCourseId(String courseId);
}
