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
import unittest.mock
import httpx

# Set dummy API keys for testing BEFORE importing app modules
os.environ["GEMINI_API_KEY"] = "test-gemini-key"
os.environ["OPENAI_API_KEY"] = "test-openai-key"
os.environ["ANTHROPIC_API_KEY"] = "test-anthropic-key"
os.environ["INTERNAL_API_KEY"] = "change-this-at-runtime-2026"

import sys
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from app.models.invoice_data import InvoiceData, InvoiceItem
from app.models.requests import ExtractionRequest


# ─── Fixtures Dir ───────────────────────────────────────────────────────────────

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "fixtures")


def _load_fixture(rel_path: str) -> str:
    """Load a fixture file relative to tests/fixtures/."""
    path = os.path.join(FIXTURES_DIR, rel_path)
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


# ─── Sample Data ────────────────────────────────────────────────────────────────

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
    return _load_fixture("sample_xml/valid_invoice.xml")


# ─── LLM Response Fixtures ──────────────────────────────────────────────────────

@pytest.fixture
def mock_gemini_response_json() -> str:
    return _load_fixture("llm_responses/gemini_valid.json")


@pytest.fixture
def mock_gpt_response_json() -> str:
    return _load_fixture("llm_responses/gpt_valid.json")


@pytest.fixture
def mock_claude_response_json() -> str:
    return _load_fixture("llm_responses/claude_valid.json")


# ─── LLM Provider Mocks ─────────────────────────────────────────────────────────
# All three providers use Python SDKs (not raw httpx), so we mock at SDK level.

@pytest.fixture
def mock_gemini_api(mock_gemini_response_json):
    """
    Mock Gemini SDK: google.generativeai.GenerativeModel.generate_content
    """
    with unittest.mock.patch("google.generativeai.GenerativeModel.generate_content") as mock_generate:
        mock_response = MagicMock()
        mock_response.text = mock_gemini_response_json
        mock_generate.return_value = mock_response
        yield mock_generate


@pytest.fixture
def mock_gpt_api(mock_gpt_response_json):
    """
    Mock OpenAI SDK: openai.OpenAI.chat.completions.create
    We patch at the provider level for reliability.
    """
    with unittest.mock.patch(
        "app.services.llm.providers.openai_provider.OpenAIProvider.generate"
    ) as mock_generate:
        mock_generate.return_value = mock_gpt_response_json
        yield mock_generate


@pytest.fixture
def mock_claude_api(mock_claude_response_json):
    """
    Mock Anthropic SDK: anthropic.Anthropic.messages.create
    We patch at the provider level for reliability.
    """
    with unittest.mock.patch(
        "app.services.llm.providers.anthropic_provider.AnthropicProvider.generate"
    ) as mock_generate:
        mock_generate.return_value = mock_claude_response_json
        yield mock_generate


# ─── Clients ────────────────────────────────────────────────────────────────────

@pytest.fixture
def test_client() -> Generator[TestClient, None, None]:
    """Sync test client for the FastAPI app."""
    from app.main import app

    with unittest.mock.patch("app.main.ExtractionConsumer") as MockConsumer:
        mock_consumer_instance = MagicMock()
        MockConsumer.return_value = mock_consumer_instance
        with TestClient(app) as client:
            yield client


@pytest.fixture
async def async_client() -> Generator[httpx.AsyncClient, None, None]:
    """Async test client for the FastAPI app with auth header."""
    from app.main import app

    with unittest.mock.patch("app.main.ExtractionConsumer") as MockConsumer:
        mock_consumer_instance = MagicMock()
        MockConsumer.return_value = mock_consumer_instance

        headers = {"X-Internal-API-Key": os.environ["INTERNAL_API_KEY"]}
        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://test",
            headers=headers,
        ) as client:
            yield client


# ─── Model Fixtures ─────────────────────────────────────────────────────────────

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
        genel_toplam=118.00,
        vergi_toplam=18.00,
        para_birimi="TRY",
        kalemler=[
            InvoiceItem(
                aciklama="Test Ürün",
                miktar=1.0,
                birim_fiyat=100.00,
                kdv_orani=18,
                toplam_tutar=100.00,
            )
        ],
    )


@pytest.fixture
def mock_rabbitmq_producer():
    """Mock RabbitMQ publisher."""
    with unittest.mock.patch(
        "app.messaging.publisher.ResultPublisher"
    ) as MockPublisher:
        mock_publisher = MagicMock()
        MockPublisher.return_value = mock_publisher
        mock_publisher.publish_result = MagicMock(return_value=True)
        yield mock_publisher

