"""
Unit tests for PromptManager — Phase 15.
"""
import pytest
from app.services.llm.prompt_manager import PromptManager


class TestPromptManager:

    # ─── System Instruction ─────────────────────────────────────────────────

    def test_system_instruction_exists(self):
        instruction = PromptManager.get_system_instruction()
        assert len(instruction) > 50
        assert "Turkish" in instruction

    def test_system_instruction_mentions_image_quality(self):
        instruction = PromptManager.get_system_instruction()
        assert "scan" in instruction.lower() or "photograph" in instruction.lower()
        assert "varying quality" in instruction.lower()

    def test_system_instruction_mentions_turkish_characters(self):
        instruction = PromptManager.get_system_instruction()
        assert "Ç" in instruction
        assert "ğ" in instruction
        assert "ı" in instruction
        assert "İ" in instruction
        assert "ö" in instruction
        assert "ş" in instruction
        assert "ü" in instruction

    # ─── User Prompt ────────────────────────────────────────────────────────

    def test_get_prompt_default(self):
        prompt = PromptManager.get_prompt()
        assert len(prompt) > 100

    def test_prompt_contains_json_schema(self):
        prompt = PromptManager.get_prompt()
        assert "invoice_number" in prompt
        assert "total_amount" in prompt
        assert "items" in prompt
        assert "supplier_name" in prompt

    def test_prompt_contains_turkish_instructions(self):
        prompt = PromptManager.get_prompt()
        assert "Fatura No" in prompt
        assert "KDV" in prompt
        assert "VKN" in prompt
        assert "TCKN" in prompt

    def test_prompt_mentions_number_format(self):
        prompt = PromptManager.get_prompt()
        assert "1.234,56" in prompt or "comma" in prompt.lower()

    def test_prompt_mentions_date_format(self):
        prompt = PromptManager.get_prompt()
        assert "DD.MM.YYYY" in prompt or "YYYY-MM-DD" in prompt

    def test_prompt_mentions_currency_default(self):
        prompt = PromptManager.get_prompt()
        assert "TRY" in prompt

    def test_prompt_no_markdown_rule(self):
        """Prompt must instruct LLM to NOT wrap response in markdown."""
        prompt = PromptManager.get_prompt()
        assert "No markdown" in prompt or "no markdown" in prompt.lower() or "```json" in prompt

    def test_prompt_null_not_empty_string(self):
        """Prompt must instruct to use null, not empty string."""
        prompt = PromptManager.get_prompt()
        assert "null" in prompt.lower()

    # ─── Versioning ─────────────────────────────────────────────────────────

    def test_latest_version(self):
        assert PromptManager.LATEST_VERSION == "v1"

    def test_get_prompt_v1_explicit(self):
        prompt = PromptManager.get_prompt("v1")
        assert len(prompt) > 100

    def test_get_prompt_unknown_version_fallback(self):
        """Unknown version should fallback to latest."""
        prompt = PromptManager.get_prompt("v999")
        default = PromptManager.get_prompt("v1")
        assert prompt == default

    # ─── Prompt Info ────────────────────────────────────────────────────────

    def test_get_prompt_info(self):
        info = PromptManager.get_prompt_info()
        assert "version" in info
        assert "latest_version" in info
        assert "system_instruction_length" in info
        assert "user_prompt_length" in info
        assert info["version"] == "v1"
        assert info["system_instruction_length"] > 0
        assert info["user_prompt_length"] > 0

    def test_get_prompt_info_specific_version(self):
        info = PromptManager.get_prompt_info("v1")
        assert info["version"] == "v1"
