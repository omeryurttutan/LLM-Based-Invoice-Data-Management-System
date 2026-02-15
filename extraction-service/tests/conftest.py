import pytest
import os
import io
import json
import base64
from typing import Dict, Any, Generator
from PIL import Image
from fastapi.testclient import TestClient
from unittest.mock import MagicMock
import unittest

# Set dummy API keys for testing BEFORE importing app modules
os.environ["GEMINI_API_KEY"] = "test-gemini-key"
os.environ["OPENAI_API_KEY"] = "test-openai-key"
os.environ["ANTHROPIC_API_KEY"] = "test-anthropic-key"
os.environ["INTERNAL_API_KEY"] = "fatura-ocr-internal-secret-key-2026"

# Import app modules
import sys
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from app.main import app
from app.models.invoice_data import InvoiceData, InvoiceItem
from app.models.requests import ExtractionRequest
# from app.models.enums import LLMProvider # Commenting out for now as file not found

@pytest.fixture
def test_client() -> Generator[TestClient, None, None]:
    """Sync test client for the FastAPI app."""
    # Mock ExtractionConsumer to prevent RabbitMQ connection attempts
    with unittest.mock.patch("app.main.ExtractionConsumer") as MockConsumer:
        mock_consumer_instance = MagicMock()
        MockConsumer.return_value = mock_consumer_instance
        
        with TestClient(app) as client:
            yield client

@pytest.fixture
def sample_image_bytes() -> bytes:
    """Create a 100x100 white RGB image in memory."""
    img = Image.new('RGB', (100, 100), color='white')
    buf = io.BytesIO()
    img.save(buf, format='JPEG')
    return buf.getvalue()

@pytest.fixture
def sample_xml_content() -> str:
    """Return a valid minimal UBL-TR Invoice XML."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "sample_xml", "valid_invoice.xml")
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

@pytest.fixture
def mock_invoice_data() -> InvoiceData:
    """Return a valid InvoiceData object."""
    return InvoiceData(
        fatura_no="FTR2024000001",
        tarih="2024-01-01",
        gonderici_vkn="1111111111",
        gonderici_unvan="Test Tedarikçi A.Ş.",
        alici_vkn="2222222222",
        alici_unvan="Test Alıcı Ltd.",
        toplam_tutar=118.00,
        vergi_toplam=18.00,
        para_birimi="TRY",
        kalemler=[
            InvoiceItem(
                aciklama="Test Ürün",
                miktar=1.0,
                birim_fiyat=100.00,
                kdv_orani=18,
                toplam=100.00
            )
        ]
    )

@pytest.fixture
def mock_gemini_response_json() -> str:
    """Return mock Gemini response from file."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "llm_responses", "gemini_valid.json")
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

@pytest.fixture
def mock_gpt_response_json() -> str:
    """Return mock GPT response from file."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "llm_responses", "gpt_valid.json")
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

@pytest.fixture
def mock_claude_response_json() -> str:
    """Return mock Claude response from file."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "llm_responses", "claude_valid.json")
    with open(path, "r", encoding="utf-8") as f:
        return f.read()
