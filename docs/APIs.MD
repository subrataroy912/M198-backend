 # API Catalog

> **Status:** Planned API contract. The current controllers expose only
> class-level `@RequestMapping` prefixes; no method-level endpoint is currently
> implemented.

All routes use the `/v1` version prefix. Protected routes will require an
authenticated user once security is implemented. Role names below are design
intent, not active authorization rules.

## Authentication

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/auth/register` | Public | Create a teacher or student account |
| POST | `/v1/auth/login` | Public | Verify credentials and issue a token |
| POST | `/v1/auth/logout` | Authenticated | Invalidate the current session |

## Courses and Admission

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/courses` | Teacher | Create a course and enrollment code |
| GET | `/v1/courses` | Authenticated | List the user's courses |
| GET | `/v1/courses/{courseId}` | Course member | View course details |
| PATCH | `/v1/courses/{courseId}` | Teacher or owner | Update course details |
| DELETE | `/v1/courses/{courseId}` | Teacher or owner | Archive or delete a course |
| POST | `/v1/courses/{courseId}/enrollment` | Student | Join using an enrollment code |
| DELETE | `/v1/courses/{courseId}/enrollment` | Student or teacher | Leave or remove course membership |
| GET | `/v1/courses/{courseId}/roster` | Course member or administrator | View the course roster |

## Coursework

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/courses/{courseId}/coursework` | Teacher or owner | Create an assignment, announcement, or material |
| GET | `/v1/courses/{courseId}/coursework` | Course member | List visible coursework |
| GET | `/v1/courses/{courseId}/coursework/{courseworkId}` | Course member | View one coursework item |
| PATCH | `/v1/courses/{courseId}/coursework/{courseworkId}` | Teacher or owner | Update coursework |
| DELETE | `/v1/courses/{courseId}/coursework/{courseworkId}` | Teacher or owner | Archive or delete coursework |

## Submissions and Grading

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/coursework/{courseworkId}/submissions` | Student | Start a draft submission |
| GET | `/v1/coursework/{courseworkId}/submissions` | Teacher | List submissions for grading |
| GET | `/v1/coursework/{courseworkId}/submissions/me` | Student | View the current student's submission |
| PATCH | `/v1/coursework/{courseworkId}/submissions/me` | Student | Save or turn in a submission |
| PATCH | `/v1/coursework/{courseworkId}/submissions/{submissionId}/grade` | Teacher | Grade and return feedback |

## Attachments

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/attachments/upload-url` | Authenticated | Request a temporary upload URL |
| GET | `/v1/attachments/{attachmentId}/download-url` | Authorized viewer | Request a temporary download URL |
| DELETE | `/v1/attachments/{attachmentId}` | Owner or administrator | Remove attachment metadata and file |

## Comments

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/coursework/{courseworkId}/comments` | Course member | Add a public coursework comment |
| GET | `/v1/coursework/{courseworkId}/comments` | Course member | List public coursework comments |
| POST | `/v1/submissions/{submissionId}/comments` | Student or teacher | Add private submission feedback |
| GET | `/v1/submissions/{submissionId}/comments` | Student or teacher | View private submission feedback |

## Analytics, Notifications, Profiles, and Explore

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| GET | `/v1/analytics/courses/{courseId}/summary` | Teacher or administrator | View course performance metrics |
| GET | `/v1/analytics/courses/{courseId}/students/{studentId}/gradebook` | Student for self or teacher | View a student's gradebook |
| GET | `/v1/notifications` | Authenticated | List the user's notifications |
| PATCH | `/v1/notifications/{notificationId}/read` | Notification owner | Mark a notification as read |
| GET | `/v1/notifications/settings` | Authenticated | Read alert settings |
| PATCH | `/v1/notifications/settings` | Authenticated | Update alert settings |
| GET | `/v1/users/me` | Authenticated | Read the current user's profile |
| GET | `/v1/users/{userId}` | Authenticated | Read a public user profile |
| PATCH | `/v1/users/me` | Authenticated | Update the current user's profile |
| GET | `/v1/explore/feed` | Public | Browse public courses |
| GET | `/v1/explore/courses/search` | Public | Search public courses |
| GET | `/v1/explore/recommendations` | Authenticated | View personalized recommendations |

## Contract Requirements

Future implementations should validate request bodies, use consistent error
responses, paginate list endpoints, enforce ownership and course membership,
avoid returning password hashes or private data, and define whether deletion is
hard deletion or archival.
### Authentication

POST /v1/auth/register

POST /v1/auth/login

POST /v1/auth/logout

---

### Courses

POST /v1/courses

GET /v1/courses

GET /v1/courses/{courseId}

PATCH /v1/courses/{courseId}

DELETE /v1/courses/{courseId}

POST /v1/courses/{courseId}/enrollment

DELETE /v1/courses/{courseId}/enrollment

GET /v1/courses/{courseId}/roster

---

### Coursework

POST /v1/courses/{courseId}/coursework

GET /v1/courses/{courseId}/coursework

GET /v1/courses/{courseId}/coursework/{courseworkId}

PATCH /v1/courses/{courseId}/coursework/{courseworkId}

DELETE /v1/courses/{courseId}/coursework/{courseworkId}

---

### Submissions

POST /v1/coursework/{courseworkId}/submissions

GET /v1/coursework/{courseworkId}/submissions

GET /v1/coursework/{courseworkId}/submissions/me

PATCH /v1/coursework/{courseworkId}/submissions/me

PATCH /v1/coursework/{courseworkId}/submissions/{submissionId}/grade

---

### Attachments

POST /v1/attachments/upload-url

GET /v1/attachments/{attachmentId}/download-url

DELETE /v1/attachments/{attachmentId}

---

### Comments

POST /v1/coursework/{courseworkId}/comments

GET /v1/coursework/{courseworkId}/comments

POST /v1/submissions/{submissionId}/comments

GET /v1/submissions/{submissionId}/comments

---

### Analytics

GET /v1/analytics/courses/{courseId}/summary

GET /v1/analytics/courses/{courseId}/students/{studentId}/gradebook

---

### Notifications

GET /v1/notifications

PATCH /v1/notifications/{notificationId}/read

GET /v1/notifications/settings

PATCH /v1/notifications/settings

---

### Profiles

GET /v1/users/me

GET /v1/users/{userId}

PATCH /v1/users/me

---

### Explore

GET /v1/explore/feed

GET /v1/explore/courses/search

GET /v1/explore/recommendations