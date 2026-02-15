import pytest
import json
from app.services.llm.response_parser import ResponseParser
from app.services.llm.base_provider import LLMResponseError

class TestResponseParser:
    
    def test_clean_text_markdown(self):
        raw = "```json\n{\"key\": \"value\"}\n```"
        cleaned = ResponseParser._clean_text(raw)
        assert cleaned == '{"key": "value"}'

    def test_clean_text_basic(self):
        raw = '{"key": "value"}'
        cleaned = ResponseParser._clean_text(raw)
        assert cleaned == raw

    def test_parse_valid_json(self):
        raw = '{"invoice_number": "INV-001", "total_amount": 100.50}'
        data = ResponseParser.parse(raw)
        assert data.invoice_number == "INV-001"
        assert data.total_amount == 100.50

    def test_parse_json_with_text(self):
        raw = 'Here is the JSON: {"invoice_number": "INV-001"}'
        data = ResponseParser.parse(raw)
        assert data.invoice_number == "INV-001"

    def test_parse_invalid_json(self):
        raw = 'Invalid JSON'
        with pytest.raises(LLMResponseError):
            ResponseParser.parse(raw)

    def test_normalize_floats(self):
        raw = json.dumps({
            "total_amount": "1.234,56", # Turkish format
            "tax_amount": "1,234.56", # US format with comma
            "subtotal": "100 TL"
        })
        data = ResponseParser.parse(raw)
        assert data.total_amount == 1234.56
        assert data.tax_amount == 1234.56
        assert data.subtotal == 100.0

    def test_normalize_dates(self):
        raw = json.dumps({
            "invoice_date": "15.01.2023",
            "due_date": "2023-01-20"
        })
        data = ResponseParser.parse(raw)
        assert data.invoice_date == "2023-01-15" # YYYY-MM-DD
        assert data.due_date == "2023-01-20"

    def test_validation_error(self):
        # InvoiceData might require specific fields, checking if pydantic validation triggers LLMResponseError
        # Assuming InvoiceData has some required fields, but if all optional, this test might pass differently.
        # Let's assume we pass something that causes validation error, e.g. wrong type if strict
        # or we can mock InvoiceData to raise error.
        
        # But actually ResponseParser.parse returns InvoiceData.
        # If input is valid JSON but invalid for model, it raises LLMResponseError.
        # Let's try passing a list instead of dict
        raw = '[]' 
        with pytest.raises(LLMResponseError):
            ResponseParser.parse(raw)
