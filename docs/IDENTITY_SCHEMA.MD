# Identity Schema

## Status

The identity portion of the supplied Google Classroom-style schema is
implemented as four MongoDB collections:

- `users`
- `user_profiles`
- `user_oauth`
- `auth_refresh_tokens`

The implementation uses application-managed ID references instead of MongoDB
`DBRef`. This keeps documents independent and makes a future service split
easier.

## Relationship Model

```text
User (1) -------- (1) UserProfile
  |
  +------------- (N) UserOAuth
```

- `UserProfile.userId` is unique, enforcing one profile per user.
- `UserOAuth.userId` allows multiple external providers per user.
- `(userId, provider)` is unique, preventing the same provider from being linked
  twice to one user.
- `(provider, providerUserId)` is unique, preventing one external account from
  being linked to multiple platform users.

## User Collection

Java model: `entity.identity.User`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Platform user identifier |
| `email` | `email` | String | Login identity; unique index |
| `passwordHash` | `password_hash` | String | Adaptive password hash; never return through an API |
| `accountType` | `account_type` | AccountType | `TEACHER`, `STUDENT`, or `ADMIN` |
| `status` | `status` | AccountStatus | `PENDING`, `ACTIVE`, `SUSPENDED`, or `DELETED` |
| `active` | `is_active` | Boolean | Fast active-account check |
| `verified` | `is_verified` | Boolean | Whether account verification is complete |
| `lastLoginAt` | `last_login_at` | Instant | Most recent successful login |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `deletedAt` | `deleted_at` | Instant | Soft-deletion timestamp |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |

## UserProfile Collection

Java model: `entity.identity.UserProfile`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Profile document identifier |
| `userId` | `user_id` | String | Reference to `User`; unique index |
| `firstName` | `first_name` | String | User's first name |
| `lastName` | `last_name` | String | User's last name |
| `displayName` | `display_name` | String | Name shown in the application |
| `headline` | `headline` | String | Short profile headline |
| `about` | `about` | String | Profile description |
| `avatarUrl` | `avatar_url` | String | Private or public avatar location |
| `bannerUrl` | `banner_url` | String | Profile banner location |
| `city` | `city` | String | Profile location |
| `country` | `country` | String | Profile location |
| `profileVisibility` | `profile_visibility` | ProfileVisibility | `PUBLIC`, `COURSE_MEMBERS`, or `PRIVATE` |
| `gradeLevel` | `grade_level` | String | Student grade or academic level |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |
| `deletedAt` | `deleted_at` | Instant | Soft-deletion timestamp |

## UserOAuth Collection

Java model: `entity.identity.UserOAuth`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | OAuth link identifier |
| `userId` | `user_id` | String | Reference to `User` |
| `provider` | `provider` | OAuthProvider | `GOOGLE`, `MICROSOFT`, or `GITHUB` |
| `providerUserId` | `provider_user_id` | String | External provider account identifier |
| `accessToken` | `access_token` | String | Provider token; must be encrypted or minimized before production |
| `refreshToken` | `refresh_token` | String | Provider refresh token; must be encrypted and rotated |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |

## RefreshToken Collection

Java model: `entity.identity.RefreshToken`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Refresh-token record identifier |
| `tokenHash` | `token_hash` | String | SHA-256 hash of the client refresh token; unique |
| `userId` | `user_id` | String | Reference to `User` |
| `expiresAt` | `expires_at` | Instant | TTL cleanup and validity boundary |
| `revokedAt` | `revoked_at` | Instant | Rotation or logout revocation timestamp |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |

## Implemented Java Files

- `entity.identity.User`
- `entity.identity.UserProfile`
- `entity.identity.UserOAuth`
- `entity.identity.RefreshToken`
- `entity.identity.AccountType`
- `entity.identity.AccountStatus`
- `entity.identity.ProfileVisibility`
- `entity.identity.OAuthProvider`
- `repository.identity.UserRepository`
- `repository.identity.UserProfileRepository`
- `repository.identity.UserOAuthRepository`
- `repository.identity.RefreshTokenRepository`
- `config.MongoAuditingConfig`

## Repository Operations

The identity repositories currently support:

- Find a user by email.
- Check whether an email already exists.
- Find a profile by platform user ID.
- Find all OAuth links for a user.
- Find a user's OAuth link by provider.
- Find a platform user by external provider identity.
- Rotate and revoke persisted refresh tokens by hash.

## Course Access Collections

The course-access domain uses three additional collections:

- `courses`
- `course_memberships`
- `enrollment_codes`

The implementation continues to use application-managed String ID references
instead of MongoDB `DBRef`.

```text
User (1) -------- (N) CourseMembership (N) -------- (1) Course
Course (1) ------ (N) EnrollmentCode
```

### Course Collection

Java model: `entity.course.Course`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Platform course identifier |
| `ownerId` | `owner_id` | String | User who owns the course |
| `title` | `title` | String | Course name |
| `section` | `section` | String | Class or section label |
| `subject` | `subject` | String | Subject/category used by discovery |
| `description` | `description` | String | Course description |
| `visibility` | `visibility` | CourseVisibility | `PUBLIC` or `PRIVATE` |
| `status` | `status` | CourseStatus | `ACTIVE` or `ARCHIVED` |
| `enrollmentEnabled` | `enrollment_enabled` | Boolean | Whether code enrollment is allowed |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |
| `archivedAt` | `archived_at` | Instant | Course archival timestamp |

### Course Membership Collection

Java model: `entity.course.CourseMembership`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Membership identifier |
| `courseId` | `course_id` | String | Reference to `Course` |
| `userId` | `user_id` | String | Reference to `User` |
| `role` | `role` | MembershipRole | `OWNER`, `TEACHER`, `ASSISTANT`, or `STUDENT` |
| `status` | `status` | MembershipStatus | `ACTIVE`, `LEFT`, or `REMOVED` |
| `joinedAt` | `joined_at` | Instant | Membership start timestamp |
| `removedAt` | `removed_at` | Instant | Leave/removal timestamp |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |

The compound unique index `(course_id, user_id)` prevents duplicate membership
documents. Membership roles and status are authorization inputs and must be
checked in the service layer.

### Enrollment Code Collection

Java model: `entity.course.EnrollmentCode`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Enrollment-code identifier |
| `courseId` | `course_id` | String | Reference to `Course` |
| `code` | `code` | String | Unique enrollment code |
| `active` | `active` | Boolean | Whether the code accepts new members |
| `createdBy` | `created_by` | String | User who generated the code |
| `expiresAt` | `expires_at` | Instant | Optional expiration boundary |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `deactivatedAt` | `deactivated_at` | Instant | Code rotation/deactivation timestamp |

Codes are uniquely indexed and queried by active status. The service layer
should enforce at most one active code per course when codes are rotated.

## Coursework Collection

The coursework domain uses the `coursework` collection for assignments,
announcements, and materials. It references courses and creators by String ID.

Java model: `entity.coursework.Coursework`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Coursework identifier |
| `courseId` | `course_id` | String | Reference to `Course` |
| `creatorId` | `creator_id` | String | User who created the item |
| `type` | `type` | CourseworkType | `ASSIGNMENT`, `ANNOUNCEMENT`, or `MATERIAL` |
| `title` | `title` | String | Coursework title |
| `description` | `description` | String | Coursework content |
| `status` | `status` | CourseworkStatus | `DRAFT`, `PUBLISHED`, or `ARCHIVED` |
| `publishedAt` | `published_at` | Instant | Publication timestamp |
| `dueAt` | `due_at` | Instant | Optional assignment deadline |
| `maximumPoints` | `maximum_points` | Integer | Optional assignment score limit |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |
| `archivedAt` | `archived_at` | Instant | Archival timestamp |

The `(course_id, status, published_at)` and `(course_id, type, status)` indexes
support the course stream and filtered coursework lists. Publication and
archival are represented separately from the item type so students can see
only published items while teachers retain access to drafts and archived data.

## Security Decisions

- `passwordHash` must contain only a strong adaptive hash, never a plain password.
- OAuth access and refresh tokens must not be returned in profile responses.
- Application refresh tokens are stored as SHA-256 hashes and revoked on rotation or logout.
- OAuth tokens should be encrypted at rest or replaced with a provider-token vault reference before production use.
- Email normalization should happen in the registration service before the unique index is used.
- Soft deletion should set both `status = DELETED` and `deletedAt` rather than immediately destroying audit-relevant identity data.
- Resource authorization is still required for profiles, courses, grades, and submissions; the entity model alone does not provide authorization.

## Submission Collection

The `submissions` collection stores one current submission per student and
coursework item. Current grade data is embedded until grade history or rubric
events require a separate collection.

Java model: `entity.submission.Submission`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Submission identifier |
| `courseworkId` | `coursework_id` | String | Reference to `Coursework` |
| `courseId` | `course_id` | String | Reference to `Course` for efficient authorization/querying |
| `studentId` | `student_id` | String | Submitting student |
| `status` | `status` | SubmissionStatus | `DRAFT`, `TURNED_IN`, `RETURNED`, `GRADED`, or `MISSING` |
| `answerText` | `answer_text` | String | Text answer or response |
| `submittedAt` | `submitted_at` | Instant | Turn-in timestamp |
| `returnedAt` | `returned_at` | Instant | Feedback return timestamp |
| `late` | `late` | Boolean | Whether the submission was late |
| `score` | `score` | Decimal | Current score |
| `graderId` | `grader_id` | String | Teacher who graded the submission |
| `feedback` | `feedback` | String | Current teacher feedback |
| `gradedAt` | `graded_at` | Instant | Grading timestamp |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |

The compound unique index `(coursework_id, student_id)` enforces one submission
per student and coursework item. Authorization must still verify that the
coursework belongs to the requested course and that the caller is the student,
teacher, or authorized course staff.

## Attachment Collection

The `attachments` collection stores file metadata and references external object
storage. File bytes and temporary upload/download URLs are not stored in MongoDB.

Java model: `entity.attachment.Attachment`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Attachment identifier |
| `courseId` | `course_id` | String | Course scope for authorization |
| `resourceType` | `resource_type` | AttachmentResourceType | `COURSEWORK` or `SUBMISSION` |
| `resourceId` | `resource_id` | String | Referenced coursework or submission |
| `ownerId` | `owner_id` | String | User who owns or uploaded the file |
| `storageKey` | `storage_key` | String | Private object-storage key |
| `originalFilename` | `original_filename` | String | Client-visible filename |
| `contentType` | `content_type` | String | MIME type |
| `sizeBytes` | `size_bytes` | Long | File size |
| `checksum` | `checksum` | String | Optional integrity checksum |
| `status` | `status` | AttachmentStatus | `PENDING`, `UPLOADED`, or `DELETED` |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |
| `deletedAt` | `deleted_at` | Instant | Metadata deletion timestamp |

The upload-url flow should create a `PENDING` record, transition it to
`UPLOADED` after the storage callback or verification step, and authorize
download/delete operations through the referenced course resource.

## Comment Collection

The `comments` collection supports public coursework discussions and private
submission feedback. The target type and visibility are stored explicitly so
authorization can distinguish the two conversations.

Java model: `entity.comment.Comment`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Comment identifier |
| `courseId` | `course_id` | String | Course scope for authorization |
| `authorId` | `author_id` | String | User who wrote the comment |
| `targetType` | `target_type` | CommentTargetType | `COURSEWORK` or `SUBMISSION` |
| `targetId` | `target_id` | String | Referenced coursework or submission |
| `visibility` | `visibility` | CommentVisibility | `PUBLIC` or `PRIVATE` |
| `body` | `body` | String | Comment content |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |
| `editedAt` | `edited_at` | Instant | Last content edit timestamp |
| `deletedAt` | `deleted_at` | Instant | Soft-deletion timestamp |

Coursework comments should use `PUBLIC` visibility. Submission comments should
use `PRIVATE` visibility and be restricted to the submitting student and the
authorized teacher or course staff.

## Notification Collection

The `notifications` collection stores user-facing alerts and references the
related resource by type and String ID.

Java model: `entity.notification.Notification`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Notification identifier |
| `recipientId` | `recipient_id` | String | User who receives the notification |
| `type` | `type` | NotificationType | Event category |
| `title` | `title` | String | Short notification title |
| `message` | `message` | String | Display message |
| `resourceType` | `resource_type` | NotificationResourceType | Related resource category |
| `resourceId` | `resource_id` | String | Related resource identifier |
| `read` | `read` | Boolean | Whether the recipient has read it |
| `readAt` | `read_at` | Instant | Read timestamp |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |

The `(recipient_id, read, created_at)` and `(recipient_id, created_at)` indexes
support unread counts and recent notification feeds.

## Notification Settings Collection

The `notification_settings` collection stores delivery preferences separately
from public profile data.

Java model: `entity.notification.NotificationSettings`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Settings identifier |
| `userId` | `user_id` | String | Reference to `User`; unique |
| `emailEnabled` | `email_enabled` | Boolean | Email delivery preference |
| `pushEnabled` | `push_enabled` | Boolean | Push delivery preference |
| `inAppEnabled` | `in_app_enabled` | Boolean | In-app delivery preference |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |

New users should receive default-enabled settings. Notification services must
still apply the recipient's settings before sending external email or push
messages.

## Explore Read Model

The `course_discovery` collection is a read-oriented projection for public
course feeds, search, and recommendations. `Course` remains the authoritative
source for course ownership, visibility, and lifecycle state.

Java model: `entity.explore.CourseDiscovery`

| Field | MongoDB field | Type | Purpose |
| --- | --- | --- | --- |
| `id` | `_id` | String | Projection identifier |
| `courseId` | `course_id` | String | Unique reference to `Course` |
| `title` | `title` | String | Search/display title |
| `subject` | `subject` | String | Search/filter subject |
| `tags` | `tags` | List<String> | Search and recommendation tags |
| `visibility` | `visibility` | CourseVisibility | Public/private projection state |
| `status` | `status` | CourseStatus | Active/archived projection state |
| `enrollmentCount` | `enrollment_count` | Long | Discovery popularity signal |
| `popularityScore` | `popularity_score` | Double | Ranking score |
| `lastActivityAt` | `last_activity_at` | Instant | Recency ranking signal |
| `createdAt` | `created_at` | Instant | Set by Mongo auditing |
| `updatedAt` | `updated_at` | Instant | Set by Mongo auditing |

Only public, active projections should be returned by public explore endpoints.
Projection updates should be idempotent and rebuildable from course and
membership data.

## Analytics Read Models

Analytics are represented as rebuildable read models rather than a second
authoritative copy of grades or memberships.

### Course Analytics Summary

Java model: `entity.analytics.CourseAnalyticsSummary`

The `course_analytics_summaries` collection stores course-level counts and
calculated rates, including student, coursework, submission, missing, graded,
average-score, and on-time-submission metrics. It has one summary per course
and a `generatedAt` timestamp to identify projection freshness.

### Student Gradebook Entry

Java model: `entity.analytics.StudentGradebookEntry`

The `student_gradebook_entries` collection stores one projection per
`courseId`, `studentId`, and `courseworkId`. It contains coursework display data,
maximum points, current score, submission status, feedback, due date, and
grading timestamps. The compound unique index prevents duplicate gradebook
entries while allowing the projection to be rebuilt from coursework and
submission documents.

Analytics authorization must be applied before reading these projections:
teachers or administrators may read course summaries, while students may read
only their own gradebook and teachers may read gradebooks for students in their
course.

## Validation

The existing Maven test suite passes with the identity, course, coursework,
submission, attachment, comment, notification, explore, and analytics
documents, repositories, Mongo auditing configuration, and compound indexes
loaded by Spring Boot.