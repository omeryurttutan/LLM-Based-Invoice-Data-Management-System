from typing import Dict
from app.config.validation_config import validation_settings

class ConfidenceCalculator:
    """
    Calculates the final weighted confidence score.
    """
    
    @staticmethod
    def calculate(
        field_score: float,
        format_score: float,
        math_score: float,
        range_score: float,
        cross_score: float
    ) -> float:
        
        # Weighted average
        final_score = (
            field_score * validation_settings.VALIDATION_WEIGHT_FIELD_COMPLETENESS +
            format_score * validation_settings.VALIDATION_WEIGHT_FORMAT +
            math_score * validation_settings.VALIDATION_WEIGHT_MATH +
            range_score * validation_settings.VALIDATION_WEIGHT_RANGE +
            cross_score * validation_settings.VALIDATION_WEIGHT_CROSSFIELD
        )
        
        # Round to 2 decimal places
        return round(max(0.0, min(100.0, final_score)), 2)

    @staticmethod
    def get_suggested_status(score: float, critical_issues_count: int) -> str:
        if score >= validation_settings.VALIDATION_AUTO_VERIFY_THRESHOLD and critical_issues_count == 0:
            return "AUTO_VERIFIED"
        elif score >= validation_settings.VALIDATION_REVIEW_THRESHOLD:
            return "NEEDS_REVIEW"
        else:
            return "LOW_CONFIDENCE"
