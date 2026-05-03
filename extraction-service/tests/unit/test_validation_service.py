import pytest
from app.services.validation_service import ValidationService
from app.models.response import InvoiceData

class TestValidationService:
    
    def test_validate_success(self, mock_invoice_data):
        """Test validation of a valid invoice."""
        # Assume validate returns a list of errors/warnings, empty if all good
        issues = ValidationService.validate(mock_invoice_data)
        assert len(issues) == 0

    def test_validate_required_fields(self, mock_invoice_data):
        """Test validation of missing required fields."""
        data = mock_invoice_data.model_copy()
        data.fatura_no = None # or empty string if model allows
        
        # Pydantic likely catches None for required fields during instantiation
        # validation service checks logic beyond basic types
        
        # If ValidationService is responsible for checking specific business rules:
        data.fatura_no = "" # Empty string might pass pydantic but fail business logic
        issues = ValidationService.validate(data)
        assert any("fatura_no" in str(issue).lower() for issue in issues)

    def test_validate_math_consistency(self, mock_invoice_data):
        """Test math validation."""
        data = mock_invoice_data.model_copy()
        # Create incorrect math: total != subtotal + tax
        data.toplam_tutar = 2000.00 # Should be 118.00
        
        issues = ValidationService.validate(data)
        assert any("math" in str(issue).lower() or "total" in str(issue).lower() for issue in issues)

    def test_validate_tax_number(self, mock_invoice_data):
        """Test VKN/TCKN validation."""
        data = mock_invoice_data.model_copy()
        data.gonderici_vkn = "123" # Invalid length
        
        issues = ValidationService.validate(data)
        assert any("vkn" in str(issue).lower() for issue in issues)

    def test_validate_date_format(self, mock_invoice_data):
        """Test date logic (e.g. not in future)."""
        data = mock_invoice_data.model_copy()
        data.tarih = "2099-01-01" # Future date
        
        issues = ValidationService.validate(data)
        assert any("date" in str(issue).lower() or "future" in str(issue).lower() for issue in issues)
