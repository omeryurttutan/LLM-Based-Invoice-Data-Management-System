import os
import base64
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import time
from openai import OpenAI, APIError, APIConnectionError, RateLimitError, AuthenticationError, APITimeoutError

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

class OpenAIProvider(BaseLLMProvider):
    """
    Implementation of BaseLLMProvider for OpenAI GPT-5.2 (using gpt-4o as current proxy if 5.2 not avail).
    Assuming 'gpt-4o' or config based model.
    """
    
    def __init__(self):
        self.api_key = settings.OPENAI_API_KEY
        if not self.api_key:
            logger.warning("openai_api_key_missing", message="OPENAI_API_KEY is not set.")
            
        self.client = OpenAI(api_key=self.api_key)
        self.model_name = os.getenv("OPENAI_MODEL", "gpt-4o") # Defaulting to 4o as 5.2 placeholder logic
        
    @property
    def provider_name(self) -> str:
        return "GPT"

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((APIConnectionError, RateLimitError, APITimeoutError, APIError)),
        reraise=True
    )
    def generate(self, image_bytes: bytes, prompt: str) -> str:
        if not self.api_key:
            raise LLMAuthenticationError("OpenAI API key is missing.")

        start_time = time.time()
        try:
            # Prepare image as data URL
            base64_image = base64.b64encode(image_bytes).decode('utf-8')
            data_url = f"data:image/jpeg;base64,{base64_image}"

            logger.info("llm_request_sent", provider=self.provider_name, model=self.model_name)

            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {
                        "role": "system", 
                        "content": "You are a helpful assistant. Output strict JSON." 
                    },
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt},
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{base64_image}"
                                }
                            }
                        ]
                    }
                ],
                temperature=0.1,
                max_tokens=4096,
                timeout=30
            )
            
            duration = (time.time() - start_time) * 1000
            logger.info("llm_response_received", provider=self.provider_name, duration_ms=duration)
            
            return response.choices[0].message.content

        except AuthenticationError as e:
            logger.error("llm_auth_error", error=str(e))
            raise LLMAuthenticationError(f"OpenAI Auth Error: {str(e)}")
            
        except RateLimitError as e:
            logger.warning("llm_rate_limit", error=str(e))
            raise LLMRateLimitError(f"OpenAI rate limit: {str(e)}")
            
        except APITimeoutError as e:
             logger.error("llm_timeout", error=str(e))
             raise LLMTimeoutError(f"OpenAI timeout: {str(e)}")
             
        except APIConnectionError as e:
            logger.error("llm_connection_error", error=str(e))
            raise LLMConnectionError(f"OpenAI connection error: {str(e)}")
            
        except APIError as e:
            # 5xx or generic
            logger.error("llm_server_error", error=str(e))
            raise LLMServerError(f"OpenAI server error: {str(e)}")
            
        except Exception as e:
            logger.error("llm_unknown_error", error=str(e))
            raise LLMError(f"Unexpected OpenAI error: {str(e)}")
