import pytest
from unittest.mock import MagicMock, patch, AsyncMock
from app.services.llm.fallback_chain import FallbackChain, AllProvidersFailedError
from app.services.llm.base_provider import LLMError, LLMTimeoutError
from app.services.llm.provider_health import HealthStatus, ProviderHealth

class TestFallbackChain:
    
    @pytest.fixture
    def mock_settings(self):
        with patch("app.services.llm.fallback_chain.settings") as mock_settings:
            mock_settings.GEMINI_API_KEY = "key"
            mock_settings.OPENAI_API_KEY = "key"
            mock_settings.ANTHROPIC_API_KEY = "key"
            mock_settings.LLM_CHAIN_ORDER = "GEMINI,GPT,CLAUDE"
            mock_settings.LLM_CHAIN_ENABLED = True
            mock_settings.LLM_FALLBACK_DELAY_SECONDS = 0.0 # No delay for tests by default
            yield mock_settings

    @pytest.fixture
    def fallback_chain(self, mock_settings):
        # Mock provider classes to return mock instances
        with patch("app.services.llm.fallback_chain.GeminiProvider") as MockGemini, \
             patch("app.services.llm.fallback_chain.OpenAIProvider") as MockGPT, \
             patch("app.services.llm.fallback_chain.AnthropicProvider") as MockClaude, \
             patch("app.services.llm.fallback_chain.ProviderHealthManager") as MockHealthManager:
            
            chain = FallbackChain()
            
            # Setup mock instances
            chain.providers["GEMINI"] = MockGemini.return_value
            chain.providers["GPT"] = MockGPT.return_value
            chain.providers["CLAUDE"] = MockClaude.return_value
            chain.health_manager = MockHealthManager.return_value
            
            # Default health check returns HEALTHY
            chain.health_manager.get_health.return_value = ProviderHealth(name="MOCK", status=HealthStatus.HEALTHY)
            
            return chain

    @pytest.mark.async_api
    async def test_primary_succeeds(self, fallback_chain):
        # Setup
        fallback_chain.providers["GEMINI"].generate.return_value = "Result"
        
        # Execute
        result, provider, logs = await fallback_chain.generate_with_fallback(b"image", "prompt")
        
        # Verify
        assert result == "Result"
        assert provider == "GEMINI"
        assert len(logs) == 1
        assert logs[0]["status"] == "SUCCESS"
        
        # Others not called
        fallback_chain.providers["GPT"].generate.assert_not_called()

    @pytest.mark.async_api
    async def test_primary_fails_secondary_succeeds(self, fallback_chain):
        # Setup
        fallback_chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
        fallback_chain.providers["GPT"].generate.return_value = "Result GPT"
        
        # Execute
        result, provider, logs = await fallback_chain.generate_with_fallback(b"image", "prompt")
        
        # Verify
        assert result == "Result GPT"
        assert provider == "GPT"
        assert len(logs) == 2
        assert logs[0]["status"] == "FAILED"
        assert logs[0]["provider"] == "GEMINI"
        assert logs[1]["status"] == "SUCCESS"
        assert logs[1]["provider"] == "GPT"

    @pytest.mark.async_api
    async def test_all_fail(self, fallback_chain):
        # Setup
        fallback_chain.providers["GEMINI"].generate.side_effect = LLMError("Fail 1")
        fallback_chain.providers["GPT"].generate.side_effect = LLMError("Fail 2")
        fallback_chain.providers["CLAUDE"].generate.side_effect = LLMError("Fail 3")
        
        # Execute & Verify
        with pytest.raises(AllProvidersFailedError):
            await fallback_chain.generate_with_fallback(b"image", "prompt")

    @pytest.mark.async_api
    async def test_skip_unhealthy_provider(self, fallback_chain):
        # Setup: Gemini is UNHEALTHY
        def get_health_side_effect(name):
            if name == "GEMINI":
                return ProviderHealth(name="GEMINI", status=HealthStatus.UNHEALTHY)
            return ProviderHealth(name=name, status=HealthStatus.HEALTHY)
            
        fallback_chain.health_manager.get_health.side_effect = get_health_side_effect
        fallback_chain.providers["GPT"].generate.return_value = "Result GPT"
        
        # Execute
        result, provider, logs = await fallback_chain.generate_with_fallback(b"image", "prompt")
        
        # Verify
        assert result == "Result GPT"
        assert provider == "GPT"
        # Gemini should have been skipped (not called)
        fallback_chain.providers["GEMINI"].generate.assert_not_called()
        assert logs[0]["status"] == "SKIPPED"
        assert logs[0]["provider"] == "GEMINI"

    @pytest.mark.async_api
    async def test_last_resort_provider(self, fallback_chain, mock_settings):
        # Setup: All unhealthy, but verify last one is tried anyway as last resort
        mock_settings.LLM_CHAIN_ORDER = "GEMINI"
        fallback_chain.chain_order = ["GEMINI"] # Re-init logic skipped in fixture, manually set for this test
        
        fallback_chain.health_manager.get_health.return_value = ProviderHealth(name="GEMINI", status=HealthStatus.UNHEALTHY)
        fallback_chain.providers["GEMINI"].generate.return_value = "Result"
        
        # Execute
        # Since it is the only one (last resort), it should be tried even if unhealthy
        result, provider, logs = await fallback_chain.generate_with_fallback(b"image", "prompt")
        
        assert result == "Result"
        assert logs[0]["status"] == "SUCCESS"

    @pytest.mark.async_api
    async def test_fallback_delay(self, fallback_chain, mock_settings):
        # Setup
        mock_settings.LLM_FALLBACK_DELAY_SECONDS = 0.5
        fallback_chain.providers["GEMINI"].generate.side_effect = LLMError("Fail")
        fallback_chain.providers["GPT"].generate.return_value = "Result"
        
        # Mock asyncio.sleep to verify call
        with patch("asyncio.sleep", new_callable=AsyncMock) as mock_sleep:
            await fallback_chain.generate_with_fallback(b"image", "prompt")
            
            # Should be called once before 2nd provider
            mock_sleep.assert_called_once_with(0.5)
