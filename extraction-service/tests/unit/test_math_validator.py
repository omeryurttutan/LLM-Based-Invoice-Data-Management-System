import pytest
from app.services.validation.math_validator import MathValidator
from app.models.invoice_data import InvoiceData, InvoiceItem
from app.models.validation import ValidationSeverity

class TestMathValidator:
    def test_perfect_math(self):
        data = InvoiceData(
            subtotal=100.0,
            tax_amount=20.0,
            total_amount=120.0,
            items=[
                InvoiceItem(description="Item 1", quantity=2, unit_price=50.0, line_total=100.0, tax_amount=20.0, tax_rate=20)
            ],
            invoice_date="2023-10-25", supplier_name="S", invoice_number="1"
        )
        score, issues = MathValidator.validate(data)
        assert score == 100.0
        assert len(issues) == 0

    def test_grand_total_mismatch(self):
        data = InvoiceData(
            subtotal=100.0,
            tax_amount=20.0,
            total_amount=150.0, # Should be 120
            items=[],
            invoice_date="2023-10-25", supplier_name="S", invoice_number="1"
        )
        score, issues = MathValidator.validate(data)
        assert score <= 60.0 # -40 penalty
        assert any(i.severity == ValidationSeverity.CRITICAL for i in issues)

    def test_item_line_mismatch(self):
        data = InvoiceData(
            subtotal=100.0, tax_amount=0, total_amount=100.0,
            items=[
                InvoiceItem(description="Item 1", quantity=2, unit_price=50.0, line_total=90.0) # Should be 100
            ],
            invoice_date="2023-10-25", supplier_name="S", invoice_number="1"
        )
        score, issues = MathValidator.validate(data)
        # Penalties:
        # Items sum (90) != Subtotal (100) -> -20
        # Line total mismatch -> -5
        # Total score -> 75
        assert score == 75.0
        assert len(issues) >= 2
