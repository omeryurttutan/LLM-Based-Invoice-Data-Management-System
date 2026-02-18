import logging
import requests
import json
from datetime import datetime
from typing import Optional
from app.config.settings import settings

logger = logging.getLogger(__name__)

class UsageReporter:
    def __init__(self):
        self.backend_url = settings.SPRING_BOOT_URL
        self.api_key = settings.INTERNAL_API_KEY
        self.endpoint = f"{self.backend_url}/api/v1/internal/llm-usage"

        # Cost per 1K tokens (Defaults)
        self.costs = {
            "GEMINI": {
                "INPUT": settings.LLM_COST_GEMINI_INPUT_PER_1K,
                "OUTPUT": settings.LLM_COST_GEMINI_OUTPUT_PER_1K
            },
            "GPT": {
                "INPUT": settings.LLM_COST_GPT_INPUT_PER_1K,
                "OUTPUT": settings.LLM_COST_GPT_OUTPUT_PER_1K
            },
            "CLAUDE": {
                "INPUT": settings.LLM_COST_CLAUDE_INPUT_PER_1K,
                "OUTPUT": settings.LLM_COST_CLAUDE_OUTPUT_PER_1K
            }
        }

    def report_usage(
        self,
        provider: str,
        model: str,
        request_type: str,
        input_tokens: int,
        output_tokens: int,
        success: bool,
        duration_ms: int,
        invoice_id: Optional[str] = None,
        correlation_id: Optional[str] = None
    ):
        """
        Calculates cost and sends usage report to backend.
        """
        if not settings.LLM_USAGE_REPORTING_ENABLED:
            return

        try:
            # Calculate estimated cost
            cost = 0.0
            provider_key = provider.upper()
            if provider_key in self.costs:
                input_cost = (input_tokens / 1000.0) * self.costs[provider_key]["INPUT"]
                output_cost = (output_tokens / 1000.0) * self.costs[provider_key]["OUTPUT"]
                cost = input_cost + output_cost

            payload = {
                "provider": provider,
                "model": model,
                "requestType": request_type,
                "inputTokens": input_tokens,
                "outputTokens": output_tokens,
                "estimatedCostUsd": cost,
                "success": success,
                "durationMs": duration_ms,
                "invoiceId": invoice_id,
                "correlationId": correlation_id
            }

            headers = {
                "Content-Type": "application/json",
                "X-Internal-API-Key": self.api_key
            }
            
            # Send using requests (synchronous, but short timeout)
            # In a high-throughput system, this should be async or fire-and-forget via a queue
            response = requests.post(
                self.endpoint, 
                json=payload, 
                headers=headers, 
                timeout=5
            )
            
            if response.status_code not in (200, 201):
                logger.warning(f"Failed to report usage: {response.status_code} - {response.text}")

        except Exception as e:
            logger.error(f"Error reporting usage: {str(e)}")
