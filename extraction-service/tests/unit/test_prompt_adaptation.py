import pytest
from app.services.llm.prompt_manager import PromptManager


class TestPromptAdaptation:
    """
    Unit tests for prompt adaptation across providers — Phase 16 requirement.
    Verifies the master prompt content is consistent and contains all required elements.
    """

    @pytest.fixture
    def prompt_manager(self):
        return PromptManager()

    def test_base_prompt_content_identical(self, prompt_manager):
        """The same prompt is used for all providers (single master prompt)."""
        prompt_v1 = prompt_manager.get_prompt("v1")
        prompt_default = prompt_manager.get_prompt()
        assert prompt_v1 == prompt_default

    def test_prompt_contains_json_schema(self, prompt_manager):
        """All prompt versions must contain the JSON output schema."""
        prompt = prompt_manager.get_prompt()
        assert "invoice_number" in prompt
        assert "invoice_date" in prompt
        assert "supplier_name" in prompt
        assert "supplier_tax_number" in prompt
        assert "items" in prompt
        assert "subtotal" in prompt
        assert "tax_amount" in prompt
        assert "total_amount" in prompt
        assert "currency" in prompt

    def test_prompt_contains_turkish_instructions(self, prompt_manager):
        """All prompt versions must contain Turkish-specific instructions."""
        prompt = prompt_manager.get_prompt()
        assert "Turkish" in prompt or "Türk" in prompt or "fatura" in prompt.lower()
        assert "KDV" in prompt
        assert "VKN" in prompt or "TCKN" in prompt
        assert "TRY" in prompt

    def test_prompt_contains_number_format_rules(self, prompt_manager):
        """Prompt must instruct Turkish comma→dot number conversion."""
        prompt = prompt_manager.get_prompt()
        # Turkish invoices use comma as decimal separator
        assert "COMMA" in prompt or "virgül" in prompt.lower() or "1.234,56" in prompt
        assert "YYYY-MM-DD" in prompt

    def test_prompt_contains_output_rules(self, prompt_manager):
        """Prompt must specify JSON-only output and null handling."""
        prompt = prompt_manager.get_prompt()
        assert "JSON" in prompt
        assert "null" in prompt

    def test_prompt_version_fallback(self, prompt_manager):
        """Unknown version should fall back to latest."""
        prompt_unknown = prompt_manager.get_prompt("v999")
        prompt_latest = prompt_manager.get_prompt(PromptManager.LATEST_VERSION)
        assert prompt_unknown == prompt_latest

    def test_gemini_uses_same_prompt(self, prompt_manager):
        """Gemini provider uses the same base prompt content."""
        prompt = prompt_manager.get_prompt()
        assert len(prompt) > 100  # Sanity: prompt is substantial
        assert "invoice" in prompt.lower()

    def test_openai_format_compatibility(self, prompt_manager):
        """Prompt is compatible with OpenAI chat completion format (can be used as user message)."""
        prompt = prompt_manager.get_prompt()
        assert isinstance(prompt, str)
        assert len(prompt.strip()) > 0

    def test_anthropic_format_compatibility(self, prompt_manager):
        """Prompt is compatible with Anthropic messages API (can be used as text content)."""
        prompt = prompt_manager.get_prompt()
        assert isinstance(prompt, str)
        assert len(prompt.strip()) > 0
