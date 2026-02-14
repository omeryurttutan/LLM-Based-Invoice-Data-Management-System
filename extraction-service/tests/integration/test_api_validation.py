import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.models.invoice_data import InvoiceData

@pytest.mark.asyncio
async def test_validate_invoice_endpoint():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        # 1. Test Config Endpoint
        response = await ac.get("/api/v1/extraction/validation/config")
        assert response.status_code == 200
        config = response.json()
        assert "VALIDATION_AUTO_VERIFY_THRESHOLD" in config
        assert config["VALIDATION_WEIGHT_MATH"] == 0.3

        # 2. Test Validate Endpoint - Valid Data
        valid_data = {
            "invoice_number": "FAT-12345",
            "invoice_date": "2023-10-27",
            "supplier_name": "Test A.S.",
            "supplier_tax_number": "1234567890", # Valid VKN
            "supplier_address": "Test Address",
            "due_date": "2023-11-27",
            "buyer_name": "Buyer A.S.",
            "buyer_tax_number": "11111111111",
            "notes": "Test notes",
            "total_amount": 118.0,
            "subtotal": 100.0,
            "tax_amount": 18.0,
            "currency": "TRY",
            "items": [
                {
                    "description": "Item 1",
                    "quantity": 1,
                    "unit_price": 100.0,
                    "line_total": 100.0, # 1*100
                    "tax_rate": 18,
                    "tax_amount": 18.0
                }
            ]
        }
        
        response = await ac.post("/api/v1/extraction/validate", json=valid_data)
        assert response.status_code == 200
        result = response.json()
        assert result["confidence_score"] > 90 # Should be very high
        assert result["suggested_status"] == "AUTO_VERIFIED"
        assert len(result["issues"]) == 0

        # 3. Test Validate Endpoint - Invalid Data (Math Error + Missing Fields)
        invalid_data = {
            "invoice_number": "FAT-999",
            # Missing invoice_date (Critical)
            "supplier_name": "Bad Supplier",
            "total_amount": 5000.0, # Mismatch with subtotal
            "subtotal": 100.0,
            "tax_amount": 20.0,
            "items": [] # Empty items warning
        }

        response = await ac.post("/api/v1/extraction/validate", json=invalid_data)
        assert response.status_code == 200
        result = response.json()
        
        # Check scores
        assert result["confidence_score"] < 70
        assert result["suggested_status"] == "LOW_CONFIDENCE"
        
        # Check specific issues
        issues = result["issues"]
        # Should have critical error for missing date
        assert any(i["field"] == "invoice_date" and i["severity"] == "CRITICAL" for i in issues)
        # Should have math error for total mismatch
        assert any(i["category"] == "C" and i["severity"] == "CRITICAL" for i in issues)

