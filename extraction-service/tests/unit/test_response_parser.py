import pytest
import json
from app.services.llm.response_parser import ResponseParser
from app.services.llm.base_provider import LLMResponseError

class TestResponseParser:

    def test_parse_valid_json(self):
        raw_text = '{"invoice_number": "TR123456", "total_amount": 100.50}'
        data = ResponseParser.parse(raw_text)
        assert data.invoice_number == "TR123456"
        assert data.total_amount == 100.50

    def test_parse_markdown_json(self):
        raw_text = '```json\n{"invoice_number": "TR123", "total_amount": 200}\n```'
        data = ResponseParser.parse(raw_text)
        assert data.invoice_number == "TR123"
        assert data.total_amount == 200.0

    def test_normalize_turkish_number_comma_decimal(self):
        # 1.234,56 -> 1234.56
        # Parser expects LLM to return valid JSON numbers usually, but if it returns strings
        # we might need to handle it. Actually, the parser logic handles strings.
        # Let's test mixed types if parser allows it? 
        # The parser._normalize handles strings.
        
        # Scenario: LLM returned number as string with Turkish format
        raw_text = '{"total_amount": "1.234,56", "subtotal": "120,50"}'
        data = ResponseParser.parse(raw_text)
        assert data.total_amount == 1234.56
        assert data.subtotal == 120.50

    def test_normalize_date(self):
        raw_text = '{"invoice_date": "15.01.2023", "due_date": "2023-02-01"}'
        data = ResponseParser.parse(raw_text)
        assert data.invoice_date == "2023-01-15"
        assert data.due_date == "2023-02-01"
        
    def test_normalize_turkish_characters(self):
        raw_text = '{"supplier_name": "GÜNEŞ TİCARET ŞİRKETİ", "items": [{"description": "Çay Bardağı"}]}'
        data = ResponseParser.parse(raw_text)
        assert data.supplier_name == "GÜNEŞ TİCARET ŞİRKETİ"
        assert data.items[0].description == "Çay Bardağı"

    def test_malformed_json(self):
        raw_text = "Not a JSON"
        with pytest.raises(LLMResponseError):
            ResponseParser.parse(raw_text)
            
    def test_currency_normalization(self):
        raw_text = '{"currency": "tl"}'
        data = ResponseParser.parse(raw_text)
        assert data.currency == "TRY"
        
        raw_text = '{"currency": "eur"}'
        data = ResponseParser.parse(raw_text)
        assert data.currency == "EUR"
