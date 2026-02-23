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


class GeminiProvider(BaseLLMProvider):
    """
    Implementation of BaseLLMProvider for Google Gemini (Flash).
    Uses the official google-generativeai SDK.
    
    Primary model: gemini-2.5-flash (configurable via GEMINI_MODEL env var).
    """

    def __init__(self):
        super().__init__(
            timeout=settings.GEMINI_TIMEOUT,
            max_retries=settings.GEMINI_MAX_RETRIES,
            retry_delay=1.0,
        )
        self.api_key = settings.GEMINI_API_KEY
        if not self.api_key:
            logger.warning("gemini_api_key_missing", message="GEMINI_API_KEY is not set.")

        genai.configure(api_key=self.api_key)

        # Determine model name from settings
        self.model_name = settings.GEMINI_MODEL

        self.generation_config = {
            "temperature": settings.GEMINI_TEMPERATURE,
            "max_output_tokens": settings.GEMINI_MAX_OUTPUT_TOKENS,
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
        return LLMProviderNames.GEMINI_3_FLASH

    def is_available(self) -> bool:
        """Check if the Gemini provider is configured and available."""
        return self.api_key is not None and len(self.api_key) > 0

    @retry(
        stop=stop_after_attempt(settings.GEMINI_MAX_RETRIES + 1),
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
        Send an image + prompt to Gemini and return structured JSON text.
        
        The system instruction is set via the model's system_instruction parameter,
        and the user message contains the image + extraction prompt.
        """
        if not self.api_key:
            raise LLMAuthenticationError("Gemini API key is missing.")

        start_time = time.time()
        try:
            # Get system instruction from prompt manager
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
                raise LLMResponseError("Empty response received from Gemini")

            return response_text, usage

        except exceptions.InvalidArgument as e:
            logger.error("llm_auth_error", provider=self.provider_name, error=str(e))
            raise LLMAuthenticationError(f"Invalid argument or API key: {str(e)}")

        except exceptions.ResourceExhausted as e:
            logger.warning("llm_rate_limit", provider=self.provider_name, error=str(e))
            raise LLMRateLimitError(f"Gemini rate limit exceeded: {str(e)}")

        except (exceptions.ServiceUnavailable, exceptions.InternalServerError) as e:
            logger.error("llm_server_error", provider=self.provider_name, error=str(e))
            raise LLMServerError(f"Gemini server error: {str(e)}")

        except exceptions.DeadlineExceeded as e:
            logger.error("llm_timeout", provider=self.provider_name, error=str(e))
            raise LLMTimeoutError(f"Gemini request timed out: {str(e)}")

        except LLMError:
            # Re-raise our custom errors without wrapping
            raise

        except Exception as e:
            logger.error("llm_unknown_error", provider=self.provider_name, error=str(e))
            if "connection" in str(e).lower():
                raise LLMConnectionError(f"Connection failed: {str(e)}")
            raise LLMError(f"Unexpected error calling Gemini: {str(e)}")
