/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
     * ==========================================
     * API Functions inside the Explore Controller:
     * ==========================================
     * 1. getExploreFeed        - Returns public trending and newly launched courses
     * 2. searchPublicCourses   - Searches public courses by keyword, tag, or subject
     * 3. getRecommendedCourses - Recommends courses using the user's learning history
 */
 /*
     * =================================================================================
     * EXPLORE CONTROLLER ENDPOINT DOCUMENTATION
     * =================================================================================
     *
     * 1. getExploreFeed
     *    - Route: GET /v1/explore/feed
     *    - Role Allowed: Public users, with additional personalized data for logged-in users.
     *    - Request: Optional page, size, category, subject, and sort query parameters.
     *    - How it works: Selects public courses using configured popularity, recency,
     *      and visibility rules without exposing private classroom data.
     *    - Response: Returns a paginated list of public course summaries.
     *    - Why it's used: Gives users a starting point for discovering available courses.
     *
     * 2. searchPublicCourses
     *    - Route: GET /v1/explore/courses/search?q={query}
     *    - Role Allowed: Public users.
     *    - Request: Search text plus optional subject, tag, page, size, and sort values.
     *    - How it works: Searches only courses marked public and applies pagination and
     *      filtering before returning results.
     *    - Response: Returns matching public course summaries and result metadata.
     *    - Why it's used: Helps users find classrooms by keyword, subject, or tag.
     *
     * 3. getRecommendedCourses
     *    - Route: GET /v1/explore/recommendations
     *    - Role Allowed: Authenticated users only.
     *    - Request: The user identity comes from the authenticated security context;
     *      optional page and size parameters control the result set.
     *    - How it works: Uses permitted learning history, enrolled courses, and stated
     *      interests to rank public course recommendations.
     *    - Response: Returns a paginated list of recommended public courses.
     *    - Why it's used: Provides personalized discovery results.
 */
package com.M198.Majorproject.controller.explore;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/explore")
public class ExploreController {

}
