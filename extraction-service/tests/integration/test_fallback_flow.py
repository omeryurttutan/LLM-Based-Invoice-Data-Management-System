import pytest
from unittest.mock import MagicMock, AsyncMock, patch
from app.services.extraction.extraction_service import ExtractionService
from app.services.llm.base_provider import LLMTimeoutError

@pytest.mark.asyncio
class TestFallbackFlowIntegration:
    
    @patch("app.services.extraction.extraction_service.PreprocessingPipeline")
    @patch("app.services.extraction.extraction_service.FallbackChain")
    @patch("app.services.extraction.extraction_service.PromptManager")
    async def test_extraction_with_fallback(self, mock_prompt_cls, mock_chain_cls, mock_pipeline_cls):
        # Setup mocks
        mock_pipeline = mock_pipeline_cls.return_value
        mock_pipeline.process.return_value.data = b"bytes"
        
        mock_prompt = mock_prompt_cls.return_value
        mock_prompt.get_prompt.return_value = "test prompt"
        
        mock_chain = mock_chain_cls.return_value
        # Simulate fallback: Logs show failure then success
        mock_chain.generate_with_fallback = AsyncMock(return_value=(
            '{"invoice_number": "GPT-Fallback"}', 
            "GPT", 
            [{"status": "FAILED", "provider": "GEMINI"}, {"status": "SUCCESS", "provider": "GPT"}]
        ))
        
        service = ExtractionService()
        
        # Execute
        result = await service.extract_from_base64("data:image/jpeg;base64,ZmFrZQ==")
        
        # Verify
        assert result.provider == "GPT"
        assert result.data.invoice_number == "GPT-Fallback"
        assert len(result.fallback_attempts) == 2
        assert result.fallback_attempts[0]["status"] == "FAILED"
