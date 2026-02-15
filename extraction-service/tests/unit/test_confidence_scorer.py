import pytest
from app.services.confidence_scorer import ConfidenceScorer
from app.models.response import InvoiceData

class TestConfidenceScorer:
    
    def test_calculate_score_perfect(self, mock_invoice_data):
        """Test score for perfect invoice."""
        score = ConfidenceScorer.calculate_score(mock_invoice_data)
        assert score >= 90
        assert score <= 100

    def test_calculate_score_missing_field(self, mock_invoice_data):
        """Test score reduction for missing optional fields."""
        data = mock_invoice_data.model_copy()
        data.gonderici_adres = None # Optional usually
        
        score = ConfidenceScorer.calculate_score(data)
        # Should be slightly lower but still high
        assert score >= 80
        assert score < 100

    def test_calculate_score_math_error(self, mock_invoice_data):
        """Test score reduction for math inconsistencies."""
        data = mock_invoice_data.model_copy()
        data.toplam_tutar = 99999.00 # Wrong math
        
        score = ConfidenceScorer.calculate_score(data)
        assert score < 80 # Significant penalty

    def test_calculate_score_low_confidence(self, mock_invoice_data):
        """Test valid range 0-100."""
        data = mock_invoice_data.model_copy()
        data.fatura_no = ""
        data.tarih = ""
        data.toplam_tutar = 0.0
        
        score = ConfidenceScorer.calculate_score(data)
        assert score >= 0
        assert score < 50
