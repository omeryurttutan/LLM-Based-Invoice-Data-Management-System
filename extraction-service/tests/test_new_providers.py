import pytest
from unittest.mock import MagicMock, patch
from app.services.llm.providers.openai_provider import OpenAIProvider
from app.services.llm.providers.anthropic_provider import AnthropicProvider
from app.services.llm.base_provider import LLMError

@pytest.fixture
def mock_openai_client():
    with patch("app.services.llm.providers.openai_provider.OpenAI") as MockClass:
        mock_instance = MockClass.return_value
        yield mock_instance

@pytest.fixture
def mock_anthropic_client():
    with patch("app.services.llm.providers.anthropic_provider.Anthropic") as MockClass:
        mock_instance = MockClass.return_value
        yield mock_instance

@pytest.fixture
def mock_settings():
    with patch("app.services.llm.providers.openai_provider.settings") as mock_settings:
        mock_settings.OPENAI_API_KEY = "test_key"
        yield mock_settings

@pytest.fixture
def mock_anthropic_settings():
    with patch("app.services.llm.providers.anthropic_provider.settings") as mock_settings:
        mock_settings.ANTHROPIC_API_KEY = "test_key"
        yield mock_settings

class TestOpenAIProvider:
    def test_provider_name(self, mock_settings):
        provider = OpenAIProvider()
        assert provider.provider_name == "GPT"

    def test_generate_success(self, mock_openai_client, mock_settings):
        provider = OpenAIProvider()
        
        # Mock response structure
        mock_response = MagicMock()
        mock_choices = MagicMock()
        mock_message = MagicMock()
        mock_message.content = '{"key": "value"}'
        mock_choices.message = mock_message
        mock_response.choices = [mock_choices]
        
        mock_openai_client.chat.completions.create.return_value = mock_response
        
        result = provider.generate(b"fake_image_bytes", "prompt")
        assert result == '{"key": "value"}'
        
        # Verify call arguments
        mock_openai_client.chat.completions.create.assert_called_once()
        call_kwargs = mock_openai_client.chat.completions.create.call_args[1]
        assert call_kwargs["model"] == provider.model_name
        assert "messages" in call_kwargs

class TestAnthropicProvider:
    def test_provider_name(self, mock_anthropic_settings):
        provider = AnthropicProvider()
        assert provider.provider_name == "CLAUDE"

    def test_generate_success(self, mock_anthropic_client, mock_anthropic_settings):
        provider = AnthropicProvider()
        
        # Mock response structure
        mock_response = MagicMock()
        mock_content = MagicMock()
        mock_content.text = '{"key": "value"}'
        mock_response.content = [mock_content]
        
        mock_anthropic_client.messages.create.return_value = mock_response
        
        result = provider.generate(b"fake_image_bytes", "prompt")
        assert result == '{"key": "value"}'
        
        # Verify call arguments
        mock_anthropic_client.messages.create.assert_called_once()
        call_kwargs = mock_anthropic_client.messages.create.call_args[1]
        assert call_kwargs["model"] == provider.model_name
        assert "messages" in call_kwargs
