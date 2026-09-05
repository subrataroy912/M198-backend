/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
	 * ==========================================
	 * API Functions inside the Comments Controller:
	 * ==========================================
	 * 1. createClassComment   - Adds a public comment to a coursework item
	 * 2. getClassComments      - Fetches public comments for a coursework item
	 * 3. createPrivateComment  - Adds private feedback to a submission
	 * 4. getPrivateComments    - Fetches private comments for a submission
 */

/*
 * =================================================================================
 * COMMENTS CONTROLLER ENDPOINT DOCUMENTATION
 * =================================================================================
 *
 * 1. createClassComment
 *    - Route: POST /v1/coursework/{courseworkId}/comments
 *    - Role Allowed: Authenticated course members.
 *    - Request Body: The comment text and, when supported, attachment references.
 *    - How it works: Verifies that the caller belongs to the course, creates a
 *      public comment for the coursework item, and records its author and time.
 *    - Response: Returns the created comment with its identifier and author data.
 *    - Why it's used: Allows course members to discuss an assignment or material.
 *
 * 2. getClassComments
 *    - Route: GET /v1/coursework/{courseworkId}/comments
 *    - Role Allowed: Authenticated course members.
 *    - Request: Coursework ID plus optional pagination and sorting parameters.
 *    - How it works: Checks course membership and returns public comments in a
 *      stable order, normally newest first or oldest first as requested.
 *    - Response: Returns a paginated list of public comments.
 *    - Why it's used: Displays the discussion attached to a coursework item.
 *
 * 3. createPrivateComment
 *    - Route: POST /v1/submissions/{submissionId}/comments
 *    - Role Allowed: The submitting student and the teacher responsible for grading.
 *    - Request Body: Private feedback text and optional attachment references.
 *    - How it works: Verifies that the caller is part of the student-teacher pair,
 *      stores the message as private, and notifies the other participant.
 *    - Response: Returns the created private comment.
 *    - Why it's used: Supports feedback that must not be visible to the whole class.
 *
 * 4. getPrivateComments
 *    - Route: GET /v1/submissions/{submissionId}/comments
 *    - Role Allowed: The submitting student and the authorized teacher.
 *    - Request: Submission ID plus optional pagination parameters.
 *    - How it works: Verifies access to the submission and returns only the private
 *      conversation associated with that submission.
 *    - Response: Returns a paginated private comment history.
 *    - Why it's used: Lets both authorized participants review submission feedback.
 */
package com.M198.Majorproject.controller.comment;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class CommentsController {

}
