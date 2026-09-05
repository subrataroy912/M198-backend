 # Repository Design

> **Status:** Planned persistence layer. The `repository` package is currently
> empty. These interfaces should be added after the MongoDB document model and
> query requirements are finalized.

Repositories should focus on persistence queries. Authorization, enrollment
rules, grading rules, and analytics calculations belong in services.

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