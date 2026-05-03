from typing import Optional, List
from pydantic import BaseModel
from fastapi import UploadFile

class ExtractionRequest(BaseModel):
    # This model acts as a future reference since files are handled via Form parameters
    provider: Optional[str] = None
    options: Optional[dict] = None
