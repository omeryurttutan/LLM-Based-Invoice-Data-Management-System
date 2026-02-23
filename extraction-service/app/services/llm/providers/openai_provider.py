import base64
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import time
from openai import AsyncOpenAI, APIError, APIConnectionError, RateLimitError, AuthenticationError, APITimeoutError

from app.config.settings import settings
from app.core.logging import logger
from app.services.llm.base_provider import (
    BaseLLMProvider,
    LLMProviderNames,
    LLMError,
    LLMTimeoutError,
    LLMRateLimitError,
    LLMServerError,
    LLMAuthenticationError,
    LLMConnectionError,
    LLMResponseError,
)
from app.services.llm.prompt_manager import PromptManager


class OpenAIProvider(BaseLLMProvider):
    """
    Implementation of BaseLLMProvider for OpenAI GPT-5 nano (Fallback 2).
    Uses the official openai Python SDK with multimodal vision input.

    This provider exists as the last fallback because:
    - Completely different infrastructure from Google → true redundancy
    - If all Google services are down, OpenAI likely still works
    - Cost-efficient: $0.05/1M input, $0.40/1M output
    """

    def __init__(self):
        super().__init__(
            timeout=settings.OPENAI_TIMEOUT,
            max_retries=settings.OPENAI_MAX_RETRIES,
            retry_delay=1.0,
        )
        self.api_key = settings.OPENAI_API_KEY
        if not self.api_key:
            logger.warning("openai_api_key_missing", message="OPENAI_API_KEY is not set.")

        self.client = AsyncOpenAI(api_key=self.api_key)
        self.model_name = settings.OPENAI_MODEL

    @property
    def provider_name(self) -> str:
        return LLMProviderNames.GPT5_NANO

    def is_available(self) -> bool:
        """Check if the OpenAI provider is configured and available."""
        return self.api_key is not None and len(self.api_key) > 0

    @retry(
        stop=stop_after_attempt(settings.OPENAI_MAX_RETRIES + 1),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((APIConnectionError, RateLimitError, APITimeoutError, APIError)),
        reraise=True
    )
    async def generate(self, image_bytes: bytes, prompt: str, mime_type: str = "image/jpeg") -> tuple[str, dict]:
        """
        Send an image + prompt to GPT-5 nano and return structured JSON text.

        Image is sent as base64 data URL in user message content array.
        System instruction from PromptManager provides extraction context.
        """
        if not self.api_key:
            raise LLMAuthenticationError("OpenAI API key is missing.")

        start_time = time.time()
        try:
            # Prepare image as data URL
            base64_image = base64.b64encode(image_bytes).decode('utf-8')
            data_url = f"data:{mime_type};base64,{base64_image}"

            # Get system instruction from PromptManager (same content as Gemini)
            system_instruction = PromptManager.get_system_instruction()

            logger.info(
                "llm_request_sent",
                provider=self.provider_name,
                model=self.model_name,
                prompt_version=PromptManager.LATEST_VERSION,
            )

            response = await self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {
                        "role": "system",
                        "content": system_instruction
                    },
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt},
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": data_url
                                }
                            }
                        ]
                    }
                ],
                temperature=settings.OPENAI_TEMPERATURE,
                max_tokens=settings.OPENAI_MAX_OUTPUT_TOKENS,
                timeout=settings.OPENAI_TIMEOUT
            )

            duration = (time.time() - start_time) * 1000
            response_text = response.choices[0].message.content

            usage = {
                "input_tokens": response.usage.prompt_tokens,
                "output_tokens": response.usage.completion_tokens
            }

            logger.info(
                "llm_response_received",
                provider=self.provider_name,
                duration_ms=round(duration, 2),
                response_size=len(response_text) if response_text else 0,
                input_tokens=usage["input_tokens"],
                output_tokens=usage["output_tokens"],
            )

            if not response_text:
                raise LLMResponseError("Empty response received from GPT-5 nano")

            return response_text, usage

        except AuthenticationError as e:
            logger.error("llm_auth_error", provider=self.provider_name, error=str(e))
            raise LLMAuthenticationError(f"OpenAI Auth Error: {str(e)}")

        except RateLimitError as e:
            logger.warning("llm_rate_limit", provider=self.provider_name, error=str(e))
            raise LLMRateLimitError(f"OpenAI rate limit: {str(e)}")

        except APITimeoutError as e:
            logger.error("llm_timeout", provider=self.provider_name, error=str(e))
            raise LLMTimeoutError(f"OpenAI timeout: {str(e)}")

        except APIConnectionError as e:
            logger.error("llm_connection_error", provider=self.provider_name, error=str(e))
            raise LLMConnectionError(f"OpenAI connection error: {str(e)}")

        except APIError as e:
            # 5xx or generic
            logger.error("llm_server_error", provider=self.provider_name, error=str(e))
            raise LLMServerError(f"OpenAI server error: {str(e)}")

        except LLMError:
            # Re-raise our custom errors without wrapping
            raise

        except Exception as e:
            logger.error("llm_unknown_error", provider=self.provider_name, error=str(e))
            raise LLMError(f"Unexpected OpenAI error: {str(e)}")
