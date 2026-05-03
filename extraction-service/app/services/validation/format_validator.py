from datetime import datetime, timedelta
import re
from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationIssue, ValidationCategory, ValidationSeverity

class FormatValidator:
    """
    Category B: Format Validation
    Checks patterns for dates, tax numbers, currency, etc.
    """
    
    ALLOWED_CURRENCIES = ["TRY", "USD", "EUR", "GBP"]

    @staticmethod
    def validate(data: InvoiceData) -> tuple[float, list[ValidationIssue]]:
        issues = []
        score = 100.0
        
        # 1. Date Validation
        if data.invoice_date:
            try:
                inv_date = datetime.strptime(data.invoice_date, "%Y-%m-%d")
                now = datetime.now()
                
                # Future check (> 1 day)
                if inv_date > now + timedelta(days=1):
                    score -= 20
                    issues.append(ValidationIssue(
                        field="invoice_date",
                        category=ValidationCategory.FORMAT_VALIDATION,
                        severity=ValidationSeverity.WARNING,
                        message="Invoice date is in the future",
                        expected_value=f"<= {now.date()}",
                        actual_value=data.invoice_date
                    ))
                
                # Old date check (> 5 years)
                if inv_date < now - timedelta(days=5*365):
                    score -= 20
                    issues.append(ValidationIssue(
                        field="invoice_date",
                        category=ValidationCategory.FORMAT_VALIDATION,
                        severity=ValidationSeverity.WARNING,
                        message="Invoice date is older than 5 years",
                        actual_value=data.invoice_date
                    ))
                    
                # Due date consistency
                if data.due_date:
                    try:
                        due = datetime.strptime(data.due_date, "%Y-%m-%d")
                        if due < inv_date:
                            pass # Handled in cross-field logic
                        if due > inv_date + timedelta(days=365):
                             score -= 20
                             issues.append(ValidationIssue(
                                field="due_date",
                                category=ValidationCategory.FORMAT_VALIDATION,
                                severity=ValidationSeverity.WARNING,
                                message="Due date is more than 1 year after invoice date",
                                actual_value=data.due_date
                            ))
                    except ValueError:
                         pass # handled by pydantic or previous parsing
            except ValueError:
                # Should be handled by Pydantic model, but safe check
                score -= 20
                issues.append(ValidationIssue(
                    field="invoice_date",
                    category=ValidationCategory.FORMAT_VALIDATION,
                    severity=ValidationSeverity.CRITICAL,
                    message="Invalid date format",
                    expected_value="YYYY-MM-DD",
                    actual_value=data.invoice_date
                ))

        # 2. Tax Number Validation (VKN/TCKN)
        if data.supplier_tax_number:
            tax_num = data.supplier_tax_number
            is_vkn = len(tax_num) == 10 and tax_num.isdigit()
            is_tckn = len(tax_num) == 11 and tax_num.isdigit() and tax_num[0] != '0'
            
            if not (is_vkn or is_tckn):
                score -= 15
                issues.append(ValidationIssue(
                    field="supplier_tax_number",
                    category=ValidationCategory.FORMAT_VALIDATION,
                    severity=ValidationSeverity.WARNING,
                    message="Supplier tax number format invalid (must be 10-digit VKN or 11-digit TCKN)",
                    actual_value=tax_num
                ))

        # Buyer Tax Number Validation
        if data.buyer_tax_number:
            btax = data.buyer_tax_number
            is_bvkn = len(btax) == 10 and btax.isdigit()
            is_btckn = len(btax) == 11 and btax.isdigit() and btax[0] != '0'
            
            if not (is_bvkn or is_btckn):
                score -= 15
                issues.append(ValidationIssue(
                    field="buyer_tax_number",
                    category=ValidationCategory.FORMAT_VALIDATION,
                    severity=ValidationSeverity.WARNING,
                    message="Buyer tax number format invalid (must be 10-digit VKN or 11-digit TCKN)",
                    actual_value=btax
                ))

        # 3. Currency Validation
        if data.currency and data.currency not in FormatValidator.ALLOWED_CURRENCIES:
            score -= 10
            issues.append(ValidationIssue(
                field="currency",
                category=ValidationCategory.FORMAT_VALIDATION,
                severity=ValidationSeverity.WARNING,
                message=f"Currency '{data.currency}' not in supported list",
                expected_value=str(FormatValidator.ALLOWED_CURRENCIES),
                actual_value=data.currency
            ))

        # 4. Invoice Number Validation
        if data.invoice_number:
            stripped = data.invoice_number.strip()
            if len(stripped) < 1:
                score -= 15
                issues.append(ValidationIssue(
                    field="invoice_number",
                    category=ValidationCategory.FORMAT_VALIDATION,
                    severity=ValidationSeverity.WARNING,
                    message="Invoice number is empty or whitespace",
                    actual_value=data.invoice_number
                ))
            elif len(stripped) > 50:
                score -= 15
                issues.append(ValidationIssue(
                    field="invoice_number",
                    category=ValidationCategory.FORMAT_VALIDATION,
                    severity=ValidationSeverity.WARNING,
                    message="Invoice number exceeds typical length (>50 characters)",
                    actual_value=f"length={len(stripped)}"
                ))
            # Placeholder check (e.g., all zeros)
            if re.match(r'^0+$', data.invoice_number):
                score -= 15
                issues.append(ValidationIssue(
                    field="invoice_number",
                    category=ValidationCategory.FORMAT_VALIDATION,
                    severity=ValidationSeverity.WARNING,
                    message="Invoice number appears to be a placeholder (all zeros)",
                    actual_value=data.invoice_number
                ))

        return max(0.0, score), issues
