from pydantic_settings import BaseSettings
from pydantic import Field

class ValidationSettings(BaseSettings):
    # Thresholds
    VALIDATION_AUTO_VERIFY_THRESHOLD: float = Field(90.0, env="VALIDATION_AUTO_VERIFY_THRESHOLD")
    VALIDATION_REVIEW_THRESHOLD: float = Field(70.0, env="VALIDATION_REVIEW_THRESHOLD")
    
    # Weights (must sum to 1.0)
    VALIDATION_WEIGHT_FIELD_COMPLETENESS: float = Field(0.30, env="VALIDATION_WEIGHT_FIELD_COMPLETENESS")
    VALIDATION_WEIGHT_FORMAT: float = Field(0.20, env="VALIDATION_WEIGHT_FORMAT")
    VALIDATION_WEIGHT_MATH: float = Field(0.30, env="VALIDATION_WEIGHT_MATH")
    VALIDATION_WEIGHT_RANGE: float = Field(0.10, env="VALIDATION_WEIGHT_RANGE")
    VALIDATION_WEIGHT_CROSSFIELD: float = Field(0.10, env="VALIDATION_WEIGHT_CROSSFIELD")
    
    # Tolerances & Limits
    VALIDATION_MATH_TOLERANCE: float = Field(0.05, env="VALIDATION_MATH_TOLERANCE")
    VALIDATION_MAX_INVOICE_AMOUNT: float = Field(100000000.0, env="VALIDATION_MAX_INVOICE_AMOUNT")
    VALIDATION_RE_EXTRACTION_ENABLED: bool = Field(False, env="VALIDATION_RE_EXTRACTION_ENABLED")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        extra = "ignore"

validation_settings = ValidationSettings()
