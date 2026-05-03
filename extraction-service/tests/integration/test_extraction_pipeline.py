import pytest
from httpx import AsyncClient
from unittest.mock import MagicMock
from app.services.llm.base_provider import LLMError


@pytest.mark.asyncio
async def test_extraction_happy_path(
    async_client: AsyncClient,
    mock_gemini_api,
    sample_image_bytes: bytes,
):
    """Full pipeline: valid image → Gemini mock → ExtractionResponse."""
    files = {"file": ("test_invoice.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    data = response.json()

    assert data["provider"] == "GEMINI"
    assert "data" in data

    invoice = data["data"]
    # gemini_valid.json → fatura_no = "GIB2024000000001"
    assert invoice["fatura_no"] == "GIB2024000000001"
    assert len(invoice["kalemler"]) == 1


@pytest.mark.asyncio
async def test_extraction_low_quality_response(
    async_client: AsyncClient,
    mock_gemini_api,
    sample_image_bytes: bytes,
):
    """Gemini returns partial / low-confidence data."""
    import json

    incomplete_json = json.dumps({
        "fatura_no": None,
        "tarih": "2024-01-01",
        "genel_toplam": 100.00,
        "kalemler": [],
    })
    mock_response = MagicMock()
    mock_response.text = incomplete_json
    mock_gemini_api.return_value = mock_response

    files = {"file": ("low_quality.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    data = response.json()
    assert data["data"]["fatura_no"] is None
    assert "confidence_score" in data


@pytest.mark.asyncio
async def test_extraction_gemini_error_triggers_fallback(
    async_client: AsyncClient,
    mock_gemini_api,
    mock_gpt_api,
    sample_image_bytes: bytes,
):
    """
    When Gemini's generate() raises an LLMError, the FallbackChain
    catches it and tries GPT (which is mocked to succeed).
    """
    # Make Gemini raise an error (not return bad text — that won't trigger fallback)
    mock_gemini_api.side_effect = LLMError("Gemini generation failed")

    files = {"file": ("test_fallback.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    data = response.json()
    # FallbackChain returns provider_name "GPT"
    assert data["provider"] == "GPT"
    assert "data" in data
