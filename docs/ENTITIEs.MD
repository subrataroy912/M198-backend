 # Domain Entities

> **Status:** Planned MongoDB document model. The `entity` package is currently
> empty, so the following classes and enums are design requirements rather than
> implemented types.

### User & Identity

* `User`
* `RefreshToken`

---

### Course & Membership (Contextual Roles)

* `Course`
* `CourseMember` *(links `User` + `Course` with role: `TEACHER`/`ADMIN` or `STUDENT`/`MEMBER`)*
* `CourseEnrollmentCode`

---

### Coursework & Assessments

* `Coursework`
* `Submission`
* `SubmissionGrade`

---

### Media & Attachments

* `Attachment`
* `CourseworkAttachment`
* `SubmissionAttachment`

---

### Collaboration & Feedback

* `Comment`
* `CourseworkComment`
* `SubmissionComment`

---

### Communication & Preferences

* `Notification`
* `NotificationSetting`

---

### Analytics & Activity Tracking

* `ActivityLog`
* `CourseAnalyticsSnapshot`
* `StudentProgress`

---

### Relevant Enums

* `CourseRole` (`TEACHER`, `STUDENT`)
* `SubmissionStatus` (`DRAFT`, `SUBMITTED`, `GRADED`, `RETURNED`)
* `CourseworkType` (`ASSIGNMENT`, `ANNOUNCEMENT`, `QUIZ`)