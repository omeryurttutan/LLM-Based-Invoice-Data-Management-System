from typing import List, Optional, Any, Dict
from pydantic import BaseModel, Field
from datetime import datetime

class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)

class DependencyStatus(BaseModel):
    status: str
    details: Optional[Dict[str, Any]] = None

class DependencyHealthResponse(BaseModel):
    spring_boot: DependencyStatus
    rabbitmq: DependencyStatus
    llm_providers: Dict[str, bool]

class ErrorResponse(BaseModel):
    error: str
    message: str
    details: Optional[Dict[str, Any]] = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    request_id: Optional[str] = None
