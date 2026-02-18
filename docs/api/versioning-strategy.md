# API Versioning Strategy

## URL Path Versioning

Fatura OCR uses URL path versioning to manage API changes.

### Current Version: v1

All API endpoints are prefixed with `/api/v1/`.

**Example:**
`GET /api/v1/invoices`

### Breaking Changes

Breaking changes will introduce a new major version (e.g., `/api/v2/`).

Breaking changes include:

- Renaming or removing URL paths.
- Renaming or removing JSON fields in responses.
- Changing the data type of a JSON field.
- Making a previously optional parameter mandatory.

### Non-Breaking Changes

Non-breaking changes are deployed to the existing version (`v1`).

Non-breaking changes include:

- Adding new endpoints.
- Adding new optional parameters.
- Adding new fields to JSON responses.
- Bug fixes and performance improvements.

## Deprecation Policy

When a new major version is released:

1. The old version is marked as **Deprecated**.
2. A deprecation warning header (`X-API-Deprecation-Date`) may be included in responses.
3. The old version will be supported for a transition period (e.g., 6 months) before removal.
