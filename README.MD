 # Majorproject

Majorproject is a Spring Boot backend for a classroom and course-management
platform. The planned system supports user accounts, teacher-led courses,
student admission through enrollment codes, coursework, submissions,
attachments, comments, notifications, profiles, exploration, and academic
analytics.

> **Current implementation status:** Authentication, JWT refresh-token
> rotation, OAuth provisioning, and Profiles are implemented. The application
> starts when MongoDB, JWT, and OAuth configuration are supplied. Course,
> coursework, submission, notification, exploration, and analytics endpoints
> remain planned.

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

## Planned User Workflows

### Account and access

1. A user registers with a `TEACHER` or `STUDENT` role.
2. The user signs in and receives an access token.
3. Protected operations use the authenticated identity and role.
4. The user can end the session through logout or token invalidation.

Authentication is implemented with registration, login, refresh, logout, JWT
validation, and OAuth provisioning. Refresh tokens are stored server-side and
rotated on use. Profile endpoints use the authenticated JWT subject for owner
operations.

### Course admission and enrollment

1. A teacher creates a course with its name, section, subject, and description.
2. The platform creates an enrollment code for the course.
3. A student submits the enrollment code to request admission.
4. The platform validates the code, prevents duplicate membership, and creates a
	student-course membership.
5. Enrolled students can access course content and their own submissions.
6. A student can leave a course, while an authorized teacher can manage the
	course roster.

The enrollment flow is a planned design. Its database model, service, and
endpoint implementation are still to be built.

### Learning and assessment

Teachers are planned to publish assignments, announcements, and materials.
Students are planned to start drafts, upload attachments, turn in work, and
receive grades and private feedback. Teachers are planned to review submissions,
grade them, and view course-level analytics.

## Technology Stack

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data MongoDB
- Lombok, configured as an optional dependency
- Maven Wrapper for reproducible Maven commands
- JUnit and Spring Boot test support

## Project Structure

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
INNGEST_EVENT_KEY=<inngest-event-key>
# Optional; defaults to Inngest Cloud's event endpoint.
INNGEST_EVENT_URL=https://inn.gs/e/
```

Replace the placeholder values in `local.env` before running locally. Do not
commit credentials or a populated connection string to source control.

### OAuth deployment configuration

Start authentication at `GET /v1/auth/oauth/google` (or `/github`), not at
`/oauth2/authorization/...`. The latter is Spring Security's default endpoint,
but this application deliberately uses the versioned `/v1/auth/oauth` endpoint.

`APP_OAUTH2_FRONTEND_CALLBACK_URL` is the **final browser destination** after a
successful or failed login. For example, it may correctly be
`https://campus-mind-ev1s.onrender.com/auth/callback` when that is the frontend.
It is not the callback that must be registered with Google.

In Google Cloud Console, register the backend's exact redirect URI instead:

```text
https://<backend-render-service>.onrender.com/login/oauth2/code/google
```

The backend route must remain publicly reachable and must not be rewritten to
the frontend. Render's forwarded HTTPS headers are honored so Spring generates
the same HTTPS URI during the authorization request. If the browser reaches the
frontend with `error=oauth_failed`, inspect the backend logs: the failure handler
logs the provider error while intentionally keeping it out of the redirect URL.

#### Inngest configuration

`INNGEST_EVENT_KEY` is an event key, not an HTTP URL. The application appends it
to `INNGEST_EVENT_URL` (which defaults to `https://inn.gs/e/`) before sending
events. An invalid event configuration no longer prevents users from completing
OAuth, password login, or registration; it is logged as an event-delivery error.
|   |-- notification/                  # Notifications and alert settings
|   |-- profile/                       # Current and public user profiles
|   `-- submission/                    # Student submissions and grading
|-- entity/identity/                   # User, profile, OAuth documents and enums
|-- repository/identity/               # Identity MongoDB repositories
`-- service/                            # Planned business services; currently empty

docs/
|-- APIs.MD                            # Planned API catalog
`-- IDENTITY_SCHEMA.MD                 # Implemented identity schema
```

The controller folders are organized by business domain so future services can
mirror the same boundaries. Spring component scanning still discovers these
controllers because they remain below the application package.

## Current Controller Surface

The following class-level prefixes are currently registered:

| Controller area | Base path |
| --- | --- |
| Authentication | `/v1/auth` |
| Courses | `/v1/courses` |
| Coursework | `/v1/courses/{courseId}/coursework` |
| Submissions | `/v1/coursework/{courseworkId}/submissions` |
| Attachments | `/v1/attachments` |
| Comments | `/v1` |
| Analytics | `/v1/analytics` |
| Notifications | `/v1/notifications` |
| Profiles | `/v1/users` |
| Explore | `/v1/explore` |

Most business areas still have planned method-level mappings. The Profiles
area is implemented with `GET /v1/users/me`, `GET /v1/users/{userId}`, and
`PATCH /v1/users/me`; the detailed endpoint catalog in
[docs/APIs.MD](docs/APIs.MD) describes the remaining planned areas.

## Configuration

`src/main/resources/application.yml` imports an optional root-level
`local.env` file. The same values can be provided as normal process environment
variables in deployment. The local file is ignored by Git.

```yaml
spring:
  mongodb:
	 uri: ${MONGODB_URI}/majorproject
```

PowerShell example:

```powershell
$env:MONGODB_URI = "mongodb+srv://<username>:<password>@<cluster>"
```

Do not commit credentials or a populated connection string to source control.

## Build, Test, and Run

Use the Maven Wrapper on Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The correct Spring Boot Maven goal is `spring-boot:run`; `mvn run:spring-boot`
is not a valid goal for this project.

The current test suite contains one Spring context test. It verifies that the
application context can start with the configured MongoDB connection; it does
not yet test HTTP endpoints, repository queries, or business workflows.

## Development Roadmap

1. Add MongoDB entities and enums for courses, memberships, coursework,
	submissions, attachments, comments, notifications, and grades.
2. Add repositories and indexes for enrollment codes, memberships, and
	submission ownership.
3. Implement DTOs with validation and safe response projections.
4. Implement services for authentication, admission, coursework, submissions,
	files, comments, notifications, profiles, and analytics.
5. Add authentication and role-based authorization.
6. Implement controller methods from the planned API catalog.
7. Add controller and service tests for permissions, admission, submission
	privacy, grading, and error responses.
8. Add API documentation generation and deployment configuration.

## Documentation Guide

- [Planned APIs](docs/APIs.MD)
- [Implemented identity schema](docs/IDENTITY_SCHEMA.MD)
- [Spring reference material](HELP.md)
