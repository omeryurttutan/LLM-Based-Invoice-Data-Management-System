import pytest
from datetime import datetime, timedelta
from app.services.validation.format_validator import FormatValidator
from app.models.invoice_data import InvoiceData

class TestFormatValidator:
    def test_valid_formats(self):
        data = InvoiceData(
            invoice_date="2023-10-25",
            supplier_tax_number="1234567890", # 10 digit
            currency="TRY",
            invoice_number="INV-001",
            supplier_name="Supplier", total_amount=100.0, subtotal=100.0, tax_amount=0.0
        )
        score, issues = FormatValidator.validate(data)
        assert score == 100.0
        assert len(issues) == 0

    def test_future_date(self):
        future = (datetime.now() + timedelta(days=5)).strftime("%Y-%m-%d")
        data = InvoiceData(
            invoice_date=future, 
            supplier_name="S", total_amount=1, subtotal=1, tax_amount=0
        )
        score, issues = FormatValidator.validate(data)
        assert score == 80.0
        assert len(issues) == 1
        assert "future" in issues[0].message

    def test_invalid_tax_number(self):
        data = InvoiceData(
            supplier_tax_number="123", # Too short
             invoice_date="2023-10-25", supplier_name="S", total_amount=1, subtotal=1, tax_amount=0
        )
        score, issues = FormatValidator.validate(data)
        assert score == 85.0
        assert len(issues) == 1

    def test_invalid_currency(self):
        data = InvoiceData(
            currency="JPY", # Not allowed
             invoice_date="2023-10-25", supplier_name="S", total_amount=1, subtotal=1, tax_amount=0
        )
        score, issues = FormatValidator.validate(data)
        assert score == 90.0
        assert len(issues) == 1
