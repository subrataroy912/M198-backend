# Identity Schema

## Status

The identity portion of the supplied Google Classroom-style schema is
implemented as three MongoDB collections:

- `users`
- `user_profiles`
- `user_oauth`

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

## Security Decisions

- `passwordHash` must contain only a strong adaptive hash, never a plain password.
- OAuth access and refresh tokens must not be returned in profile responses.
- Application refresh tokens are stored as SHA-256 hashes and revoked on rotation or logout.
- OAuth tokens should be encrypted at rest or replaced with a provider-token
  vault reference before production use.
- Email normalization should happen in the registration service before the unique
  index is used.
- Soft deletion should set both `status = DELETED` and `deletedAt` rather than
  immediately destroying audit-relevant identity data.
- Resource authorization is still required for profiles, courses, grades, and
  submissions; the entity model alone does not provide authorization.

## Validation

The existing Maven test suite passes with the identity documents, repositories,
Mongo auditing configuration, and compound indexes loaded by Spring Boot.