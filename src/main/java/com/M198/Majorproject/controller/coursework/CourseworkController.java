/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
     * ==========================================
     * API Functions inside the Coursework Controller:
     * ==========================================
     * 1. createCoursework  - Creates an assignment, announcement, or material
     * 2. getAllCoursework   - Fetches paginated coursework for a course
     * 3. getCourseworkById  - Retrieves one coursework item by ID
     * 4. updateCoursework   - Updates coursework details for teachers or owners
     * 5. deleteCoursework   - Deletes or archives coursework for teachers or owners
 */
 /*
     * =================================================================================
     * COURSEWORK CONTROLLER ENDPOINT DOCUMENTATION
     * =================================================================================
     *
     * 1. createCoursework
     *    - Route: POST /v1/courses/{courseId}/coursework
     *    - Role Allowed: Course teacher or owner.
     *    - Request Body: Type (ASSIGNMENT, ANNOUNCEMENT, or MATERIAL), title,
     *      description, due date, maximum points, and attachment references.
     *    - How it works: Verifies teacher ownership, validates the payload, creates
     *      the coursework record, and optionally notifies enrolled students.
     *    - Response: Returns the created coursework item.
     *    - Why it's used: Adds a new item to the course stream.
     *
     * 2. getAllCoursework
     *    - Route: GET /v1/courses/{courseId}/coursework
     *    - Role Allowed: Enrolled course members and authorized teachers.
     *    - Request: Course ID plus optional page, size, type, and sort parameters.
     *    - How it works: Verifies membership and returns visible stream items in a
     *      paginated response, excluding unpublished or archived items when needed.
     *    - Response: Returns a page of coursework items.
     *    - Why it's used: Populates the course stream and assignment list.
     *
     * 3. getCourseworkById
     *    - Route: GET /v1/courses/{courseId}/coursework/{courseworkId}
     *    - Role Allowed: Enrolled course members and authorized teachers.
     *    - Request: Course ID and coursework ID path parameters.
     *    - How it works: Confirms that the coursework belongs to the course and that
     *      the caller may view it before returning its complete details.
     *    - Response: Returns one coursework item with attachments and settings.
     *    - Why it's used: Opens an assignment, announcement, or material detail page.
     *
     * 4. updateCoursework
     *    - Route: PATCH /v1/courses/{courseId}/coursework/{courseworkId}
     *    - Role Allowed: Course teacher or owner.
     *    - Request Body: Any editable coursework fields such as title, description,
     *      due date, points, publication state, or attachments.
     *    - How it works: Checks ownership, validates changed fields, updates the
     *      record, and sends a notification when a relevant change is published.
     *    - Response: Returns the updated coursework item.
     *    - Why it's used: Corrects or changes course work after it is created.
     *
     * 5. deleteCoursework
     *    - Route: DELETE /v1/courses/{courseId}/coursework/{courseworkId}
     *    - Role Allowed: Course teacher or owner.
     *    - Request: Course ID and coursework ID path parameters.
     *    - How it works: Checks ownership and removes or archives the item according
     *      to the application's retention policy, including related references.
     *    - Response: Returns no content after successful deletion or archival.
     *    - Why it's used: Removes an obsolete or mistakenly created stream item.
 */
package com.M198.Majorproject.controller.coursework;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/courses/{courseId}/coursework")
public class CourseworkController {

}
