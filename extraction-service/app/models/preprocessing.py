from typing import List, Optional, Tuple, Dict, Any
from enum import Enum
from pydantic import BaseModel, Field, ConfigDict

class SupportedFormat(str, Enum):
    JPEG = "image/jpeg"
    PNG = "image/png"
    PDF = "application/pdf"
    TIFF = "image/tiff"
    WEBP = "image/webp"
    BMP = "image/bmp"

class ProcessingPreset(str, Enum):
    QUALITY_FIRST = "QUALITY_FIRST"
    BALANCED = "BALANCED"
    SIZE_OPTIMIZED = "SIZE_OPTIMIZED"
    FAST = "FAST"

class ProcessingOptions(BaseModel):
    preset: ProcessingPreset = Field(default=ProcessingPreset.BALANCED)
    skip_preprocessing: bool = False
    auto_rotate: bool = True
    auto_deskew: bool = False
    enhance_contrast: bool = True
    enhance_sharpness: bool = True
    target_format: str = "image/jpeg"
    jpeg_quality: int = Field(default=85, ge=1, le=100)
    max_dimension: int = 4096
    max_file_size_kb: int = 5000
    
    # PDF specific
    pdf_dpi: int = 200
    pdf_max_pages: int = 10
    pdf_page_selection: str = "first"  # "all", "first", "1,3,5"

class ProcessingMetadata(BaseModel):
    was_pdf: bool = False
    pdf_page_count: int = 0
    pdf_page_processed: int = 0
    was_rotated: bool = False
    rotation_degrees: int = 0
    was_deskewed: bool = False
    deskew_angle: float = 0.0
    was_enhanced: bool = False
    was_resized: bool = False
    original_dimensions: Tuple[int, int] = (0, 0)
    final_dimensions: Tuple[int, int] = (0, 0)
    compression_ratio: float = 1.0
    processing_steps: List[str] = Field(default_factory=list)

class ProcessedImage(BaseModel):
    image_data: str  # Base64 string
    mime_type: str
    original_filename: str = ""
    original_format: str = ""
    original_size_bytes: int = 0
    processed_size_bytes: int = 0
    width: int
    height: int
    metadata: ProcessingMetadata
    warnings: List[str] = Field(default_factory=list)
    processing_time_ms: int = 0
    
    model_config = ConfigDict(arbitrary_types_allowed=True)
