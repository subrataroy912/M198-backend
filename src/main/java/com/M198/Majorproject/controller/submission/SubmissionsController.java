/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */
/*
     * ==========================================
     * API Functions inside the Submissions Controller:
     * ==========================================
     * 1. initializeSubmission - Creates a blank draft submission entry for an assignment (Students only)
     * 2. getAllSubmissions   - Fetches all student submissions for a specific assignment (Teachers only)
     * 3. getMySubmission     - Allows a student to view their own submission status and file details
     * 4. updateSubmission     - Allows students to update attachments or change states (e.g., TURNED_IN, DRAFT)
     * 5. gradeSubmission      - Assigns a score, leaves a teacher comment, and returns the work (Teachers only)
 */

 /*
 * =================================================================================
 * SUBMISSIONS CONTROLLER ENDPOINT DOCUMENTATION
 * =================================================================================
 *
 * 1. initializeSubmission
 *    - HTTP Method: POST
 *    - Role Allowed: Students only
 *    - How it works: When a student clicks "Start Assignment" or opens the 
 *      assignment page for the first time, this endpoint runs. It creates a new, 
 *      blank row in the database linked to that student and that assignment ID.
 *    - State/Status: The submission status is set to DRAFT.
 *    - Why it’s used: It acts as a placeholder so the system can track that the 
 *      student has initiated the work, even if they haven't uploaded files or 
 *      typed anything yet.
 *
 * 2. getAllSubmissions
 *    - HTTP Method: GET
 *    - Role Allowed: Teachers / Instructors only
 *    - How it works: The teacher passes an assignmentId to this endpoint. The 
 *      server queries the database and fetches a complete list (array) of every 
 *      student's submission for that specific assignment.
 *    - Returned Data: It returns student names, submission dates, file links, 
 *      and current statuses (e.g., who has submitted, who is still working on a 
 *      draft, and who has been graded).
 *    - Why it’s used: This populates the teacher's grading dashboard or 
 *      submission list view.
 *
 * 3. getMySubmission
 *    - HTTP Method: GET
 *    - Role Allowed: Students only
 *    - How it works: When a student opens an assignment they have already started, 
 *      this endpoint fetches only their specific record based on their logged-in 
 *      user ID and the assignmentId.
 *    - Returned Data: It returns their current status (DRAFT, TURNED_IN, GRADED), 
 *      any files they previously uploaded, and any feedback or grades left by 
 *      the teacher.
 *    - Why it’s used: It ensures privacy so students cannot see other 
 *      classmates' files or grades, only their own.
 *
 * 4. updateSubmission
 *    - HTTP Method: PUT or PATCH
 *    - Role Allowed: Students only
 *    - How it works: This handles changes to an existing submission. A student 
 *      uses it to upload files, delete attachments, or modify text answers. 
 *      Crucially, this endpoint also changes the submission state (e.g., moving 
 *      it from DRAFT to TURNED_IN when they click the "Submit" button).
 *    - Why it’s used: It is the core endpoint for saving progress and finalizing 
 *      the hand-in process.
 *
 * 5. gradeSubmission
 *    - HTTP Method: PUT or POST
 *    - Role Allowed: Teachers / Instructors only
 *    - How it works: A teacher sends a payload containing the student's 
 *      submissionId, a numeric score (e.g., 95/100), and optional text feedback 
 *      (comment). The server updates the database row, changes the status to 
 *      GRADED (or RETURNED), and often triggers a notification to the student.
 *    - Why it’s used: It closes the assignment lifecycle, finalizing the 
 *      student's grade and releasing feedback to them.
 */
package com.M198.Majorproject.controller.submission;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/coursework/{courseworkId}/submissions")
public class SubmissionsController {

}
