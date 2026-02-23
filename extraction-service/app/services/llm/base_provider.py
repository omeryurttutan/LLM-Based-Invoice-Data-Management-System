from abc import ABC, abstractmethod
from typing import Optional


# ─── Provider Name Constants ────────────────────────────────────────────────────
# These map to the `llm_provider` column in the invoices table.

class LLMProviderNames:
    GEMINI_3_FLASH = "GEMINI_3_FLASH"
    GEMINI_2_5_FLASH = "GEMINI_2_5_FLASH"  # Phase 16 fallback 1
    GPT5_NANO = "GPT5_NANO"                # Phase 16 fallback 2


# ─── Custom Error Types ─────────────────────────────────────────────────────────

class LLMError(Exception):
    """Base exception for LLM related errors."""
    pass

class LLMTimeoutError(LLMError):
    """Raised when LLM request times out."""
    pass

class LLMRateLimitError(LLMError):
    """Raised when LLM rate limit is exceeded."""
    pass

class LLMServerError(LLMError):
    """Raised when LLM provider returns a 5xx error."""
    pass

class LLMAuthenticationError(LLMError):
    """Raised when API key is invalid."""
    pass

class LLMConnectionError(LLMError):
    """Raised when network connection fails."""
    pass

class LLMResponseError(LLMError):
    """Raised when LLM response is malformed or empty."""
    pass


# ─── Abstract Base Class ────────────────────────────────────────────────────────

class BaseLLMProvider(ABC):
    """
    Abstract base class for all LLM providers.
    Defines the interface for multimodal invoice extraction.
    
    Providers:
      - Gemini 3 Flash  (primary)
      - Gemini 2.5 Flash (fallback 1, Phase 16)
      - GPT-5 nano       (fallback 2, Phase 16)
    """

    def __init__(
        self,
        timeout: int = 30,
        max_retries: int = 2,
        retry_delay: float = 1.0,
    ):
        self.timeout = timeout
        self.max_retries = max_retries
        self.retry_delay = retry_delay

    @abstractmethod
    async def generate(self, image_bytes: bytes, prompt: str, mime_type: str = "image/jpeg") -> tuple[str, dict]:
        """
        Send an image and a prompt to the LLM and return the text response.
        
        Args:
            image_bytes: Raw bytes of the image file (JPEG/PNG/etc.)
            prompt: Text prompt instructions
            mime_type: MIME type of the image (e.g. "image/jpeg", "image/png")
            
        Returns:
            A tuple containing:
            - The raw text response from the LLM.
            - A dictionary containing usage statistics (input_tokens, output_tokens).
            
        Raises:
            LLMError: Base class for all specific LLM errors.
        """
        pass

    @property
    @abstractmethod
    def provider_name(self) -> str:
        """Return the unique identifier for this provider (e.g., 'GEMINI_3_FLASH')."""
        pass

    @abstractmethod
    def is_available(self) -> bool:
        """Check if the provider is available and properly configured."""
        pass
