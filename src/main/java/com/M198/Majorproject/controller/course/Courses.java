/**
 * CREATED AT : 09/04/2026 (M-D-Y)
 * CREATED BY : SUBRATA ROY
 */

/*
    
     * ==========================================
     * API Functions inside the Courses Controller:
     * ==========================================
     * 1. createCourse         - Creates a new course room room (Anyone can do)
     * 2. getCourses          - Lists all courses the authenticated user belongs to
     * 3. getCourseById       - Retrieves detailed metadata for a specific course
     * 4. updateCourse        - Updates course details (Teacher/Owner only)
     * 5. deleteCourse        - Archive or delete a course (Teacher/Owner only)
     * 6. enrollInCourse      - Enroll a student using an enrollment code
     * 7. unenrollFromCourse  - Remove a student from a course
     * 8. getCourseRoster     - Fetch the class roster of teachers and students
 */
 /*
 * =================================================================================
 * COURSES CONTROLLER ENDPOINT DOCUMENTATION
 * =================================================================================
 *
 * 1. createCourse
 *    - HTTP Method: POST
 *    - Role Allowed: Anyone (or authenticated users looking to host a classroom)
 *    - How it works: Accepts metadata such as the course name, section, subject, 
 *      and description. The server generates a unique course ID and creates a new 
 *      classroom instance. It usually generates a unique enrollment code automatically.
 *    - Why it’s used: Allows educators or creators to set up a brand new virtual 
 *      classroom environment from scratch.
 *
 * 2. getCourses
 *    - HTTP Method: GET
 *    - Role Allowed: All authenticated users
 *    - How it works: Checks the identity of the logged-in user and queries the 
 *      database for any courses where they are listed as either an instructor, 
 *      teaching assistant, or enrolled student.
 *    - Why it’s used: Populates the user's primary dashboard or homepage with a 
 *      grid or list view of all their active classes.
 *
 * 3. getCourseById
 *    - HTTP Method: GET
 *    - Role Allowed: Members of the course (Teachers, TAs, and Enrolled Students)
 *    - How it works: Takes a specific courseId parameter, validates that the 
 *      requesting user has permission to view it, and returns the full metadata, 
 *      settings, syllabus information, or banner details for that course.
 *    - Why it’s used: Loads the landing page or "stream" view when a user clicks 
 *      on a specific course card from their dashboard.
 *
 * 4. updateCourse
 *    - HTTP Method: PUT or PATCH
 *    - Role Allowed: Teacher / Owner only
 *    - How it works: Receives updated fields for an existing classroom (such as 
 *      changing the course title, updating the description, or altering class hours) 
 *      and modifies the corresponding record in the database.
 *    - Why it’s used: Lets instructors fix typos, update class policies, or modify 
 *      room configurations mid-semester.
 *
 * 5. deleteCourse
 *    - HTTP Method: DELETE
 *    - Role Allowed: Teacher / Owner only
 *    - How it works: Processes a request to remove the course record. Depending on 
 *      system design, it either permanently wipes the course data or marks it as 
 *      archived/inactive so it no longer appears on active user dashboards.
 *    - Why it’s used: Cleans up old, expired, or accidentally created class rooms 
 *      at the end of an academic term.
 *
 * 6. enrollInCourse
 *    - HTTP Method: POST
 *    - Role Allowed: Students (or any authenticated user seeking entry)
 *    - How it works: The user provides a alphanumeric enrollment code. The server 
 *      matches this code to the corresponding class, adds the user's ID to the 
 *      student array or enrollment mapping table, and grants them access.
 *    - Why it’s used: Serves as the primary self-service entry point for students 
 *      joining a new class without requiring manual teacher invitations.
 *
 * 7. unenrollFromCourse
 *    - HTTP Method: DELETE or POST
 *    - Role Allowed: Enrolled Students (or Teachers removing a student)
 *    - How it works: Removes the link between a specific student ID and the course 
 *      ID in the registration table. The student loses immediate access to the 
 *      course materials and assignment boards.
 *    - Why it’s used: Handles situations where a student drops a class or leaves 
 *      a section, as well as cases where an unauthorized user needs to be kicked out.
 *
 * 8. getCourseRoster
 *    - HTTP Method: GET
 *    - Role Allowed: Enrolled members or administrators
 *    - How it works: Queries the enrollment database for the requested course ID 
 *      and aggregates a list of all instructors, teaching assistants, and active 
 *      students associated with the class.
 *    - Why it’s used: Generates the "People" tab in the user interface so class 
 *      members can view their peers, contact instructors, or audit classroom headcount.
 */
package com.M198.Majorproject.controller.course;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/courses")
public class Courses {

}
