import pytest
from app.services.validation.cross_field_validator import CrossFieldValidator
from app.models.invoice_data import InvoiceData

class TestCrossFieldValidator:
    def test_due_date_before_invoice_date(self):
        data = InvoiceData(
            invoice_date="2023-11-01",
            due_date="2023-10-01",
            supplier_name="S", total_amount=100.0, subtotal=100.0, tax_amount=0, invoice_number="1"
        )
        score, issues = CrossFieldValidator.validate(data)
        assert score <= 80.0 # -20
        assert len(issues) >= 1

    def test_items_empty_positive_total(self):
        data = InvoiceData(
            total_amount=100.0,
            items=[],
            invoice_date="2023-11-01", supplier_name="S", subtotal=100.0, tax_amount=0, invoice_number="1"
        )
        score, issues = CrossFieldValidator.validate(data)
        assert score <= 90.0 # -10
        assert len(issues) >= 1
