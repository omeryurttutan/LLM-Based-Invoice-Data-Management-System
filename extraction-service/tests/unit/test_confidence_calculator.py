import pytest
from unittest.mock import patch
from app.services.validation.confidence_calculator import ConfidenceCalculator

class TestConfidenceCalculator:

    @pytest.fixture
    def mock_settings(self):
        with patch("app.services.validation.confidence_calculator.validation_settings") as mock:
            # Set default weights
            mock.VALIDATION_WEIGHT_FIELD_COMPLETENESS = 0.30
            mock.VALIDATION_WEIGHT_FORMAT = 0.20
            mock.VALIDATION_WEIGHT_MATH = 0.30
            mock.VALIDATION_WEIGHT_RANGE = 0.10
            mock.VALIDATION_WEIGHT_CROSSFIELD = 0.10
            
            mock.VALIDATION_AUTO_VERIFY_THRESHOLD = 90.0
            mock.VALIDATION_REVIEW_THRESHOLD = 70.0
            yield mock

    def test_calculate_perfect_score(self, mock_settings):
        score = ConfidenceCalculator.calculate(100, 100, 100, 100, 100)
        assert score == 100.0

    def test_calculate_weighted_score(self, mock_settings):
        # 0.3*50 + 0.2*100 + 0.3*100 + 0.1*100 + 0.1*100
        # 15 + 20 + 30 + 10 + 10 = 85
        score = ConfidenceCalculator.calculate(50, 100, 100, 100, 100)
        assert score == 85.0

    def test_get_suggested_status_auto_verified(self, mock_settings):
        status = ConfidenceCalculator.get_suggested_status(95.0, 0)
        assert status == "AUTO_VERIFIED"

    def test_get_suggested_status_needs_review_score(self, mock_settings):
        # Score below auto verify
        status = ConfidenceCalculator.get_suggested_status(80.0, 0)
        assert status == "NEEDS_REVIEW"

    def test_get_suggested_status_needs_review_critical_issues(self, mock_settings):
        # High score but critical issues
        status = ConfidenceCalculator.get_suggested_status(95.0, 1)
        # Should be NEEDS_REVIEW or similar?
        # Logic: 
        # if score >= 90 and critical == 0: AUTO
        # elif score >= 70: REVIEW
        # else: LOW
        
        # 95 >= 90 but critical != 0 -> fails first check
        # 95 >= 70 -> match second check
        assert status == "NEEDS_REVIEW"

    def test_get_suggested_status_low_confidence(self, mock_settings):
        status = ConfidenceCalculator.get_suggested_status(50.0, 0)
        assert status == "LOW_CONFIDENCE"
