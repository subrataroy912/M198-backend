# Majorproject

Majorproject is a Spring Boot backend for a classroom and course-management
platform. It provides user accounts, teacher-led courses, enrollment,
coursework, submissions, attachments, comments, notifications, profiles,
course discovery, and academic analytics.

> **Current implementation status:** Authentication (including JWT refresh-token
> rotation and OAuth provisioning) and the course, coursework, submission,
> attachment, comment, notification, analytics, profile, and explore endpoint
> slices are implemented. Their controllers delegate to domain services backed
> by MongoDB repositories. The remaining work is operational infrastructure,
> such as supplying production MongoDB, OAuth, JWT, and Cloudinary settings.

## Project Mission

The mission of Majorproject is to provide one focused workspace where teachers
can organize learning and assessment, while students can discover courses,
join them, complete coursework, submit work, receive feedback, and monitor
their progress.

The project is designed around these goals:

- Make course creation and student admission simple and controlled.
- Keep coursework, submissions, feedback, and files connected to the correct
  course context.
- Protect private student work, teacher feedback, account data, and course
  membership information.
- Give teachers useful performance and submission information.
- Keep the backend modular so each business domain can evolve independently.

## User Workflows

### Account and access

1. A user registers with a `TEACHER` or `STUDENT` role, or signs in through
   Google or GitHub OAuth.
2. The user receives JWT access and refresh tokens; refresh tokens are stored
   server-side as hashes and rotated when used.
3. Protected operations use the authenticated identity and role.
4. The user can log out and revoke the supplied refresh token.
5. The user can read and update their profile, including an optional multipart
   update with avatar and banner files.

### Course admission and enrollment

1. An authenticated user creates a course and receives an enrollment code.
2. The course owner can update or archive the course, request a cover upload,
   and view its roster.
3. A student submits the enrollment code to join the course.
4. Course members can list and view their courses; a member can leave a course.

### Learning, feedback, and discovery

1. Course staff create, update, list, retrieve, and archive coursework.
2. Students start drafts, save or turn in submissions, and view their own work;
   staff list submissions and grade them with feedback.
3. Course members add and read coursework comments and private submission
   feedback comments.
4. Authorized users create, complete, list, download, and delete attachments.
5. Users read notifications, mark them as read, and update alert settings.
6. Teachers access course summaries and student gradebooks; users browse,
   search, and receive course recommendations through explore endpoints.

## Technology Stack

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC and Spring Security
- Spring Data MongoDB
- JWT and OAuth 2.0 client support
- Cloudinary for attachment upload and download URLs
- Lombok, configured as an optional dependency
- Maven Wrapper for reproducible Maven commands
- JUnit and Spring Boot test support

## Project Structure

```text
src/main/java/com/M198/Majorproject/
|-- config/                         # Application, security, and integration configuration
|-- controller/                     # HTTP controllers by domain
|   |-- analytics/ attachment/ auth/ comment/ course/ coursework/
|   |-- explore/ notification/ profile/ submission/
|-- dto/                            # Validated request and response contracts
|-- entity/                         # MongoDB documents by domain
|-- repository/                     # MongoDB repositories by domain
|-- security/                       # JWT authentication and authorization support
`-- service/                        # Implemented domain business services
    |-- analytics/ attachment/ auth/ comment/ course/ coursework/
    `-- explore/ notification/ profile/ submission/

docs/
|-- APIs.MD                         # API catalog
`-- IDENTITY_SCHEMA.MD              # Identity schema
```

## Configuration

`src/main/resources/application.yml` imports an optional root-level `local.env`
file. The same values can be provided as normal process environment variables
in deployment. The local file is ignored by Git.

```properties
MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>
MONGODB_DATABASE=majorproject
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GITHUB_CLIENT_ID=<github-client-id>
GITHUB_CLIENT_SECRET=<github-client-secret>
JWT_SECRET=<at-least-32-character-secret>
JWT_ACCESS_TOKEN_LIFETIME=15m
JWT_REFRESH_TOKEN_LIFETIME=7d
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
APP_OAUTH2_FRONTEND_CALLBACK_URL=http://localhost:5173/auth/callback
APP_GITHUB_API_BASE_URL=https://api.github.com
CLOUDINARY_CLOUD_NAME=<cloudinary-cloud-name>
CLOUDINARY_API_KEY=<cloudinary-api-key>
CLOUDINARY_API_SECRET=<cloudinary-api-secret>
```

Replace the placeholder values in `local.env` before running locally. Do not
commit credentials or a populated connection string to source control.

### OAuth deployment configuration

Start authentication at `GET /v1/auth/oauth/google` (or `/github`), not at
`/oauth2/authorization/...`. The latter is Spring Security's default endpoint,
but this application deliberately uses the versioned `/v1/auth/oauth` endpoint.

`APP_OAUTH2_FRONTEND_CALLBACK_URL` is the **final browser destination** after a
successful or failed login. It is not the callback that must be registered with
Google. Register the backend's exact redirect URI instead:

```text
https://<backend-render-service>.onrender.com/login/oauth2/code/google
```

The backend route must remain publicly reachable and must not be rewritten to
the frontend. Render's forwarded HTTPS headers are honored so Spring generates
the same HTTPS URI during the authorization request.

## Implemented API Catalog

All routes use the `/v1` version prefix. See [docs/APIs.MD](docs/APIs.MD) for
request/response intent and access rules.

| Area | Implemented routes |
| --- | --- |
| Authentication | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`; `GET /auth/oauth/google`, `/auth/oauth/github` |
| Courses | `POST /courses`, `/courses/cover-upload`, `/{courseId}/enrollment`; `GET /courses`, `/{courseId}`, `/{courseId}/roster`; `PATCH /courses/{courseId}`; `DELETE /courses/{courseId}`, `/{courseId}/enrollment` |
| Coursework | `POST`, `GET /courses/{courseId}/coursework`; `GET`, `PATCH`, `DELETE /courses/{courseId}/coursework/{courseworkId}` |
| Submissions | `POST`, `GET /coursework/{courseworkId}/submissions`; `GET`, `PATCH /coursework/{courseworkId}/submissions/me`; `PATCH /coursework/{courseworkId}/submissions/{submissionId}/grade` |
| Attachments | `POST /attachments/upload-url`, `/{attachmentId}/complete`; `GET /attachments`, `/{attachmentId}/download-url`; `DELETE /attachments/{attachmentId}` |
| Comments | `POST`, `GET /coursework/{courseworkId}/comments`; `POST`, `GET /submissions/{submissionId}/comments` |
| Analytics | `GET /analytics/courses/{courseId}/summary`; `GET /analytics/courses/{courseId}/students/{studentId}/gradebook` |
| Notifications | `GET /notifications`, `/notifications/settings`; `PATCH /notifications/{notificationId}/read`, `/notifications/settings` |
| Profiles | `GET /users/me`, `/users/{userId}`; `PATCH /users/me` (JSON or multipart form data) |
| Explore | `GET /explore/feed`, `/explore/courses/search`, `/explore/recommendations` |

## Build, Test, and Run

Use the Maven Wrapper on Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The correct Spring Boot Maven goal is `spring-boot:run`; `mvn run:spring-boot`
is not a valid goal for this project.

The test suite includes unit and controller tests across authentication,
security, courses, coursework, submissions, attachments, comments,
notifications, analytics, and profiles.

## Infrastructure Follow-up

1. Provision and monitor production MongoDB, OAuth-provider, JWT, CORS, and
   Cloudinary credentials through the deployment environment.
2. Add CI checks for the Maven test suite and deployment configuration.
3. Add operational observability, backups, and retention procedures for the
   production database and stored assets.

## Documentation Guide

- [API catalog](docs/APIs.MD)
- [Frontend wiring task](docs/FRONTEND_WIRING_TASK.MD)
- [Implemented identity schema](docs/IDENTITY_SCHEMA.MD)
- [Spring reference material](HELP.md)
