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
from app.services.llm.providers.openai_provider import OpenAIProvider
from app.services.llm.providers.anthropic_provider import AnthropicProvider
from app.services.llm.provider_health import ProviderHealthManager, HealthStatus

class AllProvidersFailedError(LLMError):
    """Raised when all configured providers have failed."""
    pass

class FallbackChain:
    """
    Manages the sequential fallback chain for LLM providers.
    Order: Gemini -> GPT -> Claude (configurable)
    """
    
    def __init__(self):
        self.providers = {}
        self.health_manager = ProviderHealthManager()
        self._initialize_providers()
        
    def _initialize_providers(self):
        # Initialize only configured providers
        if settings.GEMINI_API_KEY:
            self.providers["GEMINI"] = GeminiProvider()
        if settings.OPENAI_API_KEY:
            self.providers["GPT"] = OpenAIProvider()
        if settings.ANTHROPIC_API_KEY:
            self.providers["CLAUDE"] = AnthropicProvider()
            
        # Parse chain order
        self.chain_order = [
            p.strip().upper() 
            for p in settings.LLM_CHAIN_ORDER.split(",") 
            if p.strip().upper() in self.providers
        ]
        
        if not self.chain_order:
            logger.warning("no_providers_configured", message="No LLM providers configured or enabled in chain order.")

    async def generate_with_fallback(self, image_bytes: bytes, prompt: str) -> tuple[str, str, List[dict], Optional[dict]]:
        """
        Try providers in order.
        Returns: (response_text, provider_name, attempt_logs, usage_stats)
        """
        attempt_logs = []
        last_error = None
        
        providers_to_try = self.chain_order
        if not settings.LLM_CHAIN_ENABLED and providers_to_try:
            # If disabled, only try the first one
            providers_to_try = [providers_to_try[0]]
            
        logger.info("fallback_chain_started", order=providers_to_try)

        for index, provider_name in enumerate(providers_to_try):
            provider = self.providers[provider_name]
            
            # Check health - if unhealthy, skip unless it's the last resort
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

            # Attempt Generation
            start_time = time.time()
            try:
                # Add delay if this is a fallback attempt (not first)
                if index > 0 and settings.LLM_FALLBACK_DELAY_SECONDS > 0:
                    logger.debug("fallback_delay", seconds=settings.LLM_FALLBACK_DELAY_SECONDS)
                    await asyncio.sleep(settings.LLM_FALLBACK_DELAY_SECONDS)

                logger.info("provider_attempt_started", provider=provider_name, attempt=index+1)
                
                # generate is now async and returns (text, usage)
                response_text, usage = await provider.generate(image_bytes, prompt)
                
                duration = (time.time() - start_time) * 1000
                self.health_manager.record_success(provider_name)
                
                attempt_logs.append({
                    "provider": provider_name,
                    "status": "SUCCESS",
                    "duration_ms": duration,
                    "usage": usage
                })
                
                return response_text, provider_name, attempt_logs, usage

            except LLMAuthenticationError as e:
                # Auth error: Record failure but do NOT retry this provider.
                # And usually don't count towards transient health issues? 
                # Actually, auth error is permanent configuration issue. 
                # Should we mark as UNHEALTHY? Yes.
                duration = (time.time() - start_time) * 1000
                logger.error("provider_auth_failed", provider=provider_name, error=str(e))
                self.health_manager.record_failure(provider_name)
                attempt_logs.append({
                    "provider": provider_name,
                    "status": "FAILED",
                    "error": "AuthError",
                    "message": str(e),
                    "duration_ms": duration
                })
                last_error = e
                # Continue to next provider immediately
                
            except LLMError as e:
                # Generic LLM Error (Timeout, RateLimit, ServerError)
                duration = (time.time() - start_time) * 1000
                logger.warning("provider_failed", provider=provider_name, error=str(e))
                self.health_manager.record_failure(provider_name)
                attempt_logs.append({
                    "provider": provider_name,
                    "status": "FAILED",
                    "error": type(e).__name__,
                    "message": str(e),
                    "duration_ms": duration
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
                    "duration_ms": duration
                })
                last_error = e
                # Continue to next

        # If loop finishes, all failed
        logger.error("all_providers_failed", total_attempts=len(providers_to_try))
        raise AllProvidersFailedError(f"All providers failed. Last error: {str(last_error)}")
