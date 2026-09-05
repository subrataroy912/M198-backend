 # Service Design

> **Status:** Planned business layer. The `service` package is currently empty.
> The services below represent the application operations that should sit
> between controllers and repositories.

Services should own validation that depends on business state, transaction or
workflow rules, membership checks, notification triggers, and safe response
construction. Controllers should remain thin request adapters.

### User & Identity

* `UserRepository`
* `RefreshTokenRepository`

---

### Course & Membership

* `CourseRepository`
* `CourseMemberRepository`
* `CourseEnrollmentCodeRepository`

---

### Coursework & Assessments

* `CourseworkRepository`
* `SubmissionRepository`
* `SubmissionGradeRepository`

---

### Media & Attachments

* `AttachmentRepository`
* `CourseworkAttachmentRepository`
* `SubmissionAttachmentRepository`

---

### Collaboration & Feedback

* `CommentRepository`
* `CourseworkCommentRepository`
* `SubmissionCommentRepository`

---

### Communication & Preferences

* `NotificationRepository`
* `NotificationSettingRepository`

---

### Analytics & Activity Tracking

* `ActivityLogRepository`
* `CourseAnalyticsSnapshotRepository`
* `StudentProgressRepository`