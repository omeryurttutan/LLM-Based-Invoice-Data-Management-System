from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationResult, ValidationSeverity, ValidationCategory
from app.services.validation.field_validator import FieldValidator
from app.services.validation.format_validator import FormatValidator
from app.services.validation.math_validator import MathValidator
from app.services.validation.range_validator import RangeValidator
from app.services.validation.cross_field_validator import CrossFieldValidator
from app.services.validation.confidence_calculator import ConfidenceCalculator
from app.core.logging import logger

class Validator:
    """
    Main validator orchestrator.
    Each validator runs independently — if one fails, others still execute.
    """
    
    def validate(self, data: InvoiceData) -> ValidationResult:
        all_issues = []
        scores = {}
        
        invoice_id = getattr(data, 'invoice_number', 'unknown')
        logger.info("validation_started", invoice_number=invoice_id)
        
        # Run all validators independently (try-catch per category)
        # 1. Field Completeness
        try:
            s1, i1 = FieldValidator.validate(data)
            scores["field_completeness"] = s1
            all_issues.extend(i1)
            logger.debug("category_score", category="field_completeness", score=s1)
        except Exception as e:
            logger.error("validator_failed", category="field_completeness", error=str(e))
            scores["field_completeness"] = 0.0
        
        # 2. Format
        try:
            s2, i2 = FormatValidator.validate(data)
            scores["format_validation"] = s2
            all_issues.extend(i2)
            logger.debug("category_score", category="format_validation", score=s2)
        except Exception as e:
            logger.error("validator_failed", category="format_validation", error=str(e))
            scores["format_validation"] = 0.0
        
        # 3. Math
        try:
            s3, i3 = MathValidator.validate(data)
            scores["math_consistency"] = s3
            all_issues.extend(i3)
            logger.debug("category_score", category="math_consistency", score=s3)
        except Exception as e:
            logger.error("validator_failed", category="math_consistency", error=str(e))
            scores["math_consistency"] = 0.0
        
        # 4. Range
        try:
            s4, i4 = RangeValidator.validate(data)
            scores["value_range"] = s4
            all_issues.extend(i4)
            logger.debug("category_score", category="value_range", score=s4)
        except Exception as e:
            logger.error("validator_failed", category="value_range", error=str(e))
            scores["value_range"] = 0.0
        
        # 5. Cross Field
        try:
            s5, i5 = CrossFieldValidator.validate(data)
            scores["cross_field_logic"] = s5
            all_issues.extend(i5)
            logger.debug("category_score", category="cross_field_logic", score=s5)
        except Exception as e:
            logger.error("validator_failed", category="cross_field_logic", error=str(e))
            scores["cross_field_logic"] = 0.0
        
        # Calculate final score
        final_score = ConfidenceCalculator.calculate(
            scores.get("field_completeness", 0.0),
            scores.get("format_validation", 0.0),
            scores.get("math_consistency", 0.0),
            scores.get("value_range", 0.0),
            scores.get("cross_field_logic", 0.0)
        )
        
        # Determine status
        critical_count = sum(1 for i in all_issues if i.severity == ValidationSeverity.CRITICAL)
        status = ConfidenceCalculator.get_suggested_status(final_score, critical_count)
        
        # Log critical issues as warnings
        for issue in all_issues:
            if issue.severity == ValidationSeverity.CRITICAL:
                logger.warning("critical_validation_issue", field=issue.field, message=issue.message)
        
        # Create summary
        issue_counts = {
            "CRITICAL": critical_count,
            "WARNING": sum(1 for i in all_issues if i.severity == ValidationSeverity.WARNING),
            "INFO": sum(1 for i in all_issues if i.severity == ValidationSeverity.INFO)
        }
        summary = f"Score: {final_score}. Found {len(all_issues)} issues: {issue_counts['CRITICAL']} critical, {issue_counts['WARNING']} warnings, {issue_counts['INFO']} info."

        # Log level based on score
        if final_score == 0:
            logger.error("validation_complete_zero_score", invoice_number=invoice_id)
        elif final_score < 70:
            logger.warning("validation_below_threshold", confidence_score=final_score, suggested_status=status, issue_count=len(all_issues))
        else:
            logger.info("validation_completed", confidence_score=final_score, suggested_status=status, issue_count=len(all_issues))
        
        logger.debug("validation_issues", issues=[i.model_dump() for i in all_issues])

        return ValidationResult(
            confidence_score=final_score,
            suggested_status=status,
            category_scores=scores,
            issues=all_issues,
            summary=summary
        )
