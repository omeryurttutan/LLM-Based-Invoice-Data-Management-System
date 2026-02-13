from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field
from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationResult

class ExtractionResponse(BaseModel):
    """
    Response model for invoice extraction.
    Wraps the extracted invoice data with metadata.
    """
    data: InvoiceData
    provider: str = Field(..., description="LLM provider used")
    processing_time_ms: float = Field(..., description="Processing time in ms")
    confidence_score: Optional[float] = Field(None, description="Confidence score (0-100)")
    suggested_status: Optional[str] = Field(None, description="Suggested status")
    validation_result: Optional[ValidationResult] = Field(None, description="Detailed validation result")
    raw_response: Optional[str] = Field(None, description="Raw LLM response (for debugging)")
    fallback_attempts: Optional[list] = Field(None, description="Log of fallback attempts")
