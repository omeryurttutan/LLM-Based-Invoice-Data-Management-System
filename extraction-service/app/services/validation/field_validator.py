from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationIssue, ValidationCategory, ValidationSeverity

class FieldValidator:
    """
    Category A: Field Completeness Check
    Checks whether critical, important, and optional fields are populated.
    """
    
    CRITICAL_FIELDS = ["invoice_number", "invoice_date", "supplier_name", "total_amount"]
    IMPORTANT_FIELDS = ["supplier_tax_number", "subtotal", "tax_amount", "currency", "items"]
    OPTIONAL_FIELDS = ["due_date", "supplier_address", "buyer_name", "buyer_tax_number", "notes"]

    @staticmethod
    def validate(data: InvoiceData) -> tuple[float, list[ValidationIssue]]:
        issues = []
        score = 100.0
        
        # Check Critical Fields (-25 points each)
        for field in FieldValidator.CRITICAL_FIELDS:
            value = getattr(data, field, None)
            if not value:
                score -= 25
                issues.append(ValidationIssue(
                    field=field,
                    category=ValidationCategory.FIELD_COMPLETENESS,
                    severity=ValidationSeverity.CRITICAL,
                    message=f"Critical field '{field}' is missing or empty",
                    actual_value="null/empty"
                ))

        # Check Important Fields (-10 points each)
        for field in FieldValidator.IMPORTANT_FIELDS:
            value = getattr(data, field, None)
            if not value and value != 0: # Allow 0 for numeric fields if valid (though usually they shouldn't be 0 here)
                # Special check for items array
                if field == "items" and isinstance(value, list) and len(value) == 0:
                    score -= 10
                    issues.append(ValidationIssue(
                        field=field,
                        category=ValidationCategory.FIELD_COMPLETENESS,
                        severity=ValidationSeverity.WARNING,
                        message=f"Important field '{field}' is empty (no line items extracted)",
                        actual_value="[]"
                    ))
                elif not isinstance(value, list) and not value:
                     score -= 10
                     issues.append(ValidationIssue(
                        field=field,
                        category=ValidationCategory.FIELD_COMPLETENESS,
                        severity=ValidationSeverity.WARNING,
                        message=f"Important field '{field}' is missing used default or null",
                        actual_value=str(value)
                    ))

        # Check Optional Fields (-3 points each)
        for field in FieldValidator.OPTIONAL_FIELDS:
            value = getattr(data, field, None)
            if not value:
                score -= 3
                issues.append(ValidationIssue(
                    field=field,
                    category=ValidationCategory.FIELD_COMPLETENESS,
                    severity=ValidationSeverity.INFO,
                    message=f"Optional field '{field}' is missing",
                    actual_value="null"
                ))

        return max(0.0, score), issues
