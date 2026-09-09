# Frontend Task: Wire the Classroom UI to the Existing Majorproject API

## Objective

Implement the frontend API client, authenticated session handling, and screens
that consume the backend **as it exists today**. Do not invent REST routes,
response properties, pagination shapes, or error formats. The backend base URL
is supplied by the frontend environment (for example,
`VITE_API_BASE_URL=https://api.example.com`); every route below is relative to
that URL and already includes the `/v1` prefix.

Use `application/json` for JSON requests. Send the access token on every route
marked **auth**:

```http
Authorization: Bearer <accessToken>
Accept: application/json
Content-Type: application/json
```

Do not send an `Authorization` header to login, registration, refresh, or the
OAuth-start routes. All timestamps are JSON ISO-8601 strings (for example,
`"2026-09-09T12:00:00Z"`). All enum values are uppercase strings.

## Required deliverables

1. Create one typed API client with a base-URL environment variable, JSON
   parsing, bearer-token injection, normalized errors, and support for `204 No
   Content` responses.
2. Create typed models from the exact JSON contracts below. Do not use database
   entities directly in the UI; use these request/response contracts.
3. Implement session state with access and refresh tokens, logout, and a
   one-time refresh-and-retry flow after a `401` response. If refresh fails,
   clear the session and route to sign-in.
4. Wire registration, password login, OAuth callback, profiles, courses,
   coursework, submissions, comments, attachments, notifications, analytics,
   and explore screens to the listed endpoints.
5. Show server `error` text in form and action feedback. For `400` validation
   responses, associate the message with the relevant field when possible.
6. Use the existing pagination response rather than assuming arrays for
   coursework, submissions, notifications, feed, search, or recommendations.
7. Do not expose Cloudinary API secrets in the frontend. The backend only sends
   short-lived upload parameters; upload the file directly using those values.

## Session, OAuth, and common behavior

### Password authentication

- `POST /v1/auth/register` creates a user and returns `201` with an auth
  response. Public registration accepts only `TEACHER` and `STUDENT`; do not
  offer `ADMIN` in the registration form.
- `POST /v1/auth/login` and `POST /v1/auth/refresh` return `200` with the same
  auth response.
- `POST /v1/auth/logout` is authenticated and returns `204`. It requires the
  refresh token in its JSON body; clear local client session state regardless of
  whether the request succeeds.

```json
// AuthResponse: returned by register, login, and refresh
{
  "accessToken": "jwt-access-token",
  "refreshToken": "opaque-refresh-token",
  "userId": "mongo-user-id",
  "email": "student@example.com",
  "displayName": "Ada Lovelace",
  "avatarUrl": "https://..."
}
```

```json
// RegisterUserRequest
{
  "email": "student@example.com",
  "password": "at-least-8-characters",
  "accountType": "STUDENT",
  "firstName": "Ada",
  "lastName": "Lovelace"
}

// LoginRequest
{ "email": "student@example.com", "password": "at-least-8-characters" }

// RefreshTokenRequest and LogoutRequest
{ "refreshToken": "opaque-refresh-token" }
```

### OAuth

Start browser navigation, rather than an XHR/fetch request, at one of:

- `GET /v1/auth/oauth/google`
- `GET /v1/auth/oauth/github`

The backend completes provider login and redirects to the configured frontend
callback URL with either:

```text
/auth/callback?accessToken=<url-encoded-token>&refreshToken=<url-encoded-token>
```

or:

```text
/auth/callback?error=oauth_failed
```

The callback page must URL-decode and persist both tokens, remove them from the
visible URL with `history.replaceState`, then load `/v1/users/me`. On
`error=oauth_failed`, show a generic sign-in failure; the backend deliberately
does not reveal provider details in the URL.

### Error contract

Handled application and security failures are JSON objects with exactly an
`error` string:

```json
{ "error": "Authentication required" }
```

Use the HTTP status first and show `body.error` when present. Do not parse
errors as success models. The expected handled statuses are:

| Status | Meaning and examples |
| --- | --- |
| `400` | Invalid input. Examples: `email Must be a valid email format`, `page must be non-negative and size must be between 1 and 100`, `COURSE_MEMBERS visibility is not available yet`. |
| `401` | Missing, expired, or invalid authentication: `Authentication required`, `Invalid credentials`, or `Invalid token`. |
| `403` | Caller lacks access: `Access denied`, `Course staff role required`, `Submission access denied`, `Attachment access denied`. |
| `404` | Missing resource: `Course not found`, `Coursework not found`, `Submission not found`, `Attachment not found`, `Profile not found`, `Notification not found`, or `Analytics data not found`. |
| `409` | Conflict: `Email is already registered`, `Handle is already taken`, `User already has membership in this course`, `Submission already exists`, or workflow conflicts such as `Only turned-in submissions can be graded`. |
| `502` | Cloudinary storage operation failed. |
| `503` | Cloudinary is not configured: `Cloudinary storage is not configured`. |

A successful archive/delete/leave/logout response has no body (`204`). Treat
network errors, non-JSON proxy errors, and unhandled `5xx` responses as a
separate generic “service unavailable” state.

## Shared JSON formats

Use `null` only where a value has not been populated; some fields are omitted
by request objects because PATCH requests are partial. These are response
formats, not input models.

```json
// PageResponse<T> — used by coursework, submissions, notifications, and explore
{
  "content": [/* T */],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false
}

// CourseResponse
{
  "id": "course-id",
  "ownerId": "user-id",
  "title": "Calculus I",
  "section": "A",
  "subject": "Mathematics",
  "description": "Limits and derivatives",
  "coverUrl": "https://...",
  "visibility": "PRIVATE",
  "status": "ACTIVE",
  "enrollmentEnabled": true,
  "enrollmentCode": "ABCD1234",
  "createdAt": "2026-09-09T12:00:00Z",
  "updatedAt": "2026-09-09T12:00:00Z"
}

// CourseworkResponse
{
  "id": "coursework-id", "courseId": "course-id", "creatorId": "user-id",
  "type": "ASSIGNMENT", "title": "Problem set 1", "description": "...",
  "status": "PUBLISHED", "publishedAt": "2026-09-09T12:00:00Z",
  "dueAt": "2026-09-16T23:59:00Z", "maximumPoints": 100,
  "createdAt": "2026-09-09T12:00:00Z", "updatedAt": "2026-09-09T12:00:00Z"
}

// SubmissionResponse
{
  "id": "submission-id", "courseworkId": "coursework-id", "courseId": "course-id",
  "studentId": "user-id", "status": "TURNED_IN", "answerText": "My answer",
  "submittedAt": "2026-09-10T12:00:00Z", "returnedAt": null, "late": false,
  "score": 95.5, "graderId": "teacher-id", "feedback": "Well done",
  "gradedAt": "2026-09-11T12:00:00Z"
}

// CommentResponse
{
  "id": "comment-id", "courseId": "course-id", "authorId": "user-id",
  "targetId": "coursework-or-submission-id", "visibility": "PUBLIC",
  "body": "Question about item 3", "createdAt": "2026-09-09T12:00:00Z",
  "editedAt": null
}

// AttachmentResponse
{
  "id": "attachment-id", "courseId": "course-id", "resourceType": "SUBMISSION",
  "resourceId": "submission-id", "ownerId": "user-id", "originalFilename": "answer.pdf",
  "contentType": "application/pdf", "sizeBytes": 42000, "status": "PENDING",
  "uploadUrl": "https://api.cloudinary.com/...", "publicId": "pending/...",
  "uploadApiKey": "...", "uploadSignature": "...", "uploadTimestamp": 1725883200,
  "downloadUrl": null, "createdAt": "2026-09-09T12:00:00Z"
}
```

## Endpoint worklist

### Profiles

All profile routes require auth.

| Method and route | Request | Success response | UI notes |
| --- | --- | --- | --- |
| `GET /v1/users/me` | — | `UserProfileResponse` | Call after authentication to hydrate account state. |
| `GET /v1/users/{userId}` | — | `PublicUserProfileResponse` | Do not expect `email` or `accountType`. |
| `PATCH /v1/users/me` | partial `UpdateUserProfileRequest` JSON **or** multipart form data | `UserProfileResponse` | Multipart uses part `profile` for the profile payload and optional `avatarFile` / `bannerFile` file parts. |

```json
// UserProfileResponse (GET/PATCH /me)
{
  "id": "user-id", "email": "student@example.com", "accountType": "STUDENT",
  "handle": "ada", "firstName": "Ada", "lastName": "Lovelace",
  "displayName": "Ada Lovelace", "avatarUrl": "https://...", "bannerUrl": "https://...",
  "headline": "Learner", "about": "...", "city": "London", "country": "GB",
  "profileVisibility": "PUBLIC", "gradeLevel": "Year 12"
}

// PublicUserProfileResponse (GET /{userId})
{
  "id": "user-id", "handle": "ada", "firstName": "Ada", "lastName": "Lovelace",
  "displayName": "Ada Lovelace", "avatarUrl": "https://...", "bannerUrl": "https://...",
  "headline": "Learner", "about": "...", "city": "London", "country": "GB",
  "profileVisibility": "PUBLIC", "gradeLevel": "Year 12"
}

// partial UpdateUserProfileRequest; send only changed properties
{
  "handle": "ada", "displayName": "Ada Lovelace", "headline": "Learner",
  "about": "...", "avatarUrl": "https://...", "bannerUrl": "https://...",
  "city": "London", "country": "GB", "gradeLevel": "Year 12",
  "profileVisibility": "PUBLIC"
}
```

`handle` permits only letters, numbers, and underscores (maximum 30). JSON
aliases accepted by the backend are `name` → `displayName`, `bio` → `about`,
`avatar` → `avatarUrl`, `banner` → `bannerUrl`, and `batchYear` → `gradeLevel`.
Do not present `COURSE_MEMBERS` as an option: the backend currently rejects it.

### Courses and membership

All routes require auth. Render controls based on the returned data and handle
`403`; the frontend must not assume client-side role checks are sufficient.

| Method and route | Request JSON | Success |
| --- | --- | --- |
| `POST /v1/courses` | `CreateCourseRequest` | `200 CourseResponse` |
| `POST /v1/courses/cover-upload` | none | `200 CourseCoverUploadResponse` |
| `GET /v1/courses` | none | `200 CourseResponse[]` |
| `GET /v1/courses/{courseId}` | none | `200 CourseResponse` |
| `PATCH /v1/courses/{courseId}` | partial `UpdateCourseRequest` | `200 CourseResponse` |
| `DELETE /v1/courses/{courseId}` | none | `204` |
| `GET /v1/courses/{courseId}/roster` | none | `200 CourseMemberResponse[]` |
| `POST /v1/courses/{courseId}/enrollment` | `{"code":"ABCD1234"}` | `200 CourseResponse` |
| `DELETE /v1/courses/{courseId}/enrollment` | none | `204` |

```json
// CreateCourseRequest
{
  "title": "Calculus I", "section": "A", "subject": "Mathematics",
  "description": "Limits and derivatives", "visibility": "PRIVATE",
  "coverUrl": "https://..."
}

// partial UpdateCourseRequest
{
  "title": "Calculus I", "section": "A", "subject": "Mathematics",
  "description": "...", "visibility": "PUBLIC", "enrollmentEnabled": true
}

// CourseMemberResponse
{ "userId": "user-id", "role": "STUDENT", "joinedAt": "2026-09-09T12:00:00Z" }

// CourseCoverUploadResponse
{
  "uploadUrl": "https://api.cloudinary.com/...", "publicId": "course-covers/...",
  "uploadApiKey": "...", "uploadSignature": "...", "uploadTimestamp": 1725883200
}
```

### Coursework and submissions

All routes require auth. List routes accept `page` (default `0`) and `size`
(default `20`, valid range `1`–`100`) and return `PageResponse`.

| Method and route | Request JSON | Success |
| --- | --- | --- |
| `POST /v1/courses/{courseId}/coursework` | `CreateCourseworkRequest` | `200 CourseworkResponse` |
| `GET /v1/courses/{courseId}/coursework?page=0&size=20` | none | `200 PageResponse<CourseworkResponse>` |
| `GET /v1/courses/{courseId}/coursework/{courseworkId}` | none | `200 CourseworkResponse` |
| `PATCH /v1/courses/{courseId}/coursework/{courseworkId}` | partial `UpdateCourseworkRequest` | `200 CourseworkResponse` |
| `DELETE /v1/courses/{courseId}/coursework/{courseworkId}` | none | `204` |
| `POST /v1/coursework/{courseworkId}/submissions` | none | `200 SubmissionResponse` |
| `GET /v1/coursework/{courseworkId}/submissions?page=0&size=20` | none | `200 PageResponse<SubmissionResponse>` |
| `GET /v1/coursework/{courseworkId}/submissions/me` | none | `200 SubmissionResponse` |
| `PATCH /v1/coursework/{courseworkId}/submissions/me` | partial `UpdateSubmissionRequest` | `200 SubmissionResponse` |
| `PATCH /v1/coursework/{courseworkId}/submissions/{submissionId}/grade` | `GradeSubmissionRequest` | `200 SubmissionResponse` |

```json
// CreateCourseworkRequest
{
  "type": "ASSIGNMENT", "title": "Problem set 1", "description": "...",
  "dueAt": "2026-09-16T23:59:00Z", "maximumPoints": 100
}
// For ANNOUNCEMENT and MATERIAL, omit dueAt and maximumPoints.

// partial UpdateCourseworkRequest
{
  "title": "Problem set 1", "description": "...", "dueAt": "2026-09-16T23:59:00Z",
  "maximumPoints": 100, "status": "PUBLISHED"
}

// partial UpdateSubmissionRequest
{ "answerText": "My answer", "status": "TURNED_IN" }

// GradeSubmissionRequest
{ "score": 95.5, "feedback": "Well done" }
```

Only published assignments accept submissions. Do not offer submissions for
announcements or materials. Students may only set `DRAFT` or `TURNED_IN`; a
graded submission cannot be edited. Only staff can grade, and only a
`TURNED_IN` submission can be graded.

### Comments and attachments

All routes require auth.

| Method and route | Request | Success |
| --- | --- | --- |
| `POST /v1/coursework/{courseworkId}/comments` | `{"body":"..."}` | `200 CommentResponse` |
| `GET /v1/coursework/{courseworkId}/comments` | none | `200 CommentResponse[]` |
| `POST /v1/submissions/{submissionId}/comments` | `{"body":"..."}` | `200 CommentResponse` |
| `GET /v1/submissions/{submissionId}/comments` | none | `200 CommentResponse[]` |
| `POST /v1/attachments/upload-url` | `CreateAttachmentRequest` | `200 AttachmentResponse` with upload data |
| `POST /v1/attachments/{attachmentId}/complete` | `CompleteAttachmentRequest` | `200 AttachmentResponse` |
| `GET /v1/attachments?resourceId={id}&resourceType=COURSEWORK` | none | `200 AttachmentResponse[]` |
| `GET /v1/attachments/{attachmentId}/download-url` | none | `200 AttachmentResponse` with `downloadUrl` |
| `DELETE /v1/attachments/{attachmentId}` | none | `204` |

```json
// CreateAttachmentRequest
{
  "resourceType": "SUBMISSION", "resourceId": "submission-id",
  "originalFilename": "answer.pdf", "contentType": "application/pdf", "sizeBytes": 42000
}

// CompleteAttachmentRequest
{ "publicId": "pending/generated-id", "sizeBytes": 42000 }
```

Attachment workflow: (1) request upload data, (2) upload the selected file
straight to the returned Cloudinary `uploadUrl` using the returned `publicId`,
`uploadApiKey`, `uploadSignature`, and `uploadTimestamp`, (3) call `complete`
with the same `publicId` and the file byte count, then (4) refresh the resource
attachment list. Do not mark a file attached until step 3 returns `UPLOADED`.
`resourceType` is only `COURSEWORK` or `SUBMISSION`.

### Notifications, analytics, and explore

All of these routes require auth. Explore feed and search are still protected
by the security configuration, even though they are discovery endpoints.

| Method and route | Request/query | Success response |
| --- | --- | --- |
| `GET /v1/notifications?unreadOnly=false&page=0&size=20` | optional `unreadOnly`, `page`, `size` | `PageResponse<NotificationResponse>` |
| `PATCH /v1/notifications/{notificationId}/read` | none | `NotificationResponse` |
| `GET /v1/notifications/settings` | none | `NotificationSettingsResponse` |
| `PATCH /v1/notifications/settings` | partial settings JSON | `NotificationSettingsResponse` |
| `GET /v1/analytics/courses/{courseId}/summary` | none | `CourseAnalyticsResponse` |
| `GET /v1/analytics/courses/{courseId}/students/{studentId}/gradebook` | none | `GradebookEntryResponse[]` |
| `GET /v1/explore/feed?subject=&page=0&size=20` | optional `subject`, pagination | `PageResponse<CourseDiscoveryResponse>` |
| `GET /v1/explore/courses/search?q=calculus&page=0&size=20` | required `q`, pagination | `PageResponse<CourseDiscoveryResponse>` |
| `GET /v1/explore/recommendations?page=0&size=20` | pagination | `PageResponse<CourseDiscoveryResponse>` |

```json
// NotificationResponse
{
  "id": "notification-id", "type": "SUBMISSION_GRADED", "title": "Submission graded",
  "message": "...", "resourceType": "SUBMISSION", "resourceId": "submission-id",
  "read": false, "readAt": null, "createdAt": "2026-09-09T12:00:00Z"
}
// NotificationSettingsRequest (send changed fields only)
{ "emailEnabled": true, "pushEnabled": false, "inAppEnabled": true }
// NotificationSettingsResponse
{ "emailEnabled": true, "pushEnabled": false, "inAppEnabled": true, "updatedAt": "2026-09-09T12:00:00Z" }

// CourseAnalyticsResponse
{
  "courseId": "course-id", "studentCount": 24, "courseworkCount": 5,
  "submissionCount": 23, "turnedInCount": 20, "missingCount": 4, "gradedCount": 16,
  "averageScore": 87.25, "onTimeSubmissionRate": 0.83,
  "missingSubmissionRate": 0.17, "generatedAt": "2026-09-09T12:00:00Z"
}
// GradebookEntryResponse
{
  "courseworkId": "coursework-id", "title": "Problem set 1", "maximumPoints": 100,
  "score": 95.5, "status": "GRADED", "feedback": "Well done",
  "dueAt": "2026-09-16T23:59:00Z", "gradedAt": "2026-09-11T12:00:00Z"
}
// CourseDiscoveryResponse
{
  "courseId": "course-id", "title": "Calculus I", "subject": "Mathematics",
  "tags": ["calculus", "math"], "enrollmentCount": 24, "popularityScore": 9.2,
  "lastActivityAt": "2026-09-09T12:00:00Z"
}
```

## Exact enum values

| Field | Allowed values |
| --- | --- |
| `accountType` | `TEACHER`, `STUDENT`, `ADMIN` (registration: only `TEACHER`, `STUDENT`) |
| `profileVisibility` | `PUBLIC`, `COURSE_MEMBERS`, `PRIVATE` (`COURSE_MEMBERS` currently rejected) |
| `visibility` | `PUBLIC`, `PRIVATE` |
| course `status` | `ACTIVE`, `ARCHIVED` |
| membership `role` | `OWNER`, `TEACHER`, `ASSISTANT`, `STUDENT` |
| coursework `type` | `ASSIGNMENT`, `ANNOUNCEMENT`, `MATERIAL` |
| coursework `status` | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| submission `status` | `DRAFT`, `TURNED_IN`, `RETURNED`, `GRADED`, `MISSING` |
| attachment `resourceType` | `COURSEWORK`, `SUBMISSION` |
| attachment `status` | `PENDING`, `UPLOADED`, `DELETED` |
| comment `visibility` | `PUBLIC`, `PRIVATE` |
| notification `type` | `COURSE_INVITATION`, `COURSEWORK_PUBLISHED`, `COURSEWORK_UPDATED`, `SUBMISSION_GRADED`, `COMMENT_ADDED` |
| notification `resourceType` | `COURSE`, `COURSEWORK`, `SUBMISSION`, `COMMENT` |

## Acceptance checklist

- [ ] No URL differs from the endpoint worklist, and the API base URL is
  configurable without code changes.
- [ ] A 401 response performs at most one refresh request, retries once, and
  otherwise ends the session cleanly.
- [ ] OAuth callback handles both token and `oauth_failed` query variants and
  removes sensitive tokens from the browser URL.
- [ ] Every paginated view reads `content`, `page`, `size`, `totalElements`,
  `totalPages`, `first`, and `last` from `PageResponse`.
- [ ] Delete, archive, leave, and logout actions correctly handle `204` without
  attempting JSON parsing.
- [ ] Forms send uppercase enum values, ISO timestamps, and only supported
  request properties.
- [ ] Attachment UI follows request-upload-complete and never sends Cloudinary
  secrets or claims an attachment is complete before backend confirmation.
- [ ] Error UI displays the backend’s `error` string and preserves form state
  for correctable `400` and `409` failures.
