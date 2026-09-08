# Profiles Backend Integration Guide

This guide describes how the frontend integrates with the implemented Profiles API.

Base URL examples use:

```text
http://localhost:8080
```

The API base path is `/v1`.

## Authentication

Profile endpoints require an authenticated user. Send the access token returned by the authentication endpoints in the `Authorization` header:

```http
Authorization: Bearer <access-token>
Content-Type: application/json
```

The backend identifies the current user from the JWT subject. The frontend must not send a user ID when reading or updating `/v1/users/me`.

When the access token expires, use the existing refresh-token flow and retry the request with the new access token. Treat `401 Unauthorized` as an authentication state change and redirect to login when token refresh fails.

## Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/v1/users/me` | Read the authenticated user's profile and account information |
| `GET` | `/v1/users/{userId}` | Read another user's public profile when visibility permits |
| `PATCH` | `/v1/users/me` | Partially update the authenticated user's profile |

## Read Current Profile

### Request

```http
GET /v1/users/me
Authorization: Bearer <access-token>
```

### Response

```json
{
  "id": "user-id",
  "email": "student@example.com",
  "accountType": "STUDENT",
  "handle": "ada_lovelace",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "displayName": "Ada Lovelace",
  "avatarUrl": "https://cdn.example.com/avatars/ada.jpg",
  "bannerUrl": "https://cdn.example.com/banners/ada.jpg",
  "headline": "Computer science student",
  "about": "Interested in algorithms and education.",
  "city": "London",
  "country": "United Kingdom",
  "profileVisibility": "PUBLIC",
  "gradeLevel": "12"
}
```

The response may include `email` and `accountType` because this is the account owner's profile. It never includes password hashes or other security credentials.

## Read a Public Profile

### Request

```http
GET /v1/users/{userId}
Authorization: Bearer <access-token>
```

### Response

```json
{
  "id": "user-id",
  "handle": "ada_lovelace",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "displayName": "Ada Lovelace",
  "avatarUrl": "https://cdn.example.com/avatars/ada.jpg",
  "bannerUrl": "https://cdn.example.com/banners/ada.jpg",
  "headline": "Computer science student",
  "about": "Interested in algorithms and education.",
  "city": "London",
  "country": "United Kingdom",
  "profileVisibility": "PUBLIC",
  "gradeLevel": "12"
}
```

Public profile responses intentionally omit `email` and `accountType`. The frontend should not expect those fields from this endpoint.

Visibility rules:

- `PUBLIC`: any authenticated user can read the profile.
- `PRIVATE`: only the profile owner can read the profile.
- `COURSE_MEMBERS`: not available yet. The backend rejects selecting this visibility until course-membership authorization is implemented.
- Missing and soft-deleted profiles return the same `404` response.

Because private profiles return `404` to unauthorized readers, the frontend should show a generic "Profile unavailable" state rather than trying to distinguish a private profile from a missing profile.

## Update Current Profile

The update endpoint uses PATCH semantics. Include only fields that should change. Omitted fields retain their existing values.

### Request

```http
PATCH /v1/users/me
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "displayName": "Ada Lovelace",
  "headline": "Algorithms enthusiast",
  "about": "Interested in algorithms and education.",
  "avatarUrl": "https://cdn.example.com/avatars/ada-new.jpg",
  "city": "London",
  "country": "United Kingdom",
  "gradeLevel": "12",
  "profileVisibility": "PUBLIC"
}
```

### Editable fields

| Field | Type | Maximum length |
| --- | --- | ---: |
| `handle` | string containing letters, numbers, or underscores | 30 |
| `firstName` | string | 100 |
| `lastName` | string | 100 |
| `displayName` | string | 150 |
| `headline` | string | 200 |
| `about` | string | 5000 |
| `avatarUrl` | string | 2048 |
| `bannerUrl` | string | 2048 |
| `city` | string | 100 |
| `country` | string | 100 |
| `gradeLevel` | string | 100 |
| `profileVisibility` | `PUBLIC` or `PRIVATE` | n/a |

The following fields must not be sent as profile update fields because they are not editable through this endpoint:

- `id`
- `email`
- `accountType`
- `passwordHash`
- `status`
- `active`
- `verified`
- `userId`

Handles are optional and unique across profiles. The value is trimmed before
being saved. Send an empty string to clear an existing handle. A handle may
contain only ASCII letters, numbers, and underscores.

Unknown fields should be ignored by the frontend request model. Do not use the current-profile response object as the PATCH request object without filtering it first.

### Successful response

The endpoint returns the updated owner response in the same shape as `GET /v1/users/me`.

## Error Handling

| Status | Meaning | Frontend behavior |
| ---: | --- | --- |
| `400` | Invalid field length, unsupported visibility, or malformed request | Show field/request validation feedback; do not retry unchanged |
| `401` | Missing or invalid access token | Refresh the token or redirect to login |
| `404` | Profile is missing, deleted, or not visible to the caller | Show the generic profile-unavailable state |
| `403` | Access denied by security configuration | Show an access-denied state and do not retry automatically |
| `409` | Existing account conflict from shared authentication behavior | Show the server-provided conflict message |
| `5xx` | Server or infrastructure failure | Show a retryable error state |

Current error bodies use this shape:

```json
{
  "error": "Profile not found"
}
```

For validation errors, the message identifies the first invalid field when available:

```json
{
  "error": "displayName size must be between 0 and 150"
}
```

## Suggested Frontend Data Flow

1. After login or registration, store the access and refresh tokens according to the frontend's security policy.
2. Request `GET /v1/users/me` to hydrate the profile screen.
3. Keep profile form state separate from the response object so read-only account fields cannot be submitted accidentally.
4. On save, send only changed editable fields to `PATCH /v1/users/me`.
5. Replace the local profile state with the PATCH response after a successful update.
6. On `404` from another user's profile, render the generic unavailable state.
7. On `401`, refresh once and retry the original request; avoid infinite retry loops.

## Example TypeScript Types

```ts
export type ProfileVisibility = "PUBLIC" | "PRIVATE";

export interface CurrentUserProfile {
  id: string;
  email: string;
  accountType: "STUDENT" | "TEACHER" | "ADMIN";
  handle?: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  avatarUrl?: string;
  bannerUrl?: string;
  headline?: string;
  about?: string;
  city?: string;
  country?: string;
  profileVisibility?: ProfileVisibility;
  gradeLevel?: string;
}

export interface PublicUserProfile {
  id: string;
  handle?: string;
  firstName?: string;
  lastName?: string;
  displayName?: string;
  avatarUrl?: string;
  bannerUrl?: string;
  headline?: string;
  about?: string;
  city?: string;
  country?: string;
  profileVisibility?: ProfileVisibility;
  gradeLevel?: string;
}

export type UpdateUserProfileRequest = Partial<
  Omit<CurrentUserProfile, "id" | "email" | "accountType">
>;
```

The TypeScript update type should be narrowed further in the frontend if the application model contains additional account or security fields. Only the editable profile fields listed above belong in PATCH requests.
