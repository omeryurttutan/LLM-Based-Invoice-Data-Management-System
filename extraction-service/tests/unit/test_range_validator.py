import pytest
from app.services.validation.range_validator import RangeValidator
from app.models.invoice_data import InvoiceData, InvoiceItem

class TestRangeValidator:
    def test_negative_values(self):
        data = InvoiceData(
            total_amount=-100.0,
            invoice_date="2023-10-25", supplier_name="S", subtotal=-100.0, tax_amount=0, invoice_number="1"
        )
        score, issues = RangeValidator.validate(data)
        assert score <= 70.0 # at least -30
        assert len(issues) >= 1

    def test_zero_total_with_items(self):
        data = InvoiceData(
            total_amount=0.0,
            items=[InvoiceItem(description="Free item", quantity=1)],
            invoice_date="2023-10-25", supplier_name="S", subtotal=0, tax_amount=0, invoice_number="1"
        )
        score, issues = RangeValidator.validate(data)
        assert score <= 75.0 # -25 points
        assert len(issues) >= 1
