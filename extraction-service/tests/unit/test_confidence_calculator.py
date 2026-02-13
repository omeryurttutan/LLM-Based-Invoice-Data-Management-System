import pytest
from app.services.validation.confidence_calculator import ConfidenceCalculator
from app.config.validation_config import validation_settings


class TestConfidenceCalculator:
    # --- Score Calculation Tests ---
    
    def test_all_scores_100(self):
        """All sub-scores at 100 → overall 100."""
        score = ConfidenceCalculator.calculate(100, 100, 100, 100, 100)
        assert score == 100.0

    def test_all_scores_0(self):
        """All sub-scores at 0 → overall 0."""
        score = ConfidenceCalculator.calculate(0, 0, 0, 0, 0)
        assert score == 0.0

    def test_weighted_average_calculation(self):
        """Verify weighted average with known values."""
        # Weights: A=0.3, B=0.2, C=0.3, D=0.1, E=0.1
        # 100*0.3=30, 50*0.2=10, 100*0.3=30, 100*0.1=10, 0*0.1=0
        # Total = 80.0
        score = ConfidenceCalculator.calculate(
            field_score=100,
            format_score=50,
            math_score=100,
            range_score=100,
            cross_score=0
        )
        assert score == 80.0

    def test_weighted_calculation_mixed(self):
        """Another mixed weighted average case."""
        # 80*0.3=24, 60*0.2=12, 90*0.3=27, 100*0.1=10, 50*0.1=5
        # Total = 78.0
        score = ConfidenceCalculator.calculate(80, 60, 90, 100, 50)
        assert score == 78.0

    def test_score_rounded_to_2_decimal_places(self):
        """Score is rounded to 2 decimal places."""
        score = ConfidenceCalculator.calculate(33.33, 66.67, 77.77, 88.88, 55.55)
        assert score == round(score, 2)

    def test_score_clamped_max(self):
        """Score cannot exceed 100."""
        score = ConfidenceCalculator.calculate(200, 200, 200, 200, 200)
        assert score <= 100.0

    def test_score_clamped_min(self):
        """Score cannot go below 0."""
        score = ConfidenceCalculator.calculate(-50, -50, -50, -50, -50)
        assert score >= 0.0

    # --- Suggested Status Tests ---

    def test_suggested_status_auto_verified(self):
        """Score >= 90 and 0 critical → AUTO_VERIFIED."""
        assert ConfidenceCalculator.get_suggested_status(95.0, 0) == "AUTO_VERIFIED"

    def test_suggested_status_auto_verified_blocked_by_critical(self):
        """Score >= 90 but has critical issues → NEEDS_REVIEW."""
        assert ConfidenceCalculator.get_suggested_status(95.0, 1) == "NEEDS_REVIEW"

    def test_suggested_status_needs_review(self):
        """Score >= 70 → NEEDS_REVIEW."""
        assert ConfidenceCalculator.get_suggested_status(75.0, 0) == "NEEDS_REVIEW"

    def test_suggested_status_low_confidence(self):
        """Score < 70 → LOW_CONFIDENCE."""
        assert ConfidenceCalculator.get_suggested_status(40.0, 0) == "LOW_CONFIDENCE"

    def test_suggested_status_boundary_90(self):
        """Score exactly at 90 with no criticals → AUTO_VERIFIED."""
        assert ConfidenceCalculator.get_suggested_status(90.0, 0) == "AUTO_VERIFIED"

    def test_suggested_status_boundary_70(self):
        """Score exactly at 70 → NEEDS_REVIEW."""
        assert ConfidenceCalculator.get_suggested_status(70.0, 0) == "NEEDS_REVIEW"

    def test_suggested_status_just_below_70(self):
        """Score 69.99 → LOW_CONFIDENCE."""
        assert ConfidenceCalculator.get_suggested_status(69.99, 0) == "LOW_CONFIDENCE"
