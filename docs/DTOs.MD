 # DTO Design

> **Status:** Planned DTO inventory. No DTO classes currently exist in the
> source tree. These names describe the intended request and response boundary
> between controllers and services.

DTOs should validate input at the controller boundary and prevent persistence
documents from being returned directly to API clients.


### Authentication

* `RegisterRequest`
* `LoginRequest`
* `AuthResponse`
* `TokenRefreshRequest`

---

### Courses

* `CreateCourseRequest`
* `UpdateCourseRequest`
* `CourseResponse`
* `CourseSummaryResponse`
* `CourseEnrollmentRequest`
* `CourseRosterResponse`
* `RosterMemberResponse`

---

### Coursework

* `CreateCourseworkRequest`
* `UpdateCourseworkRequest`
* `CourseworkResponse`
* `CourseworkSummaryResponse`

---

### Submissions

* `CreateSubmissionRequest`
* `UpdateSubmissionRequest`
* `GradeSubmissionRequest`
* `SubmissionResponse`
* `SubmissionSummaryResponse`

---

### Attachments

* `GenerateUploadUrlRequest`
* `UploadUrlResponse`
* `DownloadUrlResponse`
* `AttachmentMetadataResponse`

---

### Comments

* `CreateCommentRequest`
* `CommentResponse`

---

### Analytics

* `CourseAnalyticsSummaryResponse`
* `StudentGradebookResponse`
* `GradebookEntryResponse`

---

### Notifications

* `NotificationResponse`
* `UpdateNotificationStatusRequest`
* `NotificationSettingsResponse`
* `UpdateNotificationSettingsRequest`

---

### Profiles

* `UserProfileResponse`
* `PublicUserProfileResponse`
* `UpdateUserProfileRequest`

---

### Explore

* `ExploreFeedResponse`
* `CourseSearchResultResponse`
* `CourseRecommendationResponse`

---

### Common / Shared

* `PagedResponse`
* `ApiErrorResponse`