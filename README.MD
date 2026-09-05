 # Majorproject

Majorproject is a Spring Boot backend for a classroom and course-management
platform. The planned system supports user accounts, teacher-led courses,
student admission through enrollment codes, coursework, submissions,
attachments, comments, notifications, profiles, exploration, and academic
analytics.

> **Current implementation status:** The project is at the foundation and API
> design stage. The application starts successfully when MongoDB is configured,
> but the controllers currently contain class-level route prefixes and API
> documentation only. No endpoint methods, services, repositories, entities,
> DTOs, authentication, or authorization have been implemented yet.

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

Authentication is currently documented but not implemented. Spring Security and
JWT dependencies are not currently present in the build.

### Course admission and enrollment

1. A teacher creates a course with its name, section, subject, and description.
2. The platform creates an enrollment code for the course.
3. A student submits the enrollment code to request admission.
4. The platform validates the code, prevents duplicate membership, and creates a
	student-course membership.
5. Enrolled students can access course content and their own submissions.
6. A student can leave a course, while an authorized teacher can manage the
	course roster.

The enrollment flow is a planned design. There is currently no database model,
service, or endpoint implementation behind it.

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

```text
src/main/java/com/M198/Majorproject/
|-- MajorprojectApplication.java       # Spring Boot entry point
|-- controller/
|   |-- analytics/                     # Course and student analytics
|   |-- attachment/                    # Upload, download, and deletion URLs
|   |-- auth/                          # Registration, login, and logout
|   |-- comment/                       # Public and private comments
|   |-- course/                        # Course and membership operations
|   |-- coursework/                    # Assignments, announcements, materials
|   |-- explore/                       # Public discovery and recommendations
|   |-- notification/                  # Notifications and alert settings
|   |-- profile/                       # Current and public user profiles
|   `-- submission/                    # Student submissions and grading
|-- entity/                            # Planned MongoDB documents; currently empty
|-- repository/                        # Planned MongoDB repositories; currently empty
`-- service/                            # Planned business services; currently empty

docs/
|-- APIs.MD                            # Planned API catalog
|-- DEPENDENCIES.MD                    # Dependency and platform plan
|-- DTOs.MD                            # Planned request and response objects
|-- ENTITIEs.MD                        # Planned domain documents
|-- REPOSITORIES.MD                    # Planned persistence interfaces
`-- SERVICES.MD                        # Planned business services
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

There are no method-level `GET`, `POST`, `PATCH`, or `DELETE` mappings yet.
Consequently, the detailed endpoint catalog in [docs/APIs.MD](docs/APIs.MD)
represents the planned contract, not a currently callable API.

## Configuration

`src/main/resources/application.yml` expects the MongoDB connection string in
the `MONGODB_URI` environment variable and appends `/majorproject` as the
database name:

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
not yet test HTTP endpoints or business workflows.

## Development Roadmap

1. Define MongoDB entities and enums for users, courses, memberships,
	coursework, submissions, attachments, comments, notifications, and grades.
2. Add repositories and indexes for email, enrollment codes, memberships, and
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
- [Dependency and platform plan](docs/DEPENDENCIES.MD)
- [Planned DTOs](docs/DTOs.MD)
- [Planned entities](docs/ENTITIEs.MD)
- [Planned repositories](docs/REPOSITORIES.MD)
- [Planned services](docs/SERVICES.MD)
- [Spring reference material](HELP.md)
