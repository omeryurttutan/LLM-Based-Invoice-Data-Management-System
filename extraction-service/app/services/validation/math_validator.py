from typing import List
from app.models.invoice_data import InvoiceData, InvoiceItem
from app.models.validation import ValidationIssue, ValidationCategory, ValidationSeverity
from app.config.validation_config import validation_settings

class MathValidator:
    """
    Category C: Mathematical Consistency
    Checks totals, line items, and tax calculations.
    """

    @staticmethod
    def validate(data: InvoiceData) -> tuple[float, list[ValidationIssue]]:
        issues = []
        score = 100.0
        tolerance = validation_settings.VALIDATION_MATH_TOLERANCE

        # Helper to treat None as 0.0 for math checks
        subtotal = data.subtotal or 0.0
        tax_amount = data.tax_amount or 0.0
        total_amount = data.total_amount or 0.0
        
        # 1. Grand Total Consistency
        # Subtotal + Tax ≈ Total
        calculated_total = subtotal + tax_amount
        if abs(calculated_total - total_amount) > tolerance:
            score -= 40
            issues.append(ValidationIssue(
                field="total_amount",
                category=ValidationCategory.MATH_CONSISTENCY,
                severity=ValidationSeverity.CRITICAL,
                message="Grand total mismatch (Subtotal + Tax != Total)",
                expected_value=f"{calculated_total:.2f}",
                actual_value=f"{total_amount:.2f}"
            ))

        # 2. Items Sum vs Subtotal
        # Sum(Item Line Totals) ≈ Subtotal
        if data.items:
            # Assume line_total in items is pre-tax primarily, but sometimes it might be inconsistent.
            # Best practice: Check if sum of line totals matches subtotal.
            items_sum = sum((item.line_total or 0.0) for item in data.items)
            
            # Tolerance increases with number of items due to rounding
            item_tolerance = tolerance * len(data.items)
            
            if abs(items_sum - subtotal) > item_tolerance:
                 score -= 20
                 issues.append(ValidationIssue(
                    field="subtotal",
                    category=ValidationCategory.MATH_CONSISTENCY,
                    severity=ValidationSeverity.WARNING,
                    message="Subtotal does not match sum of item line totals",
                    expected_value=f"{items_sum:.2f}",
                    actual_value=f"{subtotal:.2f}"
                ))

        # 3. Tax Amount Consistency (Approximate)
        # Sum(Item Taxes) ≈ Total Tax OR Subtotal * Rate ≈ Total Tax
        # This is harder to check strictly because tax rate might vary per item, 
        # but if we have item tax amounts, we can check sum.
        item_tax_sum = sum((item.tax_amount or 0.0) for item in data.items)
        if item_tax_sum > 0:
             if abs(item_tax_sum - tax_amount) > (tolerance * len(data.items)):
                 # Don't penalize too heavily as extraction of per-item tax is often noisy
                 score -= 15
                 issues.append(ValidationIssue(
                    field="tax_amount",
                    category=ValidationCategory.MATH_CONSISTENCY,
                    severity=ValidationSeverity.WARNING,
                    message="Total tax amount does not match sum of item taxes",
                    expected_value=f"{item_tax_sum:.2f}",
                    actual_value=f"{tax_amount:.2f}"
                ))

        # 4. Individual Item Consistency
        # Quantity * Price ≈ Line Total
        item_penalty_cap = 20
        current_item_penalty = 0
        
        for idx, item in enumerate(data.items):
            qty = item.quantity or 0.0
            price = item.unit_price or 0.0
            line = item.line_total or 0.0
            
            if qty > 0 and price > 0 and line > 0:
                calc_line = qty * price
                # Allow tolerance of 0.05 per item
                if abs(calc_line - line) > 0.05:
                    if current_item_penalty < item_penalty_cap:
                        score -= 5
                        current_item_penalty += 5
                        
                    issues.append(ValidationIssue(
                        field=f"items[{idx}].line_total",
                        category=ValidationCategory.MATH_CONSISTENCY,
                        severity=ValidationSeverity.WARNING,
                        message=f"Item line total mismatch (Qty {qty} * Price {price} != {line})",
                         expected_value=f"{calc_line:.2f}",
                         actual_value=f"{line:.2f}"
                    ))
                    
            # 5. Tax Rate Validity
            # Common rates: 1, 10, 20 (and 0, 8, 18 for older/exempt)
            # Just warn if unusual
            if item.tax_rate is not None:
                # 0, 1, 8, 10, 18, 20 are "standard" historical/current rates in TR
                if item.tax_rate not in [0, 1, 8, 10, 18, 20]:
                     score -= 5
                     issues.append(ValidationIssue(
                        field=f"items[{idx}].tax_rate",
                        category=ValidationCategory.MATH_CONSISTENCY,
                        severity=ValidationSeverity.INFO,
                        message=f"Unusual tax rate found: {item.tax_rate}%",
                        actual_value=str(item.tax_rate)
                    ))

        return max(0.0, score), issues
