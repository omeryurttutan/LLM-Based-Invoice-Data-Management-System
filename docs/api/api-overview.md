# Fatura OCR API Overview

Welcome to the Fatura OCR API documentation. This API allows you to manage invoices, extract data using OCR/LLM, and integrate with accounting systems.

## Base URL

`http://localhost:8080/api/v1` (Development)
`https://api.faturaocr.com/api/v1` (Production)

## Authentication

The API uses **JWT (JSON Web Token)** Bearer Authentication.

1. **Login**: `POST /api/v1/auth/login` to obtain an `accessToken`.
2. **Authorize**: Send the token in the `Authorization` header:
   `Authorization: Bearer <your_access_token>`

## Key Resources

- **Auth**: Login, registration, token refresh.
- **Invoices**: Upload, list, update, and verify invoices.
- **Categories**: Manage invoice categories.
- **Suppliers**: Manage supplier templates and rules.
- **Dashboard**: Get analytics and statistics.
- **Export**: Export invoices to Excel, CSV, or Accounting XML formats.

## OpenAPI / Swagger UI

Interactive documentation is available at:
`http://localhost:8082/api/docs`

This UI allows you to explore endpoints and test requests directly.

## Rate Limiting

Rate limiting is applied to protect the API.

- **Public**: 20 requests/minute
- **Authenticated**: 100 requests/minute
- **Admin**: Higher limits available.

If you exceed the limit, you will receive a `429 Too Many Requests` response.
