import pytest
from unittest.mock import MagicMock, patch
import os
import json
from fastapi.testclient import TestClient
from app.main import app
from app.config.settings import settings

@pytest.mark.asyncio
async def test_extraction_pipeline_happy_path(test_client, mock_gemini_response_json):
    """
    2.1 Happy Path — Standard Invoice Image:
    - Input: standard_invoice.jpg
    - Mock: Gemini API returns gemini_complete.json
    - Verify: All fields extracted, confidence score > 80, provider = GEMINI
    """
    
    # Path to the dummy image we created
    image_path = os.path.join(os.path.dirname(__file__), "../fixtures/sample_invoices/standard_invoice.jpg")
    
    # Read image bytes
    with open(image_path, "rb") as f:
        image_bytes = f.read()
    
    # Mock Gemini API using unittest.mock instead of respx because SDK might use grpc/requests
    # We patch the generate_content method of the GenerativeModel class
    with patch("google.generativeai.GenerativeModel.generate_content") as mock_generate:
        # Configure the mock to return a response object with .text attribute
        mock_response = MagicMock()
        mock_response.text = mock_gemini_response_json
        mock_generate.return_value = mock_response

        # We need to simulate the API call via the endpoint
        # POST /api/v1/extraction/extract
        
        files = {
            "file": ("standard_invoice.jpg", image_bytes, "image/jpeg")
        }
        
        headers = {
            "X-Internal-API-Key": settings.INTERNAL_API_KEY
        }
        
        response = test_client.post("/api/v1/extraction/extract", files=files, headers=headers)
        
        # Verify response
        assert response.status_code == 200, f"Response: {response.text}"
        data = response.json()
        
        # ExtractionResponse fields: data, provider, processing_time_ms, confidence_score, suggested_status
        assert data["provider"] == "GEMINI"
        assert "data" in data
        assert data["confidence_score"] > 80
        assert data["suggested_status"] in ["VERIFIED", "NEEDS_REVIEW", "REJECTED"]
