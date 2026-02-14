from abc import ABC, abstractmethod
from typing import Optional

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

class BaseLLMProvider(ABC):
    """
    Abstract base class for all LLM providers (Gemini, GPT, Claude).
    Defines the interface for multimodal generation.
    """
    
    @abstractmethod
    def generate(self, image_bytes: bytes, prompt: str) -> str:
        """
        Send an image and a prompt to the LLM and return the text response.
        
        Args:
            image_bytes: Raw bytes of the image file (JPEG/PNG/etc.)
            prompt: Text prompt instructions
            
        Returns:
            The raw text response from the LLM.
            
        Raises:
            LLMError: Base class for all specific LLM errors.
        """
        pass

    @property
    @abstractmethod
    def provider_name(self) -> str:
        """Return the unique identifier for this provider (e.g., 'GEMINI')."""
        pass

    @abstractmethod
    def is_available(self) -> bool:
        """Check if the provider is available and properly configured."""
        pass
