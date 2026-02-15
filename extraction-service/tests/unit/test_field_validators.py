from app.models.invoice_data import InvoiceData, InvoiceItem
from app.services.validation.field_validator import FieldValidator
from app.services.validation.format_validator import FormatValidator
from app.services.validation.math_validator import MathValidator
from app.services.validation.range_validator import RangeValidator
from app.services.validation.cross_field_validator import CrossFieldValidator
from app.models.validation import ValidationIssue, ValidationSeverity, ValidationCategory

class TestValidators:

    def test_field_completeness(self):
        # Missing required fields
        data = InvoiceData(total_amount=100.0) 
        score, issues = FieldValidator.validate(data)
        assert score < 100.0
        assert len(issues) > 0
        
        # All required present (mocking a full invoice data)
        data = InvoiceData(
            invoice_number="123", 
            invoice_date="2023-01-01", 
            supplier_name="Sup", 
            supplier_tax_number="111", 
            buyer_name="Buy", 
            buyer_tax_number="222",
            total_amount=100.0, 
            tax_amount=18.0, 
            subtotal=82.0,
            currency="TRY",
            items=[
                InvoiceItem(description="Item 1", quantity=1.0, unit_price=82.0, line_total=82.0)
            ],
            supplier_address="Address",
            due_date="2023-01-15",
            notes="Notes"
        )
        score, issues = FieldValidator.validate(data)
        assert score == 100.0

    def test_range_validation(self):
        # Create invoice with negative total which RangeValidator checks
        data = InvoiceData(total_amount=-100.0)
        score, issues = RangeValidator.validate(data)
        assert len(issues) > 0
        assert "Negative value" in issues[0].message

    def test_format_validation(self):
        # Invalid date format (assuming raw string that failed parsing or just testing format check)
        # Note: InvoiceData fields are Optional[str] for dates usually, or normalized.
        # If normalized to YYYY-MM-DD, it passes.
        # If invalid format remains, it might be caught here.

        # Test valid tax number
        data = InvoiceData(supplier_tax_number="1234567890")
        score, issues = FormatValidator.validate(data)
        # Should be valid (10 digits)
        assert not any(i.field == "supplier_tax_number" for i in issues)
        
        # Invalid tax number
        data = InvoiceData(supplier_tax_number="123")
        score, issues = FormatValidator.validate(data)
        assert any(i.field == "supplier_tax_number" for i in issues)

    def test_math_validation(self):
        # 100 + 10 = 110. Correct.
        data = InvoiceData(subtotal=100.0, tax_amount=10.0, total_amount=110.0)
        score, issues = MathValidator.validate(data)
        assert score == 100.0
        
        # Incorrect math
        data = InvoiceData(subtotal=100.0, tax_amount=10.0, total_amount=120.0)
        score, issues = MathValidator.validate(data)
        assert score < 100.0
        assert len(issues) > 0

    def test_cross_field_validation(self):
        # Supplier == Buyer
        data = InvoiceData(supplier_tax_number="1234567890", buyer_tax_number="1234567890")
        score, issues = CrossFieldValidator.validate(data)
        assert len(issues) > 0
        
        # Date consistency: due_date < invoice_date
        data = InvoiceData(invoice_date="2023-01-02", due_date="2023-01-01")
        score, issues = CrossFieldValidator.validate(data)
        assert len(issues) > 0
