import base64
from typing import Optional

class Base64Encoder:
    """Handles Base64 encoding for API responses."""
    
    @staticmethod
    def encode(image_bytes: bytes, mime_type: Optional[str] = None) -> str:
        """
        Encode bytes to base64 string.
        """
        return base64.b64encode(image_bytes).decode("utf-8")
        
    @staticmethod
    def encode_with_uri(image_bytes: bytes, mime_type: str) -> str:
        """
        Encode bytes to base64 data URI string.
        """
        b64_str = base64.b64encode(image_bytes).decode("utf-8")
        return f"data:{mime_type};base64,{b64_str}"
