from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationIssue, ValidationCategory, ValidationSeverity
from app.config.validation_config import validation_settings

class RangeValidator:
    """
    Category D: Value Range Validation
    Checks for negative values, zeros, and extreme limits.
    """

    @staticmethod
    def validate(data: InvoiceData) -> tuple[float, list[ValidationIssue]]:
        issues = []
        score = 100.0
        
        max_amount = validation_settings.VALIDATION_MAX_INVOICE_AMOUNT

        # 1. Negative Amounts (-30 points)
        amounts = {
            "total_amount": data.total_amount,
            "subtotal": data.subtotal,
            "tax_amount": data.tax_amount
        }
        
        for field, value in amounts.items():
            if value is not None and value < 0:
                score -= 30
                issues.append(ValidationIssue(
                    field=field,
                    category=ValidationCategory.VALUE_RANGE,
                    severity=ValidationSeverity.WARNING,
                    message=f"Negative value found in {field}",
                    actual_value=str(value)
                ))

        # 2. Zero Total with Items (-25 points)
        if data.total_amount == 0 and data.items and len(data.items) > 0:
            score -= 25
            issues.append(ValidationIssue(
                field="total_amount",
                category=ValidationCategory.VALUE_RANGE,
                severity=ValidationSeverity.WARNING,
                message="Total amount is 0 but items exist",
                actual_value="0.00"
            ))

        # 3. Extreme Amounts (-10 points)
        if data.total_amount is not None and data.total_amount > max_amount:
            score -= 10
            issues.append(ValidationIssue(
                field="total_amount",
                category=ValidationCategory.VALUE_RANGE,
                severity=ValidationSeverity.WARNING,
                message=f"Amount exceeds reasonable maximum ({max_amount})",
                actual_value=str(data.total_amount)
            ))

        # 4. Item Quantity/Price Checks (-10 points per item, capped)
        item_penalty_cap = 20
        current_penalty = 0
        
        for idx, item in enumerate(data.items):
            if (item.quantity is not None and item.quantity <= 0) or \
               (item.unit_price is not None and item.unit_price <= 0):
                
                if current_penalty < item_penalty_cap:
                    score -= 10
                    current_penalty += 10
                    
                issues.append(ValidationIssue(
                    field=f"items[{idx}]",
                    category=ValidationCategory.VALUE_RANGE,
                    severity=ValidationSeverity.WARNING,
                    message="Item quantity or unit price is <= 0",
                    actual_value=f"Qty: {item.quantity}, Price: {item.unit_price}"
                ))

        return max(0.0, score), issues
