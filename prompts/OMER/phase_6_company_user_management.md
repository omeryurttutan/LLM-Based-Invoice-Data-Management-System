# PHASE 6: COMPANY AND USER MANAGEMENT API

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Backend**: Java 17 + Spring Boot 3.2 (Hexagonal Architecture)
- **Database**: PostgreSQL 15+ with Flyway migrations
- **Security**: JWT Authentication + RBAC (4 roles)

### Current State
**Phases 0-5 have been completed:**
- ✅ Phase 0: Docker Compose environment (PostgreSQL, Redis, RabbitMQ)
- ✅ Phase 1: CI/CD Pipeline with GitHub Actions
- ✅ Phase 2: Hexagonal Architecture layer structure with ArchUnit tests
- ✅ Phase 3: Database schema (companies, users, invoices, invoice_items, categories, audit_logs tables) with Flyway migrations
- ✅ Phase 4: JWT Authentication (register, login, refresh, logout, brute force protection)
- ✅ Phase 5: RBAC with 4 roles (ADMIN, MANAGER, ACCOUNTANT, INTERN), Permission enum, @PreAuthorize annotations, company-based multi-tenant isolation, CompanyContextFilter, SecurityExpressionService

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer - Backend focused)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Implement full CRUD endpoints for Company management and User management. ADMIN users must be able to create/update/delete companies, manage users within their company (create, activate/deactivate, assign roles), and list users per company. All users should be able to update their own profile information. This phase builds the administrative backbone that enables multi-tenant user and organization management.

---

## ROLE PERMISSION MATRIX FOR THIS PHASE

```
┌──────────────────────────┬────────┬─────────┬─────────────┬──────────┐
│ Operation                │ ADMIN  │ MANAGER │ ACCOUNTANT  │  INTERN  │
├──────────────────────────┼────────┼─────────┼─────────────┼──────────┤
│ Create Company           │   ✅   │   ❌    │     ❌      │    ❌    │
│ View Company Details     │   ✅   │   ✅    │     ✅      │    ✅    │
│ Update Company           │   ✅   │   ❌    │     ❌      │    ❌    │
│ Delete Company           │   ✅   │   ❌    │     ❌      │    ❌    │
├──────────────────────────┼────────┼─────────┼─────────────┼──────────┤
│ List Users (own company) │   ✅   │   ✅    │     ❌      │    ❌    │
│ Create User              │   ✅   │   ❌    │     ❌      │    ❌    │
│ Update User (by admin)   │   ✅   │   ❌    │     ❌      │    ❌    │
│ Delete User              │   ✅   │   ❌    │     ❌      │    ❌    │
│ Assign/Change Role       │   ✅   │   ❌    │     ❌      │    ❌    │
│ Toggle Active/Inactive   │   ✅   │   ❌    │     ❌      │    ❌    │
├──────────────────────────┼────────┼─────────┼─────────────┼──────────┤
│ View Own Profile         │   ✅   │   ✅    │     ✅      │    ✅    │
│ Update Own Profile       │   ✅   │   ✅    │     ✅      │    ✅    │
└──────────────────────────┴────────┴─────────┴─────────────┴──────────┘
```

---

## DETAILED REQUIREMENTS

### 1. Company Domain Model

**Purpose**: Create the Company domain entity and all related layers following Hexagonal Architecture.

**1.1 Domain Layer**

**File**: `domain/company/entity/Company.java`

The Company domain entity should include:
- `id` (UUID) - Primary key
- `name` (String, required, max 255) - Company name
- `taxNumber` (String, unique, max 20) - Turkish Tax Number (VKN)
- `taxOffice` (String, max 255) - Tax office name (Vergi Dairesi)
- `address` (String, text) - Full address
- `city` (String, max 100)
- `district` (String, max 100)
- `postalCode` (String, max 10)
- `phone` (String, max 20)
- `email` (String, max 255)
- `website` (String, max 255)
- `defaultCurrency` (String, default "TRY") - Default currency for invoices
- `invoicePrefix` (String, max 10) - Prefix for auto-generated invoice numbers
- `isActive` (boolean, default true)
- `isDeleted` (boolean, default false) - Soft delete flag
- `deletedAt` (LocalDateTime) - Soft delete timestamp
- `createdAt` / `updatedAt` (LocalDateTime) - Audit timestamps

Domain validation rules:
- Name is required and must not be blank
- Tax number must be exactly 10 digits (Turkish VKN format) if provided
- Email must be valid format if provided
- Phone must follow Turkish phone format if provided

**File**: `domain/company/port/CompanyRepository.java`

Repository port (interface) in domain layer:
- `save(Company)` → Company
- `findById(UUID)` → Optional<Company>
- `findByTaxNumber(String)` → Optional<Company>
- `findAllActive(Pageable)` → Page<Company>
- `findAllByIsDeletedFalse(Pageable)` → Page<Company>
- `existsByTaxNumber(String)` → boolean
- `softDelete(UUID)` → void

**1.2 Application Layer**

**File**: `application/company/CompanyService.java`

Use cases to implement:
- `createCompany(CreateCompanyCommand)` → CompanyResponse
- `updateCompany(UUID, UpdateCompanyCommand)` → CompanyResponse
- `getCompanyById(UUID)` → CompanyResponse
- `getCompany()` → CompanyResponse (get current user's company)
- `listCompanies(Pageable)` → Page<CompanyResponse>
- `deleteCompany(UUID)` → void (soft delete)
- `activateCompany(UUID)` → CompanyResponse
- `deactivateCompany(UUID)` → CompanyResponse

Business rules:
- Tax number must be unique across all companies
- Cannot delete a company that has active users (or handle cascading deactivation)
- Soft delete: set `is_deleted = true`, `deleted_at = now()`
- Only ADMIN can perform create/update/delete operations
- All users can view their own company details

**1.3 Infrastructure Layer**

**File**: `infrastructure/persistence/company/CompanyJpaEntity.java`

JPA entity mapping to the existing `companies` table from Phase 3.

**File**: `infrastructure/persistence/company/CompanyJpaRepository.java`

Spring Data JPA repository interface.

**File**: `infrastructure/persistence/company/CompanyRepositoryAdapter.java`

Adapter that implements the domain port and delegates to JPA repository. Handles mapping between domain entity and JPA entity.

**File**: `infrastructure/persistence/company/CompanyMapper.java`

MapStruct or manual mapper between domain entity ↔ JPA entity.

**1.4 Interfaces Layer**

**File**: `interfaces/rest/company/CompanyController.java`

REST controller with all company endpoints.

**File**: `interfaces/rest/company/dto/CreateCompanyRequest.java`
**File**: `interfaces/rest/company/dto/UpdateCompanyRequest.java`
**File**: `interfaces/rest/company/dto/CompanyResponse.java`

DTO classes with Jakarta validation annotations.

---

### 2. User Management (Admin Operations)

**Purpose**: Allow ADMIN users to manage other users within their company.

**2.1 Application Layer**

**File**: `application/user/UserManagementService.java`

This is separate from the AuthService (Phase 4). AuthService handles login/register/token operations. UserManagementService handles ADMIN user management operations:

- `createUser(CreateUserCommand)` → UserResponse
  - ADMIN creates a user within their own company
  - Generates a temporary password or uses provided password
  - Sets initial role as specified
  - New user belongs to same company as the ADMIN
- `updateUser(UUID, UpdateUserCommand)` → UserResponse
  - ADMIN can update user's full_name, phone, role
  - Cannot change user's email (immutable after creation) or password (user does this themselves)
- `getUserById(UUID)` → UserResponse
  - Must verify user belongs to same company
- `listUsersByCompany(UUID companyId, Pageable)` → Page<UserResponse>
  - ADMIN and MANAGER can list users
  - Automatically scoped to current user's company
- `deleteUser(UUID)` → void
  - Soft delete
  - Cannot delete yourself
  - Cannot delete the last ADMIN of a company
- `toggleUserActive(UUID)` → UserResponse
  - Activate or deactivate a user
  - Deactivated users cannot login
  - Cannot deactivate yourself
- `changeUserRole(UUID, Role)` → UserResponse
  - ADMIN can change any user's role
  - Cannot change own role (prevents accidental ADMIN demotion)
  - Cannot promote to ADMIN if already at max ADMIN count (optional business rule)

Business rules:
- All user management operations are company-scoped (multi-tenant isolation)
- ADMIN can only manage users within their own company
- Email uniqueness is per company (email + company_id unique constraint)
- Password is hashed with BCrypt (strength 12) when creating users
- Soft delete: set `is_deleted = true`, `deleted_at = now()`

**2.2 Interfaces Layer**

**File**: `interfaces/rest/user/UserManagementController.java`

REST controller for ADMIN user management operations.

**File**: `interfaces/rest/user/dto/CreateUserRequest.java`
**File**: `interfaces/rest/user/dto/UpdateUserRequest.java`
**File**: `interfaces/rest/user/dto/ChangeRoleRequest.java`
**File**: `interfaces/rest/user/dto/UserResponse.java`
**File**: `interfaces/rest/user/dto/UserListResponse.java`

DTO classes with proper validation.

---

### 3. Profile Management (Self-Service)

**Purpose**: Allow any authenticated user to view and update their own profile.

**File**: `application/user/ProfileService.java`

- `getMyProfile()` → UserProfileResponse
  - Returns current authenticated user's profile information
  - Includes company name and role
- `updateMyProfile(UpdateProfileCommand)` → UserProfileResponse
  - User can update: full_name, phone, avatar_url
  - User CANNOT change: email, role, company, password (password change is separate)
- `changeMyPassword(ChangePasswordCommand)` → void
  - Requires current password verification
  - New password must meet strength requirements
  - Update `password_changed_at` timestamp

**File**: `interfaces/rest/user/ProfileController.java`

**File**: `interfaces/rest/user/dto/UserProfileResponse.java`
**File**: `interfaces/rest/user/dto/UpdateProfileRequest.java`
**File**: `interfaces/rest/user/dto/ChangePasswordRequest.java`

---

### 4. Company-Scoped User Listing

**Purpose**: Provide endpoint to list users within a specific company.

**Endpoint**: `GET /api/v1/companies/{companyId}/users`

- Only ADMIN and MANAGER can access this endpoint
- Automatically validates that the companyId matches the authenticated user's company
- Returns 403 if trying to access another company's users
- Supports pagination and sorting
- Filters out soft-deleted users by default
- Optional query parameters:
  - `role` - Filter by role (ADMIN, MANAGER, ACCOUNTANT, INTERN)
  - `isActive` - Filter by active status (true/false)
  - `search` - Search by name or email (partial match)

---

## API ENDPOINTS

### Company Endpoints

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| POST | `/api/v1/companies` | Create new company | Yes | ADMIN |
| GET | `/api/v1/companies/me` | Get current user's company | Yes | ALL |
| GET | `/api/v1/companies/{id}` | Get company by ID | Yes | ADMIN |
| PUT | `/api/v1/companies/{id}` | Update company | Yes | ADMIN |
| DELETE | `/api/v1/companies/{id}` | Soft delete company | Yes | ADMIN |
| PATCH | `/api/v1/companies/{id}/activate` | Activate company | Yes | ADMIN |
| PATCH | `/api/v1/companies/{id}/deactivate` | Deactivate company | Yes | ADMIN |

### User Management Endpoints (ADMIN only)

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| POST | `/api/v1/users` | Create new user | Yes | ADMIN |
| GET | `/api/v1/users` | List users in my company | Yes | ADMIN, MANAGER |
| GET | `/api/v1/users/{id}` | Get user by ID | Yes | ADMIN, MANAGER |
| PUT | `/api/v1/users/{id}` | Update user | Yes | ADMIN |
| DELETE | `/api/v1/users/{id}` | Soft delete user | Yes | ADMIN |
| PATCH | `/api/v1/users/{id}/toggle-active` | Toggle active status | Yes | ADMIN |
| PATCH | `/api/v1/users/{id}/role` | Change user role | Yes | ADMIN |

### Company-Scoped User Listing

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| GET | `/api/v1/companies/{id}/users` | List users of a company | Yes | ADMIN, MANAGER |

### Profile Endpoints (Self-Service)

| Method | Endpoint | Description | Auth | Role |
|--------|----------|-------------|------|------|
| GET | `/api/v1/profile` | Get my profile | Yes | ALL |
| PUT | `/api/v1/profile` | Update my profile | Yes | ALL |
| POST | `/api/v1/profile/change-password` | Change my password | Yes | ALL |

---

## TECHNICAL SPECIFICATIONS

### Request/Response DTOs

#### CreateCompanyRequest
```json
{
  "name": "Akdağ Muhasebe Ltd.",          // required, max 255
  "taxNumber": "1234567890",               // optional, exactly 10 digits
  "taxOffice": "Kadıköy Vergi Dairesi",   // optional, max 255
  "address": "Moda Cad. No:12",           // optional
  "city": "İstanbul",                      // optional, max 100
  "district": "Kadıköy",                   // optional, max 100
  "postalCode": "34710",                   // optional, max 10
  "phone": "+905551234567",                // optional, max 20
  "email": "info@akdag.com",              // optional, valid email
  "website": "https://akdag.com",          // optional, max 255
  "defaultCurrency": "TRY",               // optional, default "TRY"
  "invoicePrefix": "AKD"                   // optional, max 10
}
```

#### CompanyResponse
```json
{
  "id": "uuid-here",
  "name": "Akdağ Muhasebe Ltd.",
  "taxNumber": "1234567890",
  "taxOffice": "Kadıköy Vergi Dairesi",
  "address": "Moda Cad. No:12",
  "city": "İstanbul",
  "district": "Kadıköy",
  "postalCode": "34710",
  "phone": "+905551234567",
  "email": "info@akdag.com",
  "website": "https://akdag.com",
  "defaultCurrency": "TRY",
  "invoicePrefix": "AKD",
  "isActive": true,
  "createdAt": "2026-02-10T14:30:00Z",
  "updatedAt": "2026-02-10T14:30:00Z",
  "userCount": 5
}
```

#### CreateUserRequest
```json
{
  "email": "ahmet@akdag.com",             // required, valid email
  "fullName": "Ahmet Yılmaz",             // required, max 100
  "password": "SecurePass123!",            // required, min 8, must include uppercase+lowercase+digit+special
  "phone": "+905551234567",                // optional
  "role": "ACCOUNTANT"                     // required, one of: ADMIN, MANAGER, ACCOUNTANT, INTERN
}
```

#### UserResponse
```json
{
  "id": "uuid-here",
  "email": "ahmet@akdag.com",
  "fullName": "Ahmet Yılmaz",
  "phone": "+905551234567",
  "avatarUrl": null,
  "role": "ACCOUNTANT",
  "companyId": "company-uuid",
  "companyName": "Akdağ Muhasebe Ltd.",
  "isActive": true,
  "emailVerified": false,
  "lastLoginAt": null,
  "createdAt": "2026-02-10T14:30:00Z",
  "updatedAt": "2026-02-10T14:30:00Z"
}
```

#### UpdateProfileRequest
```json
{
  "fullName": "Ahmet Yılmaz Updated",     // optional, max 100
  "phone": "+905559876543",                // optional
  "avatarUrl": "https://cdn.example.com/avatar.jpg"  // optional
}
```

#### ChangePasswordRequest
```json
{
  "currentPassword": "OldPass123!",        // required
  "newPassword": "NewSecurePass456!",      // required, min 8
  "confirmPassword": "NewSecurePass456!"   // required, must match newPassword
}
```

### Validation Rules

All request DTOs must use Jakarta Bean Validation annotations:

- `@NotBlank` for required string fields
- `@Size(max = ...)` for string length limits
- `@Email` for email format
- `@Pattern(regexp = "^[0-9]{10}$")` for Turkish tax number
- `@Pattern(regexp = "^\\+?[0-9]{10,15}$")` for phone numbers
- Custom `@ValidPassword` annotation or `@Pattern` for password strength:
  - Minimum 8 characters
  - At least one uppercase letter
  - At least one lowercase letter
  - At least one digit
  - At least one special character

### Error Response Format

Follow the consistent error format established in Phase 2:

```json
{
  "success": false,
  "error": {
    "code": "COMPANY_TAX_NUMBER_EXISTS",
    "message": "A company with this tax number already exists",
    "details": {
      "taxNumber": "1234567890"
    },
    "timestamp": "2026-02-10T14:30:00Z"
  }
}
```

Error codes to implement:
- `COMPANY_NOT_FOUND` - Company with given ID not found
- `COMPANY_TAX_NUMBER_EXISTS` - Tax number already in use
- `COMPANY_HAS_ACTIVE_USERS` - Cannot delete company with active users
- `USER_NOT_FOUND` - User with given ID not found
- `USER_EMAIL_EXISTS` - Email already exists in this company
- `USER_CANNOT_DELETE_SELF` - Admin cannot delete themselves
- `USER_CANNOT_DEACTIVATE_SELF` - Admin cannot deactivate themselves
- `USER_CANNOT_CHANGE_OWN_ROLE` - Admin cannot change their own role
- `USER_LAST_ADMIN` - Cannot delete/demote the last admin of a company
- `COMPANY_ACCESS_DENIED` - Trying to access another company's data
- `INVALID_CURRENT_PASSWORD` - Wrong current password when changing password
- `PASSWORD_SAME_AS_OLD` - New password same as current password

---

## PAGINATION SPECIFICATION

All list endpoints must support pagination:

**Query Parameters**:
- `page` - Page number (0-based, default 0)
- `size` - Page size (default 20, max 100)
- `sort` - Sort field (e.g., `createdAt,desc` or `fullName,asc`)

**Response Format** (wrapping Page object):
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

---

## DATABASE CHANGES

### No New Tables Required
This phase uses the existing `companies` and `users` tables created in Phase 3.

### Potential Migration (if needed)
If during implementation you discover missing columns or constraints, create a new migration file:

**File**: `backend/src/main/resources/db/migration/V4__faz_6_company_user_adjustments.sql`

Document any schema changes clearly with comments. Examples of potential additions:
- If `users.avatar_url` column doesn't exist, add it
- If additional indexes are needed for search/filter queries
- If you need a `user_count` view or materialized view

**IMPORTANT**: Always check existing schema first. Only create migration if truly needed. Do NOT recreate existing tables.

---

## TESTING REQUIREMENTS

### Unit Tests

1. **CompanyServiceTest**:
   - Create company successfully
   - Create company with duplicate tax number → error
   - Update company successfully
   - Delete company (soft delete)
   - Delete company with active users → error
   - Get company by ID → not found

2. **UserManagementServiceTest**:
   - Create user successfully
   - Create user with duplicate email in same company → error
   - Create user with duplicate email in different company → success
   - Update user role
   - Delete user (soft delete)
   - Delete self → error
   - Deactivate self → error
   - Change own role → error
   - Delete last admin → error
   - Toggle active status

3. **ProfileServiceTest**:
   - Get my profile
   - Update my profile
   - Change password with correct current password → success
   - Change password with wrong current password → error
   - Change password same as old → error

### Integration Tests

1. **CompanyController Integration Tests**:
   - POST /companies with valid data → 201 Created
   - POST /companies with duplicate tax number → 409 Conflict
   - GET /companies/me → 200 OK with current user's company
   - PUT /companies/{id} → 200 OK
   - DELETE /companies/{id} → 204 No Content
   - Non-ADMIN accessing company endpoints → 403 Forbidden

2. **UserManagementController Integration Tests**:
   - POST /users with valid data → 201 Created
   - GET /users → 200 OK with paginated list
   - PATCH /users/{id}/role with valid role → 200 OK
   - PATCH /users/{id}/toggle-active → 200 OK
   - Non-ADMIN accessing user management → 403 Forbidden
   - Accessing user from another company → 403 Forbidden

3. **ProfileController Integration Tests**:
   - GET /profile → 200 OK
   - PUT /profile → 200 OK
   - POST /profile/change-password → 200 OK
   - All roles can access profile endpoints → 200 OK

### Manual Testing Steps

1. **Login as ADMIN** (use admin@demo.com / Admin123! from Phase 4)
2. **Create a company**:
   ```
   POST /api/v1/companies
   Body: { "name": "Test Company", "taxNumber": "1234567890" }
   → Expect: 201 Created
   ```
3. **View company details**:
   ```
   GET /api/v1/companies/me
   → Expect: 200 OK with company info
   ```
4. **Create a new user**:
   ```
   POST /api/v1/users
   Body: { "email": "user@test.com", "fullName": "Test User", "password": "Test123!", "role": "ACCOUNTANT" }
   → Expect: 201 Created
   ```
5. **List users**:
   ```
   GET /api/v1/users?page=0&size=10&sort=fullName,asc
   → Expect: 200 OK with paginated list
   ```
6. **Change user role**:
   ```
   PATCH /api/v1/users/{userId}/role
   Body: { "role": "MANAGER" }
   → Expect: 200 OK
   ```
7. **Toggle user active status**:
   ```
   PATCH /api/v1/users/{userId}/toggle-active
   → Expect: 200 OK with updated user (isActive toggled)
   ```
8. **Login as the new user and update profile**:
   ```
   PUT /api/v1/profile
   Body: { "fullName": "Updated Name", "phone": "+905551112233" }
   → Expect: 200 OK
   ```
9. **Change password**:
   ```
   POST /api/v1/profile/change-password
   Body: { "currentPassword": "Test123!", "newPassword": "NewPass456!", "confirmPassword": "NewPass456!" }
   → Expect: 200 OK
   ```
10. **Test authorization**:
    - Login as ACCOUNTANT → try POST /api/v1/users → Expect: 403
    - Login as INTERN → try DELETE /api/v1/users/{id} → Expect: 403
    - Login as MANAGER → try GET /api/v1/users → Expect: 200 OK (read access)

---

## VERIFICATION CHECKLIST

After completing this phase, verify:

- [ ] Company CRUD endpoints work correctly (POST, GET, PUT, DELETE)
- [ ] GET /api/v1/companies/me returns current user's company
- [ ] Tax number uniqueness is enforced
- [ ] Company soft delete works (is_deleted flag, deleted_at timestamp)
- [ ] User CRUD endpoints work correctly (POST, GET, PUT, DELETE)
- [ ] ADMIN can create users within their company
- [ ] ADMIN can change user roles
- [ ] ADMIN can toggle user active/inactive status
- [ ] ADMIN cannot delete themselves
- [ ] ADMIN cannot deactivate themselves
- [ ] ADMIN cannot change their own role
- [ ] Last ADMIN of a company cannot be deleted/demoted
- [ ] Company-based isolation: users can only see their own company's data
- [ ] GET /api/v1/companies/{id}/users returns company-scoped user list
- [ ] Profile endpoints work for all roles (GET /profile, PUT /profile, POST /profile/change-password)
- [ ] Password change requires correct current password
- [ ] Pagination works on all list endpoints
- [ ] Sorting works (by name, email, createdAt, role)
- [ ] Search/filter works on user listing (by role, active status, name/email)
- [ ] Proper error responses with specific error codes
- [ ] Input validation works (DTO annotations)
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] No linting errors
- [ ] Code compiles and runs without errors
- [ ] CI pipeline passes

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_6_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (2-3 days)

### 2. Completed Tasks
List each task with checkbox:
- [ ] Company domain model (entity, repository port, service)
- [ ] Company infrastructure (JPA entity, JPA repository, adapter, mapper)
- [ ] Company REST controller with DTOs
- [ ] User management service
- [ ] User management controller with DTOs
- [ ] Profile service
- [ ] Profile controller with DTOs
- [ ] Company-scoped user listing endpoint
- [ ] Pagination support
- [ ] Input validation
- [ ] Error handling with specific error codes
- [ ] Unit tests
- [ ] Integration tests

### 3. Files Created/Modified
Organized by layer:
```
domain/company/
├── entity/Company.java
└── port/CompanyRepository.java

application/company/
├── CompanyService.java
├── dto/CreateCompanyCommand.java
├── dto/UpdateCompanyCommand.java
└── dto/CompanyResponse.java

application/user/
├── UserManagementService.java
├── ProfileService.java
├── dto/CreateUserCommand.java
├── dto/UpdateUserCommand.java
├── dto/ChangeRoleCommand.java
├── dto/UpdateProfileCommand.java
├── dto/ChangePasswordCommand.java
├── dto/UserResponse.java
└── dto/UserProfileResponse.java

infrastructure/persistence/company/
├── CompanyJpaEntity.java
├── CompanyJpaRepository.java
├── CompanyRepositoryAdapter.java
└── CompanyMapper.java

interfaces/rest/company/
├── CompanyController.java
└── dto/
    ├── CreateCompanyRequest.java
    ├── UpdateCompanyRequest.java
    └── CompanyResponse.java

interfaces/rest/user/
├── UserManagementController.java
├── ProfileController.java
└── dto/
    ├── CreateUserRequest.java
    ├── UpdateUserRequest.java
    ├── ChangeRoleRequest.java
    ├── UpdateProfileRequest.java
    ├── ChangePasswordRequest.java
    ├── UserResponse.java
    ├── UserListResponse.java
    └── UserProfileResponse.java
```

### 4. API Endpoints Summary
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | /api/v1/companies | Create company | ✅/❌ |
| GET | /api/v1/companies/me | Get my company | ✅/❌ |
| ... | ... | ... | ... |

### 5. Test Results
Include test output counts (passed/failed/skipped) and any relevant curl command outputs.

### 6. Database Changes
- List any migration files created (or state "No migration needed")
- Document any schema observations or concerns for future phases

### 7. Issues Encountered
Document any problems and their solutions.

### 8. Next Steps
What needs to be done in Phase 7 (Invoice CRUD API). Include any observations about:
- Additional database columns/indexes that might be needed
- Integration points with the next phase
- Any technical debt created

### 9. Time Spent
Actual time vs estimated (2-3 days).

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 0**: Development Environment ✅
- **Phase 1**: CI/CD Pipeline ✅
- **Phase 2**: Hexagonal Architecture ✅
- **Phase 3**: Database Schema (companies, users tables) ✅
- **Phase 4**: Authentication (JWT, login, register) ✅
- **Phase 5**: RBAC (roles, permissions, company isolation) ✅

### Required By (blocks these phases)
- **Phase 7**: Invoice CRUD API (needs company/user context for invoice ownership)
- **Phase 8**: Audit Log (needs user management events to log)
- **Phase 10**: Frontend Layout (may need user/company info in header)
- **Phase 11**: Frontend Auth Pages (needs profile endpoint)

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ ADMIN can create, view, update, and delete companies
2. ✅ ADMIN can create, view, update, and delete users within their company
3. ✅ ADMIN can assign and change user roles
4. ✅ ADMIN can activate/deactivate users
5. ✅ Self-service profile management works for all roles
6. ✅ Password change works with proper current password verification
7. ✅ Company-scoped user listing with pagination, sorting, and filtering
8. ✅ Multi-tenant isolation prevents cross-company data access
9. ✅ Proper validation and error handling with specific error codes
10. ✅ All unit and integration tests pass
11. ✅ Result file is created at `docs/OMER/step_results/faz_6_result.md`

---

## IMPORTANT NOTES

1. **Existing Schema**: The `companies` and `users` tables already exist from Phase 3. Do NOT recreate them. Build your JPA entities to map to the existing schema.
2. **Existing Auth System**: Phase 4 created AuthService with register/login. UserManagementService is a SEPARATE service for admin operations. Do not break existing auth flows.
3. **Existing RBAC**: Phase 5 created Permission enum, Role enum, @PreAuthorize annotations, CompanyContextFilter, and SecurityExpressionService. Use these in your controllers.
4. **Password Hashing**: Use BCrypt with strength 12 (same as Phase 4) when creating users.
5. **Company Context**: Use the CompanyContextHolder (Phase 5) to get the current user's company ID for multi-tenant queries.
6. **Hexagonal Architecture**: Follow the layer structure from Phase 2. Domain entities should NOT have JPA annotations. Use separate JPA entities in the infrastructure layer.
7. **API Response Wrapper**: Use the ApiResponse wrapper from Phase 2 for consistent response format.
8. **Soft Delete**: Always filter out soft-deleted records in queries (WHERE is_deleted = false).

---

**Phase 6 Completion Target**: Complete company and user management API with full CRUD, profile management, and company-scoped user listing.
