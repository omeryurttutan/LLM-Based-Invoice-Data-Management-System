from typing import Optional, Dict, Any
from pydantic import BaseModel, Field
from datetime import datetime
import uuid

class ExtractionRequest(BaseModel):
    message_id: str
    invoice_id: int
    company_id: int
    user_id: int
    file_path: str
    file_name: str
    file_type: str
    file_size: int
    priority: str = "NORMAL"
    attempt: int = 1
    max_attempts: int = 3
    timestamp: str
    correlation_id: str

class ExtractionResultMessage(BaseModel):
    message_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    correlation_id: str
    invoice_id: int
    status: str  # "COMPLETED" or "FAILED"
    
    # Success fields
    invoice_data: Optional[Dict[str, Any]] = None
    confidence_score: Optional[float] = None
    provider: Optional[str] = None
    suggested_status: Optional[str] = None
    
    # Error fields
    error_code: Optional[str] = None
    error_message: Optional[str] = None
    
    # Metadata
    processing_duration_ms: int
    timestamp: str = Field(default_factory=lambda: datetime.utcnow().isoformat())
