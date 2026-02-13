from typing import List, Dict, Optional
from pydantic import BaseModel, Field
from enum import Enum

class ValidationSeverity(str, Enum):
    CRITICAL = "CRITICAL"
    WARNING = "WARNING"
    INFO = "INFO"

class ValidationCategory(str, Enum):
    FIELD_COMPLETENESS = "A"
    FORMAT_VALIDATION = "B"
    MATH_CONSISTENCY = "C"
    VALUE_RANGE = "D"
    CROSS_FIELD_LOGIC = "E"

class ValidationIssue(BaseModel):
    field: str = Field(..., description="Field where the issue was found")
    category: ValidationCategory = Field(..., description="Validation category")
    severity: ValidationSeverity = Field(..., description="Severity level")
    message: str = Field(..., description="Human readable message")
    expected_value: Optional[str] = Field(None, description="Expected value if applicable")
    actual_value: Optional[str] = Field(None, description="Actual value found")

class ValidationResult(BaseModel):
    confidence_score: float = Field(..., description="Overall confidence score 0-100")
    suggested_status: str = Field(..., description="AUTO_VERIFIED, NEEDS_REVIEW, or LOW_CONFIDENCE")
    category_scores: Dict[str, float] = Field(..., description="Breakdown of scores by category")
    issues: List[ValidationIssue] = Field(default_factory=list, description="List of validation issues")
    summary: str = Field(..., description="Human readable summary")
