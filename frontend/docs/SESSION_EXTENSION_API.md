# Session Extension API - Backend Implementation Guide

## Overview

This document specifies the backend API endpoint required to support the session extension feature. This feature allows users to extend their active session before expiration by clicking an "Extend" button in the session expiration notification modal.

## Feature Flow

1. **Session Warning**: When a user's session is about to expire (2 minutes before actual expiration), a modal is displayed showing:
   - A message indicating the session is about to expire
   - An "Extend Session" button
   - A "Sign Out" button

2. **Extension Request**: When the user clicks "Extend Session", the frontend makes an API call to extend the session

3. **Backend Processing**: The backend validates the request and returns a new token with an extended expiration time

4. **Frontend Update**: The frontend receives the new expiration time, reschedules the warning, and displays a success notification

## Backend Implementation Requirements

### 1. API Endpoint Specification

**Endpoint**: `POST /api/auth/extend-session` (or your project's standard endpoint pattern)

**Authentication**: Must be accessible with valid JWT token in the Authorization header

**Request Headers**:
```
Authorization: Bearer <current_access_token>
Content-Type: application/json
```

**Request Body**:
```json
{
  "refreshToken": "string"
}
```

**Example cURL Request**:
```bash
curl -X POST http://localhost:8080/api/auth/extend-session \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "your_refresh_token_here"}'
```

### 2. Response Specification

**Success Response (HTTP 200)**:
```json
{
  "newExpirationTime": 1735689600000
}
```

**Response Details**:
- `newExpirationTime`: Unix timestamp in **milliseconds** (not seconds) indicating when the extended session will expire
  - Example: `1735689600000` = December 31, 2024 at 4:00 PM UTC
  - This should typically be current time + 1 hour (or your standard session timeout)

**Error Response (HTTP 401 - Unauthorized)**:
```json
{
  "error": "Invalid or expired refresh token"
}
```

**Error Response (HTTP 400 - Bad Request)**:
```json
{
  "error": "Refresh token is required"
}
```

**Error Response (HTTP 500 - Server Error)**:
```json
{
  "error": "Failed to extend session"
}
```

### 3. Security Considerations

- **Token Validation**: Always validate that the refresh token is:
  - Present in the request
  - Valid and not expired
  - Associated with the authenticated user
  - Not blacklisted (if your system maintains a token blacklist)

- **Rate Limiting**: Consider implementing rate limiting to prevent abuse:
  - Recommend: Maximum 10 extension requests per session
  - Or: Minimum 30-second interval between extensions

- **CORS**: Ensure the endpoint is accessible from your frontend domain

- **HTTPS**: Require HTTPS for all production deployments

- **Token Rotation**: After extending the session:
  - You may optionally issue a new refresh token
  - If you do, communicate the new token to the frontend (see Response Specification)

- **Logging**: Log all session extension requests for security auditing:
  ```
  [SESSION_EXTENSION] User: {userId}, Timestamp: {timestamp}, Result: {success/failure}
  ```

### 4. Optional Enhancements

#### A. Return New Access Token (Recommended for extra security)

If your architecture allows, you can issue a new access token with the extension:

**Enhanced Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "newExpirationTime": 1735689600000,
  "refreshToken": "optional_new_refresh_token"
}
```

Then update the frontend's `extendSession()` method to store the new tokens.

#### B. Extension History Tracking

Store extension events in your audit log:
```
- Session ID
- User ID
- Extension timestamp
- New expiration time
- Request origin (IP, user agent)
```

#### C. Maximum Extensions Limit

Implement a policy preventing unlimited extensions:
```
Maximum Extensions Per Session: 5
Maximum Total Session Duration: Original 1 hour + (5 × 1 hour) = 6 hours
```

### 5. Implementation Checklist

- [ ] Create POST endpoint at `/api/auth/extend-session`
- [ ] Implement refresh token validation
- [ ] Calculate new expiration time (usually current time + session timeout duration)
- [ ] Return `newExpirationTime` in milliseconds (not seconds)
- [ ] Add proper error handling and HTTP status codes
- [ ] Implement rate limiting if desired
- [ ] Add security logging for audit trail
- [ ] Test with invalid/expired tokens
- [ ] Test with missing refresh token
- [ ] Document endpoint in API documentation
- [ ] Add integration tests
- [ ] Deploy to staging for frontend testing

### 6. Frontend Integration

The frontend expects the following from this endpoint:

1. **Success**: Returns HTTP 200 with `{ newExpirationTime: <milliseconds> }`
2. **Failure**: Returns HTTP 4xx or 5xx with error details
3. **Timing**: Response time should be under 5 seconds (recommend < 2 seconds)

The frontend will:
- Use the new expiration time to reschedule the warning
- Display a success notification: "Session extended successfully"
- On failure: display "Could not extend your session. You will be logged out" and logout after 2 seconds

### 7. Testing the Endpoint

**Test Scenario 1: Successful Extension**
```bash
# With valid tokens
POST /api/auth/extend-session HTTP/1.1
Authorization: Bearer <valid_token>

{"refreshToken": "<valid_refresh_token>"}

# Expected: HTTP 200 with newExpirationTime
```

**Test Scenario 2: Invalid Refresh Token**
```bash
# With invalid refresh token
POST /api/auth/extend-session HTTP/1.1
Authorization: Bearer <valid_token>

{"refreshToken": "invalid_token_xyz"}

# Expected: HTTP 401 with error message
```

**Test Scenario 3: Missing Refresh Token**
```bash
# Missing refresh token
POST /api/auth/extend-session HTTP/1.1
Authorization: Bearer <valid_token>

{}

# Expected: HTTP 400 with error message
```

**Test Scenario 4: Expired Access Token**
```bash
# With expired access token
POST /api/auth/extend-session HTTP/1.1
Authorization: Bearer <expired_token>

{"refreshToken": "<valid_refresh_token>"}

# Expected: HTTP 401 with error message
```

## Frontend Code Reference

The frontend calls this endpoint through the `AuthStore` service with this code:

```typescript
// In authentication.service.ts
extendSession(callback: (expirationTime: number) => void): void {
  const refreshToken = this.getRefreshToken()
  
  this.authStore.extendSession({ refreshToken }).subscribe({
    next: (response: { newExpirationTime: number }) => {
      callback(response.newExpirationTime)
      // Show success notification
    },
    error: (err) => {
      // Show error notification and logout
    }
  })
}
```

The `authStore.extendSession()` method should make the HTTP POST call to the backend endpoint.

## Support & Questions

If you have questions about implementing this endpoint:
1. Check the test scenarios above
2. Ensure the response format matches the specification
3. Verify that timestamps are in milliseconds (not seconds)
4. Check that proper error handling is in place
5. Test with the frontend in both success and failure scenarios

## Session Timeout Configuration

Verify these settings align with your security policy:

- **Initial Session Timeout**: Usually 1 hour
- **Warning Before Expiration**: 2 minutes (hardcoded in frontend)
- **Grace Period After Warning**: 60 seconds (after grace period expires, user is logged out)
- **Maximum Extensions**: Consider implementing a limit (e.g., 5 per session)

These values can be adjusted in the frontend's [session-expiration.service.ts](../src/app/core/services/session-expiration.service.ts) if needed.
