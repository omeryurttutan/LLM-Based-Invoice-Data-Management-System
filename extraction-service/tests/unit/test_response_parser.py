"""
Unit tests for ResponseParser — Phase 15.
"""
import pytest
import json
from app.services.llm.response_parser import ResponseParser
from app.services.llm.base_provider import LLMResponseError


class TestResponseParserCleanText:

    def test_clean_markdown_json_fence(self):
        raw = '```json\n{"key": "value"}\n```'
        cleaned = ResponseParser._clean_text(raw)
        assert cleaned == '{"key": "value"}'

    def test_clean_markdown_plain_fence(self):
        raw = '```\n{"key": "value"}\n```'
        cleaned = ResponseParser._clean_text(raw)
        assert cleaned == '{"key": "value"}'

    def test_clean_no_fences(self):
        raw = '{"key": "value"}'
        cleaned = ResponseParser._clean_text(raw)
        assert cleaned == raw

    def test_clean_strips_whitespace(self):
        raw = '  \n  {"key": "value"}  \n  '
        cleaned = ResponseParser._clean_text(raw)
        assert cleaned == '{"key": "value"}'


class TestResponseParserParsing:

    def test_parse_valid_json(self):
        raw = '{"invoice_number": "INV-001", "total_amount": 100.50}'
        data = ResponseParser.parse(raw)
        assert data.invoice_number == "INV-001"
        assert data.total_amount == 100.50

    def test_parse_json_with_surrounding_text(self):
        raw = 'Here is the JSON: {"invoice_number": "INV-001", "total_amount": 50.0}'
        data = ResponseParser.parse(raw)
        assert data.invoice_number == "INV-001"
        assert data.total_amount == 50.0

    def test_parse_json_with_markdown_fences(self):
        raw = '```json\n{"invoice_number": "CLEAN-123", "total_amount": 100.00}\n```'
        data = ResponseParser.parse(raw)
        assert data.invoice_number == "CLEAN-123"
        assert data.total_amount == 100.00

    def test_parse_json_with_explanation(self):
        raw = 'Sure, here is the JSON:\n\n```json\n{"invoice_number": "EXP-1", "total_amount": 200.0}\n```\n\nHope this helps!'
        data = ResponseParser.parse(raw)
        assert data.invoice_number == "EXP-1"

    def test_parse_invalid_json(self):
        raw = 'Not valid JSON at all'
        with pytest.raises(LLMResponseError):
            ResponseParser.parse(raw)

    def test_parse_malformed_json(self):
        raw = '{"invoice_number": "BROKEN'
        with pytest.raises(LLMResponseError):
            ResponseParser.parse(raw)

    def test_parse_array_raises_error(self):
        """Arrays are not valid invoice data — should raise LLMResponseError."""
        raw = '[{"key": "value"}]'
        with pytest.raises(LLMResponseError):
            ResponseParser.parse(raw)


class TestResponseParserTurkishNumbers:

    def test_turkish_comma_decimal(self):
        """Turkish format: 1.234,56 → 1234.56"""
        raw = json.dumps({
            "total_amount": "1.234,56",
            "tax_amount": "234,56",
            "subtotal": "1.000,00"
        })
        data = ResponseParser.parse(raw)
        assert data.total_amount == 1234.56
        assert data.tax_amount == 234.56
        assert data.subtotal == 1000.00

    def test_us_format_with_comma(self):
        """US format: 1,234.56 → 1234.56"""
        raw = json.dumps({"total_amount": "1,234.56"})
        data = ResponseParser.parse(raw)
        assert data.total_amount == 1234.56

    def test_currency_symbol_stripping(self):
        """TL and ₺ symbols should be stripped."""
        raw = json.dumps({
            "subtotal": "100 TL",
            "total_amount": "120₺",
            "tax_amount": "20 TRY"
        })
        data = ResponseParser.parse(raw)
        assert data.subtotal == 100.0
        assert data.total_amount == 120.0
        assert data.tax_amount == 20.0

    def test_already_numeric_values(self):
        raw = json.dumps({"total_amount": 500.0, "tax_amount": 100})
        data = ResponseParser.parse(raw)
        assert data.total_amount == 500.0
        assert data.tax_amount == 100.0


class TestResponseParserDates:

    def test_turkish_dot_date(self):
        """DD.MM.YYYY → YYYY-MM-DD"""
        raw = json.dumps({"invoice_date": "15.01.2026"})
        data = ResponseParser.parse(raw)
        assert data.invoice_date == "2026-01-15"

    def test_turkish_slash_date(self):
        """DD/MM/YYYY → YYYY-MM-DD"""
        raw = json.dumps({"invoice_date": "15/01/2026"})
        data = ResponseParser.parse(raw)
        assert data.invoice_date == "2026-01-15"

    def test_iso_date_passthrough(self):
        """YYYY-MM-DD should pass through."""
        raw = json.dumps({"invoice_date": "2026-01-15"})
        data = ResponseParser.parse(raw)
        assert data.invoice_date == "2026-01-15"

    def test_null_date(self):
        raw = json.dumps({"due_date": None})
        data = ResponseParser.parse(raw)
        assert data.due_date is None


class TestResponseParserNullHandling:

    def test_empty_string_to_none(self):
        """Empty strings should be converted to None."""
        raw = json.dumps({
            "invoice_number": "",
            "supplier_name": "",
            "buyer_name": "  ",
            "notes": "",
        })
        data = ResponseParser.parse(raw)
        assert data.invoice_number is None
        assert data.supplier_name is None
        assert data.buyer_name is None
        assert data.notes is None

    def test_whitespace_string_to_none(self):
        raw = json.dumps({"supplier_address": "   "})
        data = ResponseParser.parse(raw)
        assert data.supplier_address is None

    def test_valid_strings_preserved(self):
        raw = json.dumps({"supplier_name": "  Örnek Firma A.Ş.  "})
        data = ResponseParser.parse(raw)
        assert data.supplier_name == "Örnek Firma A.Ş."


class TestResponseParserCurrency:

    def test_try_default(self):
        raw = json.dumps({})
        data = ResponseParser.parse(raw)
        assert data.currency == "TRY"

    def test_tl_to_try(self):
        raw = json.dumps({"currency": "TL"})
        data = ResponseParser.parse(raw)
        assert data.currency == "TRY"

    def test_usd_preserved(self):
        raw = json.dumps({"currency": "usd"})
        data = ResponseParser.parse(raw)
        assert data.currency == "USD"

    def test_unknown_currency_to_try(self):
        raw = json.dumps({"currency": "UNKNOWN"})
        data = ResponseParser.parse(raw)
        assert data.currency == "TRY"


class TestResponseParserItems:

    def test_multiple_items(self):
        raw = json.dumps({
            "items": [
                {
                    "description": "Ürün 1",
                    "quantity": 2,
                    "unit": "adet",
                    "unit_price": 100.0,
                    "tax_rate": 20,
                    "tax_amount": 40.0,
                    "line_total": 240.0
                },
                {
                    "description": "Hizmet 2",
                    "quantity": 1,
                    "unit": "saat",
                    "unit_price": 500.0,
                    "tax_rate": 20,
                    "tax_amount": 100.0,
                    "line_total": 600.0
                }
            ],
            "subtotal": 700.0,
            "tax_amount": 140.0,
            "total_amount": 840.0,
        })
        data = ResponseParser.parse(raw)
        assert len(data.items) == 2
        assert data.items[0].description == "Ürün 1"
        assert data.items[0].quantity == 2.0
        assert data.items[1].description == "Hizmet 2"
        assert data.items[1].unit_price == 500.0

    def test_items_with_turkish_numbers(self):
        raw = json.dumps({
            "items": [
                {
                    "description": "Test",
                    "quantity": "1",
                    "unit_price": "1.500,00",
                    "tax_rate": "20",
                    "tax_amount": "300,00",
                    "line_total": "1.800,00"
                }
            ]
        })
        data = ResponseParser.parse(raw)
        assert data.items[0].unit_price == 1500.0
        assert data.items[0].tax_amount == 300.0
        assert data.items[0].line_total == 1800.0

    def test_empty_items_list(self):
        raw = json.dumps({"items": []})
        data = ResponseParser.parse(raw)
        assert data.items == []

    def test_item_empty_description_to_none(self):
        raw = json.dumps({
            "items": [{"description": "", "unit": "  "}]
        })
        data = ResponseParser.parse(raw)
        assert data.items[0].description is None
        assert data.items[0].unit is None
