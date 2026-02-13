import pytest
import asyncio
from unittest.mock import MagicMock, AsyncMock, patch
from app.services.llm.fallback_chain import FallbackChain, AllProvidersFailedError
from app.services.llm.base_provider import LLMError, LLMTimeoutError
from app.config.settings import settings

# Mock providers to avoid real initialization/API calls
@pytest.fixture
def mock_providers():
    with patch("app.services.llm.fallback_chain.GeminiProvider") as MockGemini, \
         patch("app.services.llm.fallback_chain.OpenAIProvider") as MockOpenAI, \
         patch("app.services.llm.fallback_chain.AnthropicProvider") as MockAnthropic:
        
        gemini = MockGemini.return_value
        gemini.provider_name = "GEMINI"
        
        openai = MockOpenAI.return_value
        openai.provider_name = "GPT"
        
        anthropic = MockAnthropic.return_value
        anthropic.provider_name = "CLAUDE"
        
        yield {"GEMINI": gemini, "GPT": openai, "CLAUDE": anthropic}

@pytest.mark.asyncio
async def test_fallback_success_first_try(mock_providers):
    # Setup: Gemini succeeds
    mock_providers["GEMINI"].generate.return_value = "success_gemini"
    
    chain = FallbackChain()
    # Force chain order for test consistency
    chain.chain_order = ["GEMINI", "GPT", "CLAUDE"]
    chain.providers = mock_providers
    
    response, provider, logs = await chain.generate_with_fallback(b"img", "prompt")
    
    assert response == "success_gemini"
    assert provider == "GEMINI"
    assert len(logs) == 1
    assert logs[0]["status"] == "SUCCESS"
    
    # Verify others NOT called
    mock_providers["GPT"].generate.assert_not_called()

@pytest.mark.asyncio
async def test_fallback_fail_then_succeed(mock_providers):
    # Setup: Gemini fails with Timeout, GPT succeeds
    mock_providers["GEMINI"].generate.side_effect = LLMTimeoutError("Timeout")
    mock_providers["GPT"].generate.return_value = "success_gpt"
    
    chain = FallbackChain()
    chain.chain_order = ["GEMINI", "GPT", "CLAUDE"]
    chain.providers = mock_providers
    
    # speed up delay
    with patch("asyncio.sleep", return_value=None):
        response, provider, logs = await chain.generate_with_fallback(b"img", "prompt")
    
    assert response == "success_gpt"
    assert provider == "GPT"
    assert len(logs) == 2
    assert logs[0]["status"] == "FAILED"
    assert logs[0]["error"] == "LLMTimeoutError"
    assert logs[1]["status"] == "SUCCESS"

@pytest.mark.asyncio
async def test_fallback_all_fail(mock_providers):
    # Setup: All fail
    mock_providers["GEMINI"].generate.side_effect = LLMError("Error1")
    mock_providers["GPT"].generate.side_effect = LLMError("Error2")
    mock_providers["CLAUDE"].generate.side_effect = LLMError("Error3")
    
    chain = FallbackChain()
    chain.chain_order = ["GEMINI", "GPT", "CLAUDE"]
    chain.providers = mock_providers
    
    with pytest.raises(AllProvidersFailedError):
        with patch("asyncio.sleep", return_value=None):
            await chain.generate_with_fallback(b"img", "prompt")
            
    # Verify all called
    assert mock_providers["GEMINI"].generate.call_count == 1
    assert mock_providers["GPT"].generate.call_count == 1
    assert mock_providers["CLAUDE"].generate.call_count == 1
