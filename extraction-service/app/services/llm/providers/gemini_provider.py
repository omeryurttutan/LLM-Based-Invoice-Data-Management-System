import google.generativeai as genai
from google.api_core import exceptions
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import time
import os
from typing import Optional

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

class GeminiProvider(BaseLLMProvider):
    """
    Implementation of BaseLLMProvider for Google Gemini 3 Flash.
    """
    
    def __init__(self):
        self.api_key = settings.GEMINI_API_KEY
        if not self.api_key:
            logger.warning("gemini_api_key_missing", message="GEMINI_API_KEY is not set.")
        
        genai.configure(api_key=self.api_key)
        
        # Determine model name (allow override via env var, default to gemini-2.0-flash)
        # Fallback to gemini-1.5-flash if 2.0 is not yet available in the region/sdk
        self.model_name = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
        
        self.generation_config = {
            "temperature": 0.1,
            "max_output_tokens": 4096,
            "response_mime_type": "application/json",
        }
        
        # Safety settings: Block minimal content
        self.safety_settings = [
            {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
            {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"},
        ]

    @property
    def provider_name(self) -> str:
        return "GEMINI"

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((exceptions.ResourceExhausted, exceptions.ServiceUnavailable, exceptions.InternalServerError)),
        reraise=True
    )
    def generate(self, image_bytes: bytes, prompt: str) -> str:
        if not self.api_key:
            raise LLMAuthenticationError("Gemini API key is missing.")

        start_time = time.time()
        try:
            model = genai.GenerativeModel(
                model_name=self.model_name,
                generation_config=self.generation_config,
                safety_settings=self.safety_settings
            )
            
            # Create content parts
            # google-generativeai supports passing bytes directly if wrapped in a dict with mime_type
            # However, simpler to pass image object if using PIL, but here we have bytes.
            # The SDK accepts a list of parts. For image bytes: 
            # {"mime_type": "image/jpeg", "data": image_bytes}
            # We'll assume jpeg/png based on detection or just generic image/jpeg for now, 
            # or better: we rely on the preprocessing to output standardized format (likely JPEG).
            
            image_part = {
                "mime_type": "image/jpeg", 
                "data": image_bytes
            }
            
            contents = [prompt, image_part]
            
            logger.info("llm_request_sent", provider=self.provider_name, model=self.model_name)
            
            response = model.generate_content(
                contents,
                request_options={"timeout": 30} # 30s timeout
            )
            
            duration = (time.time() - start_time) * 1000
            logger.info("llm_response_received", provider=self.provider_name, duration_ms=duration)
            
            return response.text

        except exceptions.InvalidArgument as e:
            # Usually API key issue or bad request
            logger.error("llm_auth_error", error=str(e))
            raise LLMAuthenticationError(f"Invalid argument or API key: {str(e)}")
            
        except exceptions.ResourceExhausted as e:
            # Rate limit (429)
            logger.warning("llm_rate_limit", error=str(e))
            raise LLMRateLimitError(f"Gemini rate limit exceeded: {str(e)}")
            
        except (exceptions.ServiceUnavailable, exceptions.InternalServerError) as e:
            # 5xx errors
            logger.error("llm_server_error", error=str(e))
            raise LLMServerError(f"Gemini server error: {str(e)}")
            
        except exceptions.DeadlineExceeded as e:
            # Timeout
            logger.error("llm_timeout", error=str(e))
            raise LLMTimeoutError(f"Gemini request timed out: {str(e)}")
            
        except Exception as e:
            # Catch-all
            logger.error("llm_unknown_error", error=str(e))
            # Check if it's a connection error from underlying http lib
            if "connection" in str(e).lower():
                raise LLMConnectionError(f"Connection failed: {str(e)}")
            raise LLMError(f"Unexpected error calling Gemini: {str(e)}")
