# 🚀 Majorproject Dependency Plan

> **Architecture:** Modular monolith first → microservices when the product and
> team have a real need for independent scaling, deployment, or ownership.

<div align="center">

![Java 25](https://img.shields.io/badge/Java-25-red?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?style=for-the-badge&logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-Database-47A248?style=for-the-badge&logo=mongodb)
![Architecture](https://img.shields.io/badge/Architecture-Modular%20Monolith-blue?style=for-the-badge)

</div>

## 🎯 What This File Is

This is a simple dependency guide for the team. It explains which tools may be
needed to complete the classroom platform and when each tool should be added.

The project currently has controller shells and documentation. Services,
entities, repositories, DTOs, authentication, and endpoint implementations are
not complete yet. This document is a plan only; it does not change the build.

## ✅ Already In The Project

| Name | Used for | Status |
| --- | --- | --- |
| Spring Web MVC | REST controllers and HTTP APIs | ✅ Present |
| Spring Data MongoDB | MongoDB connection and repositories | ✅ Present |
| Lombok | Reducing Java boilerplate | ✅ Present |
| Spring Boot MongoDB Test Support | MongoDB-related tests | ✅ Present |
| Spring Boot MVC Test Support | Controller and web-layer tests | ✅ Present |

## 🧭 Adoption Stages

| Stage | Focus | Recommendation |
| --- | --- | --- |
| 🟢 Stage 1 | Complete the modular monolith | Add now, when each feature is implemented |
| 🟡 Stage 2 | Product features | Add only when the feature needs it |
| 🔵 Stage 3 | Production operations | Add before serious production traffic |
| 🟣 Stage 4 | Microservices | Add only after boundaries are proven |

---

## 🟢 Stage 1: Modular Monolith Foundation

### 🌐 API and validation

| Name | Why the team may need it |
| --- | --- |
| Spring Validation | Validate registration, course, coursework, submission, and profile requests |
| Spring Boot Actuator | Health checks, readiness checks, and operational information |
| Springdoc OpenAPI | Generate API documentation and a Swagger UI |
| Jackson Java Time Support | Correct JSON handling for dates and times |

### 🔐 Security and identity

| Name | Why the team may need it |
| --- | --- |
| Spring Security | Login, authorization, password protection, and protected routes |
| Spring Security JWT Support | Validate access tokens for API requests |
| OAuth2 Client | Optional Google, Microsoft, or other external login |
| Spring Security Test Support | Test roles, users, JWT claims, and permissions |

**Recommended direction:** Use short-lived access tokens and securely stored
refresh tokens. Choose one token approach and document it. Do not combine
multiple JWT libraries without a clear reason.

### 🗄️ Database and persistence

| Name | Why the team may need it |
| --- | --- |
| MongoDB | Users, courses, memberships, coursework, submissions, and notifications |
| MongoDB Migration Tool | Versioned indexes and database changes |
| Redis | Cache, rate limits, short-lived data, and distributed locks when needed |
| Redis Session Support | Only if the team chooses server-managed sessions instead of JWT |

MongoDB transactions require a replica set or MongoDB Atlas. A simple standalone
local database should not be treated as production-equivalent.

### 🔁 DTO mapping

| Name | Why the team may need it |
| --- | --- |
| MapStruct | Convert safely between DTOs and database documents |
| Lombok | Generate getters, setters, builders, and constructors where appropriate |

DTOs should be used at the API boundary. Database documents should never be
returned directly from controllers, especially for users, tokens, and grades.

### 🧪 Testing and code quality

| Name | Why the team may need it |
| --- | --- |
| Spring Boot Test Support | Application, service, and controller testing |
| JUnit | Unit and integration tests |
| Mockito | Isolate services and external providers in unit tests |
| Testcontainers | Run realistic MongoDB integration tests |
| Awaitility | Test asynchronous notifications and jobs |
| ArchUnit | Enforce domain boundaries inside the monolith |
| JaCoCo | Generate test coverage reports |
| Maven Enforcer | Enforce Java, Maven, and dependency rules |
| OWASP Dependency Check | Detect vulnerable dependencies in CI |

Use Testcontainers for repository behavior and mock external providers such as
email, object storage, and push notifications in unit tests.

---

## 🟡 Stage 2: Product Features

### 📎 Attachments and file storage

Choose **one** cloud storage provider. The application should hide it behind an
internal `ObjectStorageService` so the provider can be changed later.

| Name | Why the team may need it |
| --- | --- |
| Amazon S3 | Private files and presigned upload/download links |
| Google Cloud Storage | Alternative cloud object storage |
| Azure Blob Storage | Alternative cloud object storage |
| MinIO | Local S3-compatible development storage |
| LocalStack | Local AWS-compatible integration testing |
| Apache Tika | Detect real file types and reject unsafe uploads |
| Image Processing Library | Resize avatars and create safe previews |

Store file metadata in MongoDB and binary files in object storage. Add file-size
limits, ownership checks, expiry, malware scanning, and retention rules.

### 📧 Email and notifications

| Name | Why the team may need it |
| --- | --- |
| Spring Mail | Email verification, password reset, and course notifications |
| Quartz Scheduler | Durable reminders and due-date jobs |
| ShedLock | Prevent duplicate scheduled jobs when multiple app instances run |
| Firebase Admin | Optional mobile push notifications |

Basic Spring scheduling may be enough for one instance. Add Quartz or ShedLock
when jobs must survive restarts or run across multiple instances.

### 🔎 Search and discovery

| Name | Why the team may need it |
| --- | --- |
| MongoDB Text Search | Start with simple public-course search |
| MongoDB Atlas Search | Managed search, filtering, and ranking at scale |
| Elasticsearch | Separate search platform for advanced search needs |
| OpenSearch | Alternative separate search platform |

Choose one search platform. Do not add Elasticsearch and OpenSearch together.

### 📊 Reports and exports

| Name | Why the team may need it |
| --- | --- |
| Apache POI | Excel gradebooks and roster exports |
| PDF Generation Library | Grade reports and printable documents |
| Apache Commons CSV | CSV exports |

Large reports should run as background jobs and produce a downloadable file
instead of keeping an HTTP request open.

### 💬 Realtime features

| Name | Why the team may need it |
| --- | --- |
| Spring WebSocket | Realtime comments, notifications, and presence |
| STOMP | Message format for Spring WebSocket features |
| Redis Pub/Sub | Share realtime events across application instances |

Add realtime transport only after the normal REST workflows are stable.

---

## 🔵 Stage 3: Production Reliability

| Name | Why the team may need it |
| --- | --- |
| Spring Boot Actuator | Application health and operational endpoints |
| Prometheus | Collect application metrics |
| Grafana | Dashboards for metrics and service health |
| OpenTelemetry | Distributed traces and telemetry collection |
| Resilience4j | Timeouts, retries, circuit breakers, and bulkheads |
| Structured Logging | Searchable JSON logs in production |
| Sentry or Error Tracking | Runtime error reports and diagnostics |
| Secret Manager | Store and rotate database, token, and cloud credentials |

Always configure timeouts. Retry only safe, idempotent operations. Do not
blindly retry enrollment writes, grading, authentication, or file deletion.

---

## 🟣 Stage 4: Future Microservices Platform

Do not add this stack while the application is still a small modular monolith.
Introduce it only when the team has clear bounded contexts and a real need for
independent deployment or scaling.

### 📨 Messaging and events

| Name | Why the team may need it |
| --- | --- |
| Apache Kafka | Durable event streaming between services |
| Spring Kafka | Connect Spring services to Kafka |
| RabbitMQ | Task queues and simpler command messaging |
| Spring AMQP | Connect Spring services to RabbitMQ |
| Avro or Protocol Buffers | Versioned event and service contracts |
| Schema Registry | Manage compatible event schemas |
| Debezium | Database change events and change-data capture |

Choose Kafka or RabbitMQ for a specific messaging design unless there is a
strong reason to operate both.

### 🔗 Service-to-service communication

| Name | Why the team may need it |
| --- | --- |
| Spring RestClient | Simple synchronous HTTP calls |
| Spring WebClient | Non-blocking calls and streaming integrations |
| Spring Cloud OpenFeign | Declarative HTTP clients between services |
| gRPC | Typed, high-performance internal communication |
| Spring Cloud Circuit Breaker | Standard resilience around remote calls |

### 🚪 Gateway and service platform

| Name | Why the team may need it |
| --- | --- |
| Spring Cloud Gateway | Routing, edge policies, and rate limits |
| Spring Cloud Config | Central configuration for services |
| Kubernetes | Deployment, scaling, service discovery, and health management |
| Kubernetes Ingress | External traffic routing |
| Consul or Eureka | Service discovery when the platform does not provide it |
| Spring Cloud Kubernetes | Spring integration with Kubernetes |

Spring Cloud versions must match the Spring Boot version. Kubernetes already
provides many capabilities that older Spring Cloud components provide, so the
team should avoid duplicating platform features.

### 🔄 Distributed workflows and secrets

| Name | Why the team may need it |
| --- | --- |
| Redis | Cross-service cache, locks, sessions, and rate limits |
| Temporal | Durable long-running workflows and retries |
| Camunda | Business process and workflow automation alternative |
| Transactional Outbox | Reliable publication of database events |
| HashiCorp Vault | Central secret storage and rotation |
| Cloud Secret Manager | Cloud-native secret storage alternative |

Avoid distributed transactions. Prefer idempotent commands, an outbox pattern,
deduplication, retries, and compensating actions.

---

## 🧩 Team Decisions Required

Before adding the next group of dependencies, decide:

- 🔐 JWT access tokens or server-managed sessions.
- 🗄️ MongoDB migration approach.
- 📦 Object storage provider.
- 📧 Email and push notification providers.
- 🔎 MongoDB search or a separate search platform.
- 📨 Kafka or RabbitMQ for asynchronous events.
- 📈 Metrics and tracing platform.
- ☸️ Kubernetes-native tools or Spring Cloud infrastructure.
- 📄 PDF and report requirements.
- 🛡️ Data retention, audit logs, and student privacy rules.

## 🛡️ Security Baseline

Dependencies do not automatically make the platform secure. The team must also
implement:

- Password hashing with an adaptive password encoder.
- JWT key rotation and secure refresh-token storage.
- Role and resource-level access checks for every course operation.
- Submission, grade, profile, and attachment privacy.
- Request validation and upload limits.
- File type verification and malware scanning.
- Audit logs for login, enrollment, grading, deletion, and permission changes.
- Secret rotation and environment-specific configuration.
- Rate limiting for login, registration, enrollment codes, and file URLs.
- Account deletion and student-data retention policies.
- Automated dependency updates and vulnerability scanning.

## 🗺️ Recommended Order

1. 🧱 Build entities, repositories, services, DTOs, and validation.
2. 🔐 Add Spring Security and authorization tests.
3. 🧪 Add Testcontainers, architecture tests, coverage, and vulnerability scanning.
4. 📎 Add object storage and secure attachment handling.
5. 📧 Add email, notifications, scheduling, and analytics.
6. 📈 Add metrics, traces, structured logs, and dashboards.
7. ⚡ Add Redis only for measured caching, rate limits, or coordination needs.
8. 🟣 Extract services only after the modular-monolith boundaries are proven.

> **Golden rule:** Start with the smallest dependable dependency set. Add
> complexity when the product, traffic, or team structure earns it.
