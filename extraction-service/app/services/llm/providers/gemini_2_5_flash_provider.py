import google.generativeai as genai
from google.api_core import exceptions
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import time

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


class Gemini25FlashProvider(BaseLLMProvider):
    """
    Implementation of BaseLLMProvider for Google Gemini 2.5 Flash (Fallback 1).
    Uses the same google-generativeai SDK and shared GEMINI_API_KEY,
    but targets the gemini-2.5-flash model with its own configuration.

    This provider exists as the first fallback because:
    - Same SDK/key as primary → minimal additional complexity
    - Different model version may still work when 3 Flash has issues
    - Cheapest option: $0.10/1M input, $0.40/1M output
    """

    def __init__(self):
        super().__init__(
            timeout=settings.GEMINI_2_5_FLASH_TIMEOUT,
            max_retries=settings.GEMINI_2_5_FLASH_MAX_RETRIES,
            retry_delay=1.0,
        )
        # Shared API key with Gemini 3 Flash
        self.api_key = settings.GEMINI_API_KEY
        if not self.api_key:
            logger.warning("gemini_25_api_key_missing", message="GEMINI_API_KEY is not set (shared with Gemini 3 Flash).")

        genai.configure(api_key=self.api_key)

        self.model_name = settings.GEMINI_2_5_FLASH_MODEL

        self.generation_config = {
            "temperature": settings.GEMINI_2_5_FLASH_TEMPERATURE,
            "max_output_tokens": settings.GEMINI_2_5_FLASH_MAX_OUTPUT_TOKENS,
            "response_mime_type": "application/json",
        }

        # Safety settings: Block minimal content (invoice text is business content)
        self.safety_settings = [
            {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"},
        ]

    @property
    def provider_name(self) -> str:
        return LLMProviderNames.GEMINI_2_5_FLASH

    def is_available(self) -> bool:
        """Check if the Gemini 2.5 Flash provider is configured and available."""
        return self.api_key is not None and len(self.api_key) > 0

    @retry(
        stop=stop_after_attempt(settings.GEMINI_2_5_FLASH_MAX_RETRIES + 1),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((
            exceptions.ResourceExhausted,
            exceptions.ServiceUnavailable,
            exceptions.InternalServerError,
        )),
        reraise=True,
    )
    async def generate(self, image_bytes: bytes, prompt: str, mime_type: str = "image/jpeg") -> tuple[str, dict]:
        """
        Send an image + prompt to Gemini 2.5 Flash and return structured JSON text.

        Uses the same multimodal approach as Gemini 3 Flash:
        system_instruction via model parameter, image as inline data.
        """
        if not self.api_key:
            raise LLMAuthenticationError("Gemini API key is missing.")

        start_time = time.time()
        try:
            # Get system instruction from prompt manager (same as primary)
            system_instruction = PromptManager.get_system_instruction()

            model = genai.GenerativeModel(
                model_name=self.model_name,
                generation_config=self.generation_config,
                safety_settings=self.safety_settings,
                system_instruction=system_instruction,
            )

            # Create content parts: image as inline data + text prompt
            image_part = {
                "mime_type": mime_type,
                "data": image_bytes,
            }

            contents = [prompt, image_part]

            logger.info(
                "llm_request_sent",
                provider=self.provider_name,
                model=self.model_name,
                prompt_version=PromptManager.LATEST_VERSION,
            )

            response = await model.generate_content_async(
                contents,
                request_options={"timeout": self.timeout},
            )

            duration = (time.time() - start_time) * 1000
            response_text = response.text

            usage = {
                "input_tokens": getattr(response.usage_metadata, "prompt_token_count", 0),
                "output_tokens": getattr(response.usage_metadata, "candidates_token_count", 0),
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
                raise LLMResponseError("Empty response received from Gemini 2.5 Flash")

            return response_text, usage

        except exceptions.InvalidArgument as e:
            logger.error("llm_auth_error", provider=self.provider_name, error=str(e))
            raise LLMAuthenticationError(f"Invalid argument or API key: {str(e)}")

        except exceptions.DeadlineExceeded as e:
            logger.error("llm_timeout", provider=self.provider_name, error=str(e))
            raise LLMTimeoutError(f"Gemini 2.5 Flash request timed out: {str(e)}")

        except (exceptions.ResourceExhausted, exceptions.ServiceUnavailable, exceptions.InternalServerError):
            # Let these propagate to the @retry decorator for automatic retries.
            # After all retries exhausted, reraise=True will bubble them up.
            raise

        except LLMError:
            # Re-raise our custom errors without wrapping
            raise

        except Exception as e:
            logger.error("llm_unknown_error", provider=self.provider_name, error=str(e))
            if "connection" in str(e).lower():
                raise LLMConnectionError(f"Connection failed: {str(e)}")
            raise LLMError(f"Unexpected error calling Gemini 2.5 Flash: {str(e)}")
