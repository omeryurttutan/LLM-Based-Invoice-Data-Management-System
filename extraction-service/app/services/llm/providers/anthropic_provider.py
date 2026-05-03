import base64
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import time
from anthropic import AsyncAnthropic, APIConnectionError, RateLimitError, AuthenticationError, APITimeoutError, APIError

from app.config.settings import settings
from app.core.logging import logger
from app.services.llm.base_provider import (
    BaseLLMProvider,
    LLMError,
    LLMTimeoutError,
    LLMRateLimitError,
    LLMServerError,
    LLMAuthenticationError,
    LLMConnectionError
)

class AnthropicProvider(BaseLLMProvider):
    """
    Implementation of BaseLLMProvider for Claude Haiku 4.5.
    """
    
    def __init__(self):
        self.api_key = settings.ANTHROPIC_API_KEY
        if not self.api_key:
            logger.warning("anthropic_api_key_missing", message="ANTHROPIC_API_KEY is not set.")
            
        self.client = AsyncAnthropic(api_key=self.api_key)
        self.model_name = settings.ANTHROPIC_MODEL
        
    @property
    def provider_name(self) -> str:
        return "CLAUDE"

    def is_available(self) -> bool:
        """Check if the Anthropic provider is configured and available."""
        return self.api_key is not None and len(self.api_key) > 0

    @retry(
        stop=stop_after_attempt(settings.ANTHROPIC_MAX_RETRIES + 1),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((APIConnectionError, RateLimitError, APITimeoutError, APIError)),
        reraise=True
    )
    async def generate(self, image_bytes: bytes, prompt: str) -> tuple[str, dict]:
        if not self.api_key:
            raise LLMAuthenticationError("Anthropic API key is missing.")

        start_time = time.time()
        try:
            # Base64 encode
            base64_image = base64.b64encode(image_bytes).decode('utf-8')

            logger.info("llm_request_sent", provider=self.provider_name, model=self.model_name)

            # Anthropic system prompt is a top level param
            # User message contains content blocks
            response = await self.client.messages.create(
                model=self.model_name,
                max_tokens=settings.ANTHROPIC_MAX_TOKENS,
                temperature=settings.ANTHROPIC_TEMPERATURE,
                system="You are a helpful assistant. Output strict JSON.",
                messages=[
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "image",
                                "source": {
                                    "type": "base64",
                                    "media_type": "image/jpeg",
                                    "data": base64_image,
                                },
                            },
                            {
                                "type": "text",
                                "text": prompt
                            }
                        ],
                    }
                ],
                timeout=settings.ANTHROPIC_TIMEOUT
            )
            
            duration = (time.time() - start_time) * 1000
            logger.info("llm_response_received", provider=self.provider_name, duration_ms=duration)
            
            usage = {
                "input_tokens": response.usage.input_tokens,
                "output_tokens": response.usage.output_tokens
            }
            
            return response.content[0].text, usage

        except AuthenticationError as e:
            logger.error("llm_auth_error", error=str(e))
            raise LLMAuthenticationError(f"Anthropic Auth Error: {str(e)}")
            
        except RateLimitError as e:
            logger.warning("llm_rate_limit", error=str(e))
            raise LLMRateLimitError(f"Anthropic rate limit: {str(e)}")
            
        except APITimeoutError as e:
             logger.error("llm_timeout", error=str(e))
             raise LLMTimeoutError(f"Anthropic timeout: {str(e)}")
             
        except APIConnectionError as e:
            logger.error("llm_connection_error", error=str(e))
            raise LLMConnectionError(f"Anthropic connection error: {str(e)}")
            
        except APIError as e:
            logger.error("llm_server_error", error=str(e))
            raise LLMServerError(f"Anthropic server error: {str(e)}")
            
        except Exception as e:
            logger.error("llm_unknown_error", error=str(e))
            raise LLMError(f"Unexpected Anthropic error: {str(e)}")
