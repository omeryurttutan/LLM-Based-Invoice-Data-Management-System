# API Error Codes

This document lists the standard error codes and responses returned by the Fatura OCR API.

## Error Response Structure

All error responses follow the standard `ApiResponse` structure:

```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2023-10-27T10:00:00"
}
```

## Common HTTP Status Codes

| Status Code | Description           | Meaning                                     |
| :---------- | :-------------------- | :------------------------------------------ |
| 200         | OK                    | Request succeeded.                          |
| 201         | Created               | Resource created successfully.              |
| 400         | Bad Request           | Invalid input or validation failure.        |
| 401         | Unauthorized          | Authentication required or failed.          |
| 403         | Forbidden             | Access denied (insufficient permissions).   |
| 404         | Not Found             | Resource not found.                         |
| 422         | Unprocessable Entity  | Validation failed for semantic reasons.     |
| 429         | Too Many Requests     | Rate limit exceeded.                        |
| 500         | Internal Server Error | Server encountered an unexpected condition. |

## Specific Error Messages

### Authentication & Authorization

- `Invalid credentials`: Login failed.
- `Token expired`: JWT token has expired.
- `Access denied`: User does not have the required role.

### Invoices

- `Invoice not found`: The requested invoice ID does not exist.
- `Invoice execution failed`: Error during extraction or processing.
- `Duplicate invoice`: An invoice with the same number and supplier already exists.

### System

- `Internal system error`: Generic server error.
- `Service unavailable`: External dependency (e.g., OCR provider) is down.
