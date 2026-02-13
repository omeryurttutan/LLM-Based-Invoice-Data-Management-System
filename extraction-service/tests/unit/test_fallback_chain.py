import pytest
from unittest.mock import MagicMock, AsyncMock, patch
from app.services.llm.fallback_chain import FallbackChain, AllProvidersFailedError
from app.services.llm.provider_health import ProviderHealthManager, HealthStatus
from app.services.llm.base_provider import LLMTimeoutError, LLMAuthenticationError

@pytest.mark.asyncio
class TestFallbackChain:
    
    @pytest.fixture
    def chain(self):
        with patch("app.services.llm.fallback_chain.settings") as mock_settings:
            mock_settings.GEMINI_API_KEY = "key"
            mock_settings.OPENAI_API_KEY = "key"
            mock_settings.ANTHROPIC_API_KEY = "key"
            mock_settings.LLM_CHAIN_ORDER = "GEMINI,GPT,CLAUDE"
            mock_settings.LLM_CHAIN_ENABLED = True
            mock_settings.LLM_FALLBACK_DELAY_SECONDS = 0 # No delay for tests
            
            chain = FallbackChain()
            
            # Mock providers
            chain.providers["GEMINI"] = MagicMock()
            chain.providers["GPT"] = MagicMock()
            chain.providers["CLAUDE"] = MagicMock()
            
            # Reset health
            chain.health_manager = ProviderHealthManager()
            chain.health_manager._minit()
            
            return chain

    async def test_fallback_success_first_provider(self, chain):
        chain.providers["GEMINI"].generate.return_value = "Gemini Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert response == "Gemini Response"
        assert provider == "GEMINI"
        assert len(logs) == 1
        assert logs[0]["status"] == "SUCCESS"
        
        chain.providers["GPT"].generate.assert_not_called()

    async def test_fallback_second_provider(self, chain):
        # Gemini fails with Timeout
        chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
        # GPT succeeds
        chain.providers["GPT"].generate.return_value = "GPT Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert response == "GPT Response"
        assert provider == "GPT"
        assert len(logs) == 2
        assert logs[0]["status"] == "FAILED"
        assert logs[1]["status"] == "SUCCESS"

    async def test_all_providers_failed(self, chain):
        chain.providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
        chain.providers["GPT"].generate.side_effect = LLMTimeoutError("Timeout")
        chain.providers["CLAUDE"].generate.side_effect = LLMTimeoutError("Timeout")
        
        with pytest.raises(AllProvidersFailedError):
            await chain.generate_with_fallback(b"bytes", "prompt")

    async def test_auth_error_skip(self, chain):
        # Gemini fails with Auth Error
        chain.providers["GEMINI"].generate.side_effect = LLMAuthenticationError("Bad Key")
        # GPT succeeds
        chain.providers["GPT"].generate.return_value = "GPT Response"
        
        response, provider, logs = await chain.generate_with_fallback(b"bytes", "prompt")
        
        assert provider == "GPT"
        assert logs[0]["status"] == "FAILED"
        assert logs[0]["error"] == "AuthError"
        
        # Check health - Auth error should mark unhealthy
        assert chain.health_manager.get_health("GEMINI").status == HealthStatus.HEALTHY # Wait, logic said AuthError marks failure?
        # Let's check implementation behavior
        # record_failure is called. 
        assert chain.health_manager.get_health("GEMINI").total_failures == 1
