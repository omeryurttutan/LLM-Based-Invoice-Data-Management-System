import pytest
from unittest.mock import MagicMock, AsyncMock, patch
from app.services.llm.fallback_chain import FallbackChain, AllProvidersFailedError
from app.services.llm.provider_health import ProviderHealthManager, HealthStatus
from app.services.llm.base_provider import LLMTimeoutError, LLMAuthenticationError, LLMResponseError, LLMError

@pytest.mark.asyncio
class TestFallbackChain:
    
    @pytest.fixture
    def chain(self):
        with patch("app.services.llm.fallback_chain.settings") as mock_settings, \
             patch("app.services.llm.fallback_chain.GeminiProvider"), \
             patch("app.services.llm.fallback_chain.OpenAIProvider"), \
             patch("app.services.llm.fallback_chain.AnthropicProvider"):
            mock_settings.GEMINI_API_KEY = "key"
            mock_settings.OPENAI_API_KEY = "key"
            mock_settings.ANTHROPIC_API_KEY = "key"
            mock_settings.LLM_CHAIN_ORDER = "GEMINI,GPT,CLAUDE"
            mock_settings.LLM_CHAIN_ENABLED = True
            mock_settings.LLM_FALLBACK_DELAY_SECONDS = 0
            
            # Reset singleton
            ProviderHealthManager._instance = None
            
            chain = FallbackChain()
            
            # Replace with mock providers
            chain.providers["GEMINI"] = MagicMock()
            chain.providers["GPT"] = MagicMock()
            chain.providers["CLAUDE"] = MagicMock()
            
            # Reset health
            chain.health_manager = ProviderHealthManager()
            chain.health_manager._minit()
            
            return chain

    async def test_fallback_success_first_provider(self, chain):
        """Happy path: first provider (Gemini) succeeds → no fallback triggered."""
        chain.providers["GEMINI"].generate.return_value = "Gemini Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert response == "Gemini Response"
        assert provider == "GEMINI"
        assert len(logs) == 1
        assert logs[0]["status"] == "SUCCESS"
        
        chain.providers["GPT"].generate.assert_not_called()

    async def test_fallback_second_provider(self, chain):
        """First fallback: Gemini fails with timeout → GPT succeeds."""
        chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
        chain.providers["GPT"].generate.return_value = "GPT Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert response == "GPT Response"
        assert provider == "GPT"
        assert len(logs) == 2
        assert logs[0]["status"] == "FAILED"
        assert logs[1]["status"] == "SUCCESS"

    async def test_second_fallback_to_claude(self, chain):
        """Second fallback: Gemini + GPT both fail → Claude succeeds."""
        chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
        chain.providers["GPT"].generate.side_effect = LLMTimeoutError("Timeout")
        chain.providers["CLAUDE"].generate.return_value = "Claude Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert response == "Claude Response"
        assert provider == "CLAUDE"
        assert len(logs) == 3
        assert logs[0]["status"] == "FAILED"
        assert logs[1]["status"] == "FAILED"
        assert logs[2]["status"] == "SUCCESS"

    async def test_all_providers_failed(self, chain):
        """All three providers fail → AllProvidersFailedError raised."""
        chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
        chain.providers["GPT"].generate.side_effect = LLMTimeoutError("Timeout")
        chain.providers["CLAUDE"].generate.side_effect = LLMTimeoutError("Timeout")
        
        with pytest.raises(AllProvidersFailedError):
            await chain.generate_with_fallback(b"bytes", "prompt")

    async def test_auth_error_skip(self, chain):
        """Auth error skip: Gemini has auth error → immediately skips to GPT."""
        chain.providers["GEMINI"].generate.side_effect = LLMAuthenticationError("Bad Key")
        chain.providers["GPT"].generate.return_value = "GPT Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert provider == "GPT"
        assert logs[0]["status"] == "FAILED"
        assert logs[0]["error"] == "AuthError"
        assert chain.health_manager.get_health("GEMINI").total_failures == 1

    async def test_parse_error_fallback(self, chain):
        """Parse error fallback: LLMResponseError triggers fallback to next provider."""
        chain.providers["GEMINI"].generate.side_effect = LLMResponseError("Invalid JSON")
        chain.providers["GPT"].generate.return_value = '{"invoice_number": "GPT456"}'
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert provider == "GPT"
        assert logs[0]["status"] == "FAILED"
        assert "LLMResponseError" in logs[0]["error"]

    async def test_chain_disabled(self):
        """When LLM_CHAIN_ENABLED=false → only first provider is tried."""
        with patch("app.services.llm.fallback_chain.settings") as mock_settings, \
             patch("app.services.llm.fallback_chain.GeminiProvider"), \
             patch("app.services.llm.fallback_chain.OpenAIProvider"), \
             patch("app.services.llm.fallback_chain.AnthropicProvider"):
            mock_settings.GEMINI_API_KEY = "key"
            mock_settings.OPENAI_API_KEY = "key"
            mock_settings.ANTHROPIC_API_KEY = "key"
            mock_settings.LLM_CHAIN_ORDER = "GEMINI,GPT,CLAUDE"
            mock_settings.LLM_CHAIN_ENABLED = False
            mock_settings.LLM_FALLBACK_DELAY_SECONDS = 0
            
            ProviderHealthManager._instance = None
            chain = FallbackChain()
            chain.providers["GEMINI"] = MagicMock()
            chain.providers["GPT"] = MagicMock()
            chain.providers["CLAUDE"] = MagicMock()
            chain.health_manager = ProviderHealthManager()
            chain.health_manager._minit()
            
            chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
            
            with pytest.raises(AllProvidersFailedError):
                await chain.generate_with_fallback(b"bytes", "prompt")
            
            chain.providers["GPT"].generate.assert_not_called()
            chain.providers["CLAUDE"].generate.assert_not_called()

    async def test_chain_order_configuration(self):
        """Custom chain order 'GPT,CLAUDE,GEMINI' → GPT is tried first."""
        with patch("app.services.llm.fallback_chain.settings") as mock_settings, \
             patch("app.services.llm.fallback_chain.GeminiProvider"), \
             patch("app.services.llm.fallback_chain.OpenAIProvider"), \
             patch("app.services.llm.fallback_chain.AnthropicProvider"):
            mock_settings.GEMINI_API_KEY = "key"
            mock_settings.OPENAI_API_KEY = "key"
            mock_settings.ANTHROPIC_API_KEY = "key"
            mock_settings.LLM_CHAIN_ORDER = "GPT,CLAUDE,GEMINI"
            mock_settings.LLM_CHAIN_ENABLED = True
            mock_settings.LLM_FALLBACK_DELAY_SECONDS = 0
            
            ProviderHealthManager._instance = None
            chain = FallbackChain()
            chain.providers["GEMINI"] = MagicMock()
            chain.providers["GPT"] = MagicMock()
            chain.providers["CLAUDE"] = MagicMock()
            chain.health_manager = ProviderHealthManager()
            chain.health_manager._minit()
            
            chain.providers["GPT"].generate.return_value = "GPT First"
            
            response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
            
            assert provider == "GPT"
            assert response == "GPT First"
            chain.providers["GEMINI"].generate.assert_not_called()

    @patch("app.services.llm.fallback_chain.asyncio.sleep", new_callable=AsyncMock)
    async def test_inter_provider_delay(self, mock_sleep):
        """Verify delay between provider attempts."""
        with patch("app.services.llm.fallback_chain.settings") as mock_settings, \
             patch("app.services.llm.fallback_chain.GeminiProvider"), \
             patch("app.services.llm.fallback_chain.OpenAIProvider"), \
             patch("app.services.llm.fallback_chain.AnthropicProvider"):
            mock_settings.GEMINI_API_KEY = "key"
            mock_settings.OPENAI_API_KEY = "key"
            mock_settings.ANTHROPIC_API_KEY = "key"
            mock_settings.LLM_CHAIN_ORDER = "GEMINI,GPT,CLAUDE"
            mock_settings.LLM_CHAIN_ENABLED = True
            mock_settings.LLM_FALLBACK_DELAY_SECONDS = 2
            
            ProviderHealthManager._instance = None
            chain = FallbackChain()
            chain.providers["GEMINI"] = MagicMock()
            chain.providers["GPT"] = MagicMock()
            chain.providers["CLAUDE"] = MagicMock()
            chain.health_manager = ProviderHealthManager()
            chain.health_manager._minit()
            
            chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
            chain.providers["GPT"].generate.return_value = "GPT Response"
            
            response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
            
            assert provider == "GPT"
            mock_sleep.assert_called_once_with(2)

    async def test_fallback_attempt_details(self, chain):
        """Verify attempt log structure for each provider."""
        chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Gemini Timeout")
        chain.providers["GPT"].generate.return_value = "GPT Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert len(logs) == 2
        
        # First attempt (GEMINI - failed)
        assert logs[0]["provider"] == "GEMINI"
        assert logs[0]["status"] == "FAILED"
        assert "error" in logs[0]
        assert "message" in logs[0]
        assert "duration_ms" in logs[0]
        
        # Second attempt (GPT - success)
        assert logs[1]["provider"] == "GPT"
        assert logs[1]["status"] == "SUCCESS"
        assert "duration_ms" in logs[1]
