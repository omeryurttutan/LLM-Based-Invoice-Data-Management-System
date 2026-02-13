import pytest
from app.services.validation.field_validator import FieldValidator
from app.models.invoice_data import InvoiceData, InvoiceItem
from app.models.validation import ValidationCategory, ValidationSeverity

class TestFieldValidator:
    def _get_complete_invoice(self):
        return InvoiceData(
            invoice_number="INV-001",
            invoice_date="2023-10-25",
            supplier_name="Test Supplier",
            total_amount=100.0,
            supplier_tax_number="1234567890",
            subtotal=80.0,
            tax_amount=20.0,
            currency="TRY",
            items=[InvoiceItem(description="Item 1", quantity=1, unit_price=80, line_total=80, tax_rate=20, tax_amount=20)],
            due_date="2023-11-25",
            supplier_address="Address",
            buyer_name="Buyer",
            buyer_tax_number="987",
            notes="Note"
        )

    def test_all_fields_present(self):
        data = self._get_complete_invoice()
        score, issues = FieldValidator.validate(data)
        assert score == 100.0
        assert len(issues) == 0

    def test_missing_critical_field(self):
        data = self._get_complete_invoice()
        data.invoice_number = None # Missing
        
        score, issues = FieldValidator.validate(data)
        assert score == 75.0 # 100 - 25
        assert len(issues) == 1
        assert issues[0].severity == ValidationSeverity.CRITICAL

    def test_missing_important_field(self):
        data = self._get_complete_invoice()
        data.supplier_tax_number = None # Missing important
        
        score, issues = FieldValidator.validate(data)
        assert score == 90.0 # 100 - 10
        assert len(issues) == 1
        assert issues[0].severity == ValidationSeverity.WARNING

    def test_missing_optional_field(self):
        data = self._get_complete_invoice()
        data.due_date = None # Missing optional
        
        score, issues = FieldValidator.validate(data)
        assert score == 97.0 # 100 - 3
        # Should be only 1 issue for due_date
        assert len(issues) == 1
