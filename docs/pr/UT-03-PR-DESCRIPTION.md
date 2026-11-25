# UT-03 Contact Access Control - PR Description

## Overview
This PR implements the UT-03 Contact Access Control feature, which controls when HR can view candidate contact information (email and phone). The feature ensures:

1. **Contact info is masked by default** in all APIs returning candidate data
2. **HR can see contact info** only when:
   - The candidate has applied to a job at HR's company (`APPLICATION` path)
   - A STAFF member has approved a contact request (`APPROVED_REQUEST` path)
3. **Every access is logged** in `contact_access_logs` table
4. **Candidates can view** which companies have accessed their contact info

## Database Changes (Flyway Migration V4)

### New Tables

**contact_requests** - HR requests to access candidate contact info
```sql
- id (UUID, PK)
- company_id (UUID, FK) - HR's company
- user_id (UUID, FK) - Candidate's user_id
- job_id (UUID, nullable) - Optional JD reference
- reason (TEXT) - Why HR needs contact info
- status (ENUM: PENDING, APPROVED, REJECTED)
- reviewed_by (UUID, FK) - STAFF who reviewed
- reviewed_at (TIMESTAMPTZ)
- created_at / updated_at (TIMESTAMPTZ)
```

**contact_access_logs** - Audit trail of every contact access
```sql
- id (UUID, PK)
- company_id (UUID, FK) - HR's company
- hr_id (UUID) - HR user who viewed
- user_id (UUID, FK) - Candidate whose contact was viewed
- via (ENUM: APPLICATION, APPROVED_REQUEST) - Access path
- created_at (TIMESTAMPTZ)
```

Indexes on all foreign keys for query performance.

## Backend Changes

### Entities
- `ContactRequest.java` - JPA entity with Lombok, enum `ContactRequestStatus`
- `ContactAccessLog.java` - JPA entity with Lombok, enum `ViaType`

### Repositories
- `ContactRequestRepository` - Custom queries for pending/approved requests
- `ContactAccessLogRepository` - Queries by company, HR, user, date range

### Services
- **ContactAccessService** (core logic):
  - `canSeeContact(companyId, userId)` - Shared function used by ALL APIs
    - Checks application via `recruitment-service` API
    - Checks approved contact requests
  - `requestContactAccess()` - HR creates request
  - `approveRequest()` / `rejectRequest()` - STAFF actions
  - `getContact()` - Returns raw email/phone + logs access
  - `logAccess()` - Creates audit log entry
  - `getAccessLogs()` / `getMyAccessLogs()` - Query logs

- **ProfileQueryService** (updated):
  - `getProfileForPreview(userId, companyId, ...)` now accepts `companyId`
  - Uses `ContactAccessService.canSeeContact()` to determine masking
  - Returns `email`/`phone` as null when access denied, plus `maskedEmail`/`maskedPhone`

- **VisibilityService** (updated):
  - `getPreviewProfile(userId, staffCompanyId)` passes companyId
  - Delegates to `ProfileQueryService`

### Controllers
- **ContactController** (`/api/v1`):
  - HR endpoints:
    - `POST /candidates/{userId}/contact-requests` - Create request
    - `GET /candidates/{userId}/contact` - Get contact (403 if denied)
    - `GET /me/contact-access-logs` - Candidate's own access logs
  - STAFF endpoints:
    - `GET /staff/contact-requests` - List with status filter
    - `POST /staff/contact-requests/{id}/approve` - Approve
    - `POST /staff/contact-requests/{id}/reject` - Reject
    - `GET /staff/contact-access-logs` - Filterable audit logs (company, user, date range)

- **StaffProfileController** (updated):
  - `GET /staff/profiles/{userId}/preview` now resolves staff's companyId
  - Passes companyId to `VisibilityService.getPreviewProfile()`

### DTOs
- `ContactRequestDto` - with nested `CreateRequest` and `RejectRequest`
- `ContactAccessLogDto`

## Frontend Changes

### API Layer
- `contact.api.ts` - TypeScript types and TanStack Query endpoints

### Components/Pages
- **HR**: `ContactAccessSection` component for candidate detail view
  - Shows locked/unlocked/pending states
  - "Xem liên hệ" button when candidate applied
  - "Xin mở liên hệ" modal when not applied
  
- **STAFF**: Two new pages
  - `StaffContactRequestsPage` (`/staff/contact-requests`)
    - Table with approve/reject actions for PENDING requests
    - Search, status filter, pagination
  - `StaffContactAccessLogsPage` (`/staff/contact-access-logs`)
    - Filter by companyId, userId, date range
    - Shows access path (APPLICATION vs APPROVED_REQUEST)
    - Pagination
  
- **Candidate**: `CandidateContactAccessLogs` component added as new tab "Nhật ký liên hệ" in `CandidateProfilePage`

### Routing
- Added routes for STAFF pages in `AppRoutes.tsx` under STAFF/ADMIN role guard

## Acceptance Criteria Coverage

| # | Requirement | Implementation |
|---|-------------|----------------|
| 1 | Contact info masked by default | `ProfileMaskingService` used when `canSeeContact()` returns false |
| 2 | HR sees contact if candidate applied | `hasApplication()` calls recruitment-service |
| 3 | HR sees contact if STAFF approved | `existsApprovedRequest()` query |
| 4 | Contact request flow | HR creates → STAFF approves/rejects |
| 5 | Access logging | `logAccess()` called on every successful `/contact` call |
| 6 | Candidate sees access logs | `/me/contact-access-logs` endpoint + UI tab |
| 7 | STAFF manages requests | `/staff/contact-requests` page with approve/reject |
| 8 | STAFF views audit logs | `/staff/contact-access-logs` with filters |

## API Examples

### HR creates contact request
```bash
curl -X POST http://localhost:8082/api/v1/candidates/{userId}/contact-requests \
  -H "X-User-Id: {hr-uuid}" \
  -H "Content-Type: application/json" \
  -d '{"jobId": "optional-job-uuid", "reason": "Cần liên hệ để phỏng vấn"}'
```

### HR gets contact info
```bash
curl -X GET http://localhost:8082/api/v1/candidates/{userId}/contact \
  -H "X-User-Id: {hr-uuid}"
# Response: { email: "...", phone: "..." } or 403 CONTACT_LOCKED
```

### STAFF approves request
```bash
curl -X POST http://localhost:8082/api/v1/staff/contact-requests/{id}/approve \
  -H "X-User-Id: {staff-uuid}"
```

### STAFF views access logs
```bash
curl -X GET "http://localhost:8082/api/v1/staff/contact-access-logs?from=2026-11-01&to=2026-11-30" \
  -H "X-User-Id: {staff-uuid}"
```

## Files Changed

### Backend (profile-service)
```
src/main/resources/db/migration/V4__contact_access_control.sql
src/main/java/com/example/profileservice/entity/ContactRequest.java
src/main/java/com/example/profileservice/entity/ContactAccessLog.java
src/main/java/com/example/profileservice/repository/ContactRequestRepository.java
src/main/java/com/example/profileservice/repository/ContactAccessLogRepository.java
src/main/java/com/example/profileservice/service/ContactAccessService.java
src/main/java/com/example/profileservice/service/ProfileQueryService.java (modified)
src/main/java/com/example/profileservice/service/VisibilityService.java (modified)
src/main/java/com/example/profileservice/controller/ContactController.java (new)
src/main/java/com/example/profileservice/controller/StaffProfileController.java (modified)
src/main/java/com/example/profileservice/dto/ContactRequestDto.java
src/main/java/com/example/profileservice/dto/ContactAccessLogDto.java
src/test/java/com/example/profileservice/service/VisibilityServiceTest.java (modified)
```

### Frontend
```
src/api/endpoints/contact.api.ts (new)
src/components/hr/ContactAccessSection.tsx (new)
src/pages/staff/StaffContactRequestsPage.tsx (new)
src/pages/staff/StaffContactAccessLogsPage.tsx (new)
src/components/candidate/CandidateContactAccessLogs.tsx (new)
src/pages/candidate/CandidateProfilePage.tsx (modified - added tab)
src/routes/AppRoutes.tsx (modified - added routes)
```

## Testing
- All existing backend tests pass
- Frontend builds successfully (`npm run build`)
- TypeScript compilation clean
- Database migration V4 applies cleanly

## Deployment Notes
1. Run Flyway migration V4 on PostgreSQL
2. Ensure `recruitment-service` is accessible at `http://recruitment-service:8086` for `hasApplication()` check
3. Staff company resolution currently uses placeholder - integrate with company-service for production

## Related Issues
- UT-03 specification
- Depends on: HS-03 (Portfolio - already merged), ProfileMaskingService (existing)