import pytest
from app.services.llm.prompt_manager import PromptManager

class TestPromptManager:
    
    def test_get_prompt_default(self):
        prompt = PromptManager.get_prompt()
        assert "Turkey" in prompt or "Turkish" in prompt
        assert "EXPECTED JSON OUTPUT SCHEMA" in prompt
        assert "invoice_number" in prompt

    def test_get_prompt_v1(self):
        prompt = PromptManager.get_prompt("v1")
        assert "Turkish invoice data extraction" in prompt
        assert "DD.MM.YYYY" in prompt
        assert "1.234,56" in prompt
