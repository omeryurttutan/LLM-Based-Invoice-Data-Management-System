import time
import asyncio
from typing import List, Optional

from app.config.settings import settings
from app.core.logging import logger
from app.services.llm.base_provider import (
    BaseLLMProvider,
    LLMError,
    LLMTimeoutError,
    LLMRateLimitError,
    LLMServerError,
    LLMAuthenticationError,
    LLMConnectionError,
    LLMResponseError
)
from app.services.llm.providers.gemini_provider import GeminiProvider
from app.services.llm.providers.gemini_2_5_flash_provider import Gemini25FlashProvider
from app.services.llm.providers.openai_provider import OpenAIProvider
from app.services.llm.provider_health import ProviderHealthManager, HealthStatus


class AllProvidersFailedError(LLMError):
    """Raised when all configured providers have failed."""

    def __init__(self, message: str, attempts: List[dict] = None, total_time_ms: float = 0):
        super().__init__(message)
        self.attempts = attempts or []
        self.total_time_ms = total_time_ms


class FallbackChain:
    """
    Manages the sequential fallback chain for LLM providers.
    
    Default order: Gemini 3 Flash → Gemini 2.5 Flash → GPT-5 nano
    
    Rules:
    - Strictly sequential — NOT parallel
    - 2-second delay between provider attempts (configurable)
    - Authentication errors skip provider immediately without retries
    - UNHEALTHY providers are skipped unless they are the last resort
    - Same preprocessed base64 image is passed to all providers
    """

    def __init__(self):
        self.providers = {}
        self.health_manager = ProviderHealthManager()
        self._initialize_providers()

    def _initialize_providers(self):
        """Initialize only configured providers based on available API keys."""
        # Gemini 3 Flash (Primary) — uses GEMINI_API_KEY
        if settings.GEMINI_API_KEY:
            self.providers["GEMINI_3_FLASH"] = GeminiProvider()

        # Gemini 2.5 Flash (Fallback 1) — shares GEMINI_API_KEY
        if settings.GEMINI_API_KEY:
            self.providers["GEMINI_2_5_FLASH"] = Gemini25FlashProvider()

        # GPT-5 nano (Fallback 2) — uses OPENAI_API_KEY
        if settings.OPENAI_API_KEY:
            self.providers["GPT5_NANO"] = OpenAIProvider()

        # Parse chain order from settings, filtering to available providers
        self.chain_order = [
            p.strip().upper()
            for p in settings.LLM_CHAIN_ORDER.split(",")
            if p.strip().upper() in self.providers
        ]

        if not self.chain_order:
            logger.warning("no_providers_configured", message="No LLM providers configured or enabled in chain order.")

    async def generate_with_fallback(self, image_bytes: bytes, prompt: str) -> tuple[str, str, List[dict], Optional[dict]]:
        """
        Try providers sequentially until one succeeds.
        
        Args:
            image_bytes: Raw bytes of the preprocessed invoice image
            prompt: Text prompt for extraction
            
        Returns:
            Tuple of (response_text, provider_name, attempt_logs, usage_stats)
            
        Raises:
            AllProvidersFailedError: When all providers in the chain have failed
        """
        chain_start = time.time()
        attempt_logs = []
        last_error = None

        providers_to_try = self.chain_order
        if not settings.LLM_CHAIN_ENABLED and providers_to_try:
            # If chain is disabled, only try the first (primary) provider
            providers_to_try = [providers_to_try[0]]

        logger.info("fallback_chain_started", order=providers_to_try)

        for index, provider_name in enumerate(providers_to_try):
            provider = self.providers[provider_name]

            # Check health — skip UNHEALTHY unless it's the last resort
            health = self.health_manager.get_health(provider_name)
            is_last_resort = (index == len(providers_to_try) - 1)

            if health.status == HealthStatus.UNHEALTHY and not is_last_resort:
                logger.warning("skipping_unhealthy_provider", provider=provider_name)
                attempt_logs.append({
                    "provider": provider_name,
                    "status": "SKIPPED",
                    "reason": "UNHEALTHY"
                })
                continue

            # Add delay if this is a fallback attempt (not first provider in chain)
            if index > 0 and settings.LLM_FALLBACK_DELAY_SECONDS > 0:
                logger.debug("fallback_delay", seconds=settings.LLM_FALLBACK_DELAY_SECONDS)
                await asyncio.sleep(settings.LLM_FALLBACK_DELAY_SECONDS)

            # Attempt generation
            start_time = time.time()
            try:
                logger.info(
                    "provider_attempt_started",
                    provider=provider_name,
                    attempt_index=index + 1,
                    total_providers=len(providers_to_try),
                )

                # All providers receive the same image bytes and prompt
                response_text, usage = await provider.generate(image_bytes, prompt)

                duration = (time.time() - start_time) * 1000
                self.health_manager.record_success(provider_name)

                attempt_logs.append({
                    "provider": provider_name,
                    "status": "SUCCESS",
                    "duration_ms": round(duration, 2),
                    "usage": usage
                })

                logger.info(
                    "fallback_chain_completed",
                    provider=provider_name,
                    chain_position=index + 1,
                    total_attempts=len(attempt_logs),
                    total_time_ms=round((time.time() - chain_start) * 1000, 2),
                )

                return response_text, provider_name, attempt_logs, usage

            except LLMAuthenticationError as e:
                # Auth error: skip immediately, don't retry this provider
                duration = (time.time() - start_time) * 1000
                logger.error("provider_auth_failed", provider=provider_name, error=str(e))
                self.health_manager.record_failure(provider_name)
                attempt_logs.append({
                    "provider": provider_name,
                    "status": "FAILED",
                    "error": "LLMAuthenticationError",
                    "message": str(e),
                    "duration_ms": round(duration, 2)
                })
                last_error = e
                # Continue to next provider immediately

            except LLMError as e:
                # Transient LLM error (Timeout, RateLimit, ServerError, ResponseError)
                duration = (time.time() - start_time) * 1000
                logger.warning("provider_failed", provider=provider_name, error_type=type(e).__name__, error=str(e))
                self.health_manager.record_failure(provider_name)
                attempt_logs.append({
                    "provider": provider_name,
                    "status": "FAILED",
                    "error": type(e).__name__,
                    "message": str(e),
                    "duration_ms": round(duration, 2)
                })
                last_error = e
                # Continue to next provider

            except Exception as e:
                # Unexpected error
                duration = (time.time() - start_time) * 1000
                logger.error("provider_unexpected_error", provider=provider_name, error=str(e))
                self.health_manager.record_failure(provider_name)
                attempt_logs.append({
                    "provider": provider_name,
                    "status": "FAILED",
                    "error": "UnexpectedError",
                    "message": str(e),
                    "duration_ms": round(duration, 2)
                })
                last_error = e
                # Continue to next provider

        # All providers failed
        total_time = (time.time() - chain_start) * 1000
        logger.error(
            "all_providers_failed",
            total_attempts=len(attempt_logs),
            total_time_ms=round(total_time, 2),
        )
        raise AllProvidersFailedError(
            f"All {len(attempt_logs)} providers failed. Last error: {str(last_error)}",
            attempts=attempt_logs,
            total_time_ms=total_time,
        )
