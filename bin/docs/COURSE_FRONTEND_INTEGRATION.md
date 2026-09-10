# Course and Explore API Frontend Integration

All course-management requests use the `/v1` prefix and require an access token:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

The discovery feed and search endpoints are deliberately public. The
recommendations endpoint remains authenticated.

## Course dashboard and detail pages

1. Call `GET /v1/courses` after sign-in to populate the user's course dashboard.
   It returns active courses for which the current user has an active membership.
2. When a card is opened, call `GET /v1/courses/{courseId}`. A `404` can mean the
   course is archived, does not exist, or the current user is not an active member;
   do not expose which case applies.
3. Use `GET /v1/courses/{courseId}/roster` for the People tab. Members can read
   it, as can administrators.

`CourseResponse` contains `id`, `ownerId`, `title`, `section`, `subject`,
`description`, `coverUrl`, `visibility` (`PUBLIC` or `PRIVATE`), `status`
(`ACTIVE` or `ARCHIVED`), `enrollmentEnabled`, `createdAt`, and `updatedAt`.
The enrollment code is returned only when a course is created, so store or show
it to the owner immediately; it is not included in ordinary course reads.

## Create, edit, archive, and cover images

Create a course with `POST /v1/courses`:

```json
{
  "title": "Calculus I",
  "section": "A",
  "subject": "Mathematics",
  "description": "Limits and derivatives",
  "visibility": "PRIVATE"
}
```

The creator becomes the `OWNER`; course creation is available to any
authenticated account. The `200` response includes the newly generated
`enrollmentCode` once. Only an `OWNER` or `TEACHER` membership can edit or
archive a course.

To upload a cover, first request `POST /v1/courses/cover-upload`. Submit the
returned `publicId`, `uploadTimestamp`, `uploadSignature`, and `uploadApiKey`
to the returned Cloudinary `uploadUrl` with the image file. After Cloudinary
returns its secure URL, save it with:

```http
PATCH /v1/courses/{courseId}
```

```json
{ "coverUrl": "https://res.cloudinary.com/.../image/upload/..." }
```

`PATCH` accepts any subset of `title`, `section`, `subject`, `description`,
`coverUrl`, `visibility`, and `enrollmentEnabled`. Send `DELETE
/v1/courses/{courseId}` to archive a course; success is `204 No Content`.

## Joining and leaving

The join flow is for accounts with the `STUDENT` role:

```http
POST /v1/courses/{courseId}/enrollment
```

```json
{ "code": "ABCD1234" }
```

Show the returned course on success. Treat `403` as an invalid/expired code,
disabled enrollment, or an account that is not a student; treat `409` as
already joined. `DELETE /v1/courses/{courseId}/enrollment` leaves the course
and returns `204`. Owners cannot leave their own course.

## Explore screens

These endpoints return `PageResponse<CourseDiscoveryResponse>` with `content`,
`page`, `size`, `totalElements`, `totalPages`, `first`, and `last`.

| Screen | Request |
| --- | --- |
| Public discovery feed | `GET /v1/explore/feed?subject=Mathematics&page=0&size=20` |
| Public search | `GET /v1/explore/courses/search?q=calculus&page=0&size=20` |
| Signed-in recommendations | `GET /v1/explore/recommendations?page=0&size=20` |

Use page numbers starting at `0`; `size` must be from `1` through `100`.
Search requires a nonblank `q`; do not substitute an empty query with the feed.
Discovery results contain only public, active courses and expose summary fields:
`courseId`, `title`, `subject`, `tags`, `enrollmentCount`, `popularityScore`,
and `lastActivityAt`. Recommendations currently use the same public ranking as
the feed, so label them as “Recommended” rather than claiming individualized
reasoning.

## Error handling

Render validation errors (`400`) inline. For protected course calls, redirect
or refresh credentials on `401`, and hide the action/show a permission message
on `403`. A `409` should preserve the user's form and show the returned `error`
message. Successful archive and leave calls have no response body because they
return `204`.
