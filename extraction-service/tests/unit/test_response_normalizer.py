import pytest
from decimal import Decimal
from app.services.response_normalizer import ResponseNormalizer
from app.models.response import InvoiceData

class TestResponseNormalizer:
    
    def test_normalize_date_formats(self):
        """Test date normalization."""
        assert ResponseNormalizer.normalize_date("01.01.2024") == "2024-01-01"
        assert ResponseNormalizer.normalize_date("01/01/2024") == "2024-01-01"
        assert ResponseNormalizer.normalize_date("2024-01-01") == "2024-01-01"
        assert ResponseNormalizer.normalize_date("1 Ocak 2024") == "2024-01-01"
        
        # Invalid date should return original or None depending on implementation
        # Assuming implementation returns None or logs warning for invalid
        assert ResponseNormalizer.normalize_date("Invalid Date") is None

    def test_normalize_amount_formats(self):
        """Test amount normalization (decimal handling)."""
        assert ResponseNormalizer.normalize_amount("1.234,56") == 1234.56  # TR Format
        assert ResponseNormalizer.normalize_amount("1,234.56") == 1234.56  # EN Format
        assert ResponseNormalizer.normalize_amount("1234.56") == 1234.56   # Plain
        assert ResponseNormalizer.normalize_amount("100") == 100.00
        
        # Test with currency symbol
        assert ResponseNormalizer.normalize_amount("100 TL") == 100.00
        assert ResponseNormalizer.normalize_amount("$100.00") == 100.00

    def test_normalize_invoice_data(self, mock_invoice_data):
        """Test full object normalization."""
        # Create data with un-normalized fields
        raw_data = mock_invoice_data.model_copy()
        raw_data.tarih = "01.01.2024"
        raw_data.toplam_tutar = "1.200,00" # String input if model allows, or pre-model dict
        
        # Since InvoiceData has typed fields (float/Decimal), testing strictly the utility functions 
        # that run BEFORE model validation is often what Normalizer does. 
        # Or if Normalizer accepts a dict and returns InvoiceData.
        
        # Assuming Normalizer.normalize(data: dict) -> dict
        raw_dict = {
            "tarih": "01.01.2024",
            "genel_toplam": "1.200,00",
            "para_birimi": "TL"
        }
        
        normalized = ResponseNormalizer.normalize(raw_dict)
        
        assert normalized["tarih"] == "2024-01-01"
        assert normalized["genel_toplam"] == 1200.00
        assert normalized["para_birimi"] == "TRY"
