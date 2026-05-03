import pytest
import json
from app.services.llm.response_parser import ResponseParser
from app.services.llm.prompt_manager import PromptManager
from app.services.llm.base_provider import LLMResponseError

# Load fixtures
with open("tests/fixtures/mock_responses.json", "r") as f:
    MOCK_RESPONSES = json.load(f)

class TestResponseParser:
    def test_parse_valid_invoice(self):
        raw_text = MOCK_RESPONSES["valid_invoice"]["text"]
        data = ResponseParser.parse(raw_text)
        
        assert data.invoice_number == "FAT-2024-001"
        assert data.total_amount == 1200.00
        assert data.items[0].description == "Server Maintenance"
        assert data.currency == "TRY"

    def test_parse_turkish_format(self):
        raw_text = MOCK_RESPONSES["turkish_format"]["text"]
        data = ResponseParser.parse(raw_text)
        
        assert data.total_amount == 1234.56
        assert data.invoice_date == "2024-01-15"
        assert data.currency == "TRY"

    def test_parse_markdown_messy(self):
        raw_text = MOCK_RESPONSES["markdown_messy"]["text"]
        data = ResponseParser.parse(raw_text)
        
        assert data.invoice_number == "CLEAN-123"
        assert data.total_amount == 100.00

    def test_parse_malformed_json(self):
        raw_text = MOCK_RESPONSES["malformed_json"]["text"]
        with pytest.raises(LLMResponseError):
            ResponseParser.parse(raw_text)

class TestPromptManager:
    def test_get_prompt_default(self):
        prompt = PromptManager.get_prompt()
        assert "Turkish" in prompt
        assert "JSON SCHEMA" in prompt
        
    def test_get_prompt_v1(self):
        prompt = PromptManager.get_prompt("v1")
        assert "v1" in PromptManager.LATEST_VERSION # Verify logic
        assert len(prompt) > 100
