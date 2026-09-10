# API Catalog

> **Status:** Authentication and the documented feature endpoint slices are
> implemented. Pagination, cloud-storage URL providers, and projection rebuild
> jobs remain infrastructure follow-up work.

All API routes use the `/v1` version prefix. Protected routes require an
authenticated user. Role names below are design intent; resource-level
authorization for the planned feature controllers is not implemented yet.

## Authentication

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/auth/register` | Public | Create a teacher or student account and issue JWTs |
| POST | `/v1/auth/login` | Public | Verify credentials and issue JWTs |
| POST | `/v1/auth/refresh` | Public with refresh token | Rotate a refresh token and issue new JWTs |
| POST | `/v1/auth/logout` | Authenticated | Revoke the supplied refresh token |
| GET | `/v1/auth/oauth/google` | Public | Start Google OAuth login/registration |
| GET | `/v1/auth/oauth/github` | Public | Start GitHub OAuth login/registration |

Password registration accepts `TEACHER` or `STUDENT` only. OAuth users default to
`STUDENT`. OAuth accounts are linked to an existing account only when the provider
returns a verified matching email. OAuth failures redirect to the configured
frontend callback with `error=oauth_failed`; the underlying reason is logged by
the API without being exposed to the browser. Access tokens are bearer JWTs; refresh tokens are rotated and stored
only as SHA-256 hashes in `auth_refresh_tokens`. Unauthenticated and forbidden
API requests return consistent JSON `401` and `403` responses.

## Courses and Admission

| Method | Route | Intended access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/courses` | Authenticated | Create a course and enrollment code |
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

The current-user endpoint returns the authenticated user's account and profile
fields, while public profile responses exclude email and account type. Profile
updates are partial and only modify profile fields; account identity, password,
account type, and status cannot be changed through this endpoint. Soft-deleted
profiles are not returned. `PUBLIC` profiles can be read by authenticated users,
but `PRIVATE` profiles are restricted to their owner. `COURSE_MEMBERS` visibility
is reserved until course-membership authorization is implemented. Profiles may
have an optional unique `handle` of up to 30 characters using letters, numbers,
and underscores.

---

### Explore

GET /v1/explore/feed

GET /v1/explore/courses/search

GET /v1/explore/recommendations
