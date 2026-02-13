from datetime import datetime
from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationIssue, ValidationCategory, ValidationSeverity

class CrossFieldValidator:
    """
    Category E: Cross-Field Logic Validation
    Checks logical relationships between fields.
    """

    @staticmethod
    def validate(data: InvoiceData) -> tuple[float, list[ValidationIssue]]:
        issues = []
        score = 100.0
        
        # 1. Items vs Totals (-10 points)
        # Empty items but positive total suggests incomplete extraction
        if (not data.items or len(data.items) == 0) and (data.total_amount or 0) > 0:
            score -= 10
            issues.append(ValidationIssue(
                field="items",
                category=ValidationCategory.CROSS_FIELD_LOGIC,
                severity=ValidationSeverity.WARNING,
                message="Items list is empty but total amount is positive",
                actual_value="items count: 0"
            ))

        # 2. Date Logic (-20 points)
        if data.invoice_date:
            try:
                inv_date = datetime.strptime(data.invoice_date, "%Y-%m-%d")
                now = datetime.now()
                
                # Due date before invoice date
                if data.due_date:
                    try:
                        due_date = datetime.strptime(data.due_date, "%Y-%m-%d")
                        if due_date < inv_date:
                            score -= 20
                            issues.append(ValidationIssue(
                                field="due_date",
                                category=ValidationCategory.CROSS_FIELD_LOGIC,
                                severity=ValidationSeverity.CRITICAL,
                                message="Due date is before invoice date",
                                actual_value=f"Due: {data.due_date}, Inv: {data.invoice_date}"
                            ))
                    except ValueError:
                        pass
                
                # Invoice date more than 30 days in the future
                from datetime import timedelta
                if inv_date > now + timedelta(days=30):
                    score -= 20
                    issues.append(ValidationIssue(
                        field="invoice_date",
                        category=ValidationCategory.CROSS_FIELD_LOGIC,
                        severity=ValidationSeverity.CRITICAL,
                        message="Invoice date is more than 30 days in the future (likely wrong year)",
                        actual_value=data.invoice_date
                    ))
            except ValueError:
                pass

        # 3. Supplier Completeness (-10 points)
        has_name = bool(data.supplier_name)
        has_tax = bool(data.supplier_tax_number)
        
        if has_name and not has_tax:
            score -= 10
            issues.append(ValidationIssue(
                field="supplier_tax_number",
                category=ValidationCategory.CROSS_FIELD_LOGIC,
                severity=ValidationSeverity.WARNING,
                message="Supplier name is present but tax number is missing",
                actual_value="null"
            ))
        elif has_tax and not has_name:
            score -= 10
            issues.append(ValidationIssue(
                field="supplier_name",
                category=ValidationCategory.CROSS_FIELD_LOGIC,
                severity=ValidationSeverity.WARNING,
                message="Supplier tax number is present but name is missing",
                actual_value="null"
            ))

        # 4. Currency vs Amount Size
        # Suspiciously small amount for TRY (-3 points)
        if data.currency == "TRY" and data.total_amount is not None and 0 < data.total_amount < 0.50:
             score -= 3
             issues.append(ValidationIssue(
                field="total_amount",
                category=ValidationCategory.CROSS_FIELD_LOGIC,
                severity=ValidationSeverity.INFO,
                message="Total amount is unusually small (< 0.50 TRY)",
                actual_value=str(data.total_amount)
            ))
        
        # Unusually large foreign currency amount (-10 points)
        if data.currency in ("USD", "EUR") and data.total_amount is not None and data.total_amount > 10_000_000:
            score -= 10
            issues.append(ValidationIssue(
                field="total_amount",
                category=ValidationCategory.CROSS_FIELD_LOGIC,
                severity=ValidationSeverity.WARNING,
                message=f"Total amount unusually large for {data.currency} (> 10,000,000)",
                actual_value=str(data.total_amount)
            ))
            
        return max(0.0, score), issues
