#!/bin/bash
TOKEN=$(curl -s -X POST http://localhost:8082/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"admin@demo.com","password":"Admin123!"}' | grep -o '"accessToken":"[^"]*' | cut -d '"' -f 4)
echo "TOKEN: $TOKEN"
if [ -n "$TOKEN" ]; then
  curl -s -X POST http://localhost:8082/api/v1/invoices \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "invoiceNumber": "INV-TEST-003",
      "invoiceDate": "2024-02-27",
      "supplierName": "Test Supplier",
      "currency": "TRY",
      "items": [
        {
          "description": "Item 1",
          "quantity": 1,
          "unit": "ADET",
          "unitPrice": 100,
          "taxRate": 20
        }
      ]
    }'
fi
