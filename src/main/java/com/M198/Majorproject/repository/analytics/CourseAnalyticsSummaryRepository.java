/**
 * CREATED BY : SUBRATA ROY
 * REPOSITORY : CourseAnalyticsSummaryRepository
 * PURPOSE    : Retrieves course-level analytics summaries for teacher and academic overview screens.
 *
 * This repository keeps grade and performance summary records available for analytics endpoints,
 * dashboards, and progress reporting features.
 */
package com.M198.Majorproject.repository.analytics;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.analytics.CourseAnalyticsSummary;

public interface CourseAnalyticsSummaryRepository extends MongoRepository<CourseAnalyticsSummary, String> {

    Optional<CourseAnalyticsSummary> findByCourseId(String courseId);
}
