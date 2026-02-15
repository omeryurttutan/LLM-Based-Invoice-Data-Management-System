import pytest
from unittest.mock import MagicMock, patch
from app.services.validation.validator import Validator, ValidationResult
from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationIssue, ValidationSeverity, ValidationCategory

class TestValidator:
    
    @pytest.fixture
    def validator(self):
        return Validator()

    @pytest.fixture
    def sample_data(self):
        return InvoiceData(total_amount=100.0)

    @patch("app.services.validation.validator.FieldValidator")
    @patch("app.services.validation.validator.FormatValidator")
    @patch("app.services.validation.validator.MathValidator")
    @patch("app.services.validation.validator.RangeValidator")
    @patch("app.services.validation.validator.CrossFieldValidator")
    @patch("app.services.validation.validator.ConfidenceCalculator")
    def test_validate_success(self, MockCalc, MockCross, MockRange, MockMath, MockFormat, MockField, validator, sample_data):
        # Setup mocks
        MockField.validate.return_value = (100.0, [])
        MockFormat.validate.return_value = (100.0, [])
        MockMath.validate.return_value = (100.0, [])
        MockRange.validate.return_value = (100.0, [])
        MockCross.validate.return_value = (100.0, [])
        
        MockCalc.calculate.return_value = 100.0
        MockCalc.get_suggested_status.return_value = "AUTO_VERIFIED"
        
        # Execute
        result = validator.validate(sample_data)
        
        # Verify
        assert isinstance(result, ValidationResult)
        assert result.confidence_score == 100.0
        assert result.suggested_status == "AUTO_VERIFIED"
        assert len(result.issues) == 0
        
        # Verify calls
        MockField.validate.assert_called_once()
        MockFormat.validate.assert_called_once()
        # ... others ...

    @patch("app.services.validation.validator.FieldValidator")
    # ... other mocks can be implicit or partial if we rely on exception handling
    # but let's mock all to be safe and avoid side effects
    @patch("app.services.validation.validator.FormatValidator")
    @patch("app.services.validation.validator.MathValidator")
    @patch("app.services.validation.validator.RangeValidator")
    @patch("app.services.validation.validator.CrossFieldValidator")
    @patch("app.services.validation.validator.ConfidenceCalculator")
    def test_validate_with_issues(self, MockCalc, MockCross, MockRange, MockMath, MockFormat, MockField, validator, sample_data):
        # Setup mocks
        issue = ValidationIssue(field="total_amount", message="Error", severity=ValidationSeverity.CRITICAL, category=ValidationCategory.MATH_CONSISTENCY)
        
        MockField.validate.return_value = (100.0, [])
        MockFormat.validate.return_value = (100.0, [])
        MockMath.validate.return_value = (50.0, [issue])
        MockRange.validate.return_value = (100.0, [])
        MockCross.validate.return_value = (100.0, [])
        
        MockCalc.calculate.return_value = 85.0
        MockCalc.get_suggested_status.return_value = "NEEDS_REVIEW"
        
        # Execute
        result = validator.validate(sample_data)
        
        # Verify
        assert result.confidence_score == 85.0
        assert result.suggested_status == "NEEDS_REVIEW"
        assert len(result.issues) == 1
        assert result.issues[0].severity == ValidationSeverity.CRITICAL

    @patch("app.services.validation.validator.FieldValidator")
    @patch("app.services.validation.validator.FormatValidator")
    @patch("app.services.validation.validator.MathValidator")
    @patch("app.services.validation.validator.RangeValidator")
    @patch("app.services.validation.validator.CrossFieldValidator")
    @patch("app.services.validation.validator.ConfidenceCalculator")
    def test_validate_exception_handling(self, MockCalc, MockCross, MockRange, MockMath, MockFormat, MockField, validator, sample_data):
        # Setup mocks to raise exception
        MockField.validate.side_effect = Exception("Crash")
        MockFormat.validate.return_value = (100.0, [])
        MockMath.validate.return_value = (100.0, [])
        MockRange.validate.return_value = (100.0, [])
        MockCross.validate.return_value = (100.0, [])
        
        MockCalc.calculate.return_value = 80.0 # Assuming field score became 0
        MockCalc.get_suggested_status.return_value = "NEEDS_REVIEW"
        
        # Execute
        result = validator.validate(sample_data)
        
        # Verify
        # Should not crash, but result in lower score (mocked above)
        assert result.category_scores["field_completeness"] == 0.0
        assert result.category_scores["format_validation"] == 100.0
