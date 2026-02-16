import pytest
from httpx import AsyncClient
from app.services.llm.base_provider import LLMError


@pytest.mark.asyncio
async def test_fallback_primary_fails_secondary_succeeds(
    async_client: AsyncClient,
    mock_gemini_api,
    mock_gpt_api,
    sample_image_bytes: bytes,
):
    """Gemini generate() fails → GPT succeeds."""
    # Make Gemini's generate() raise an error
    mock_gemini_api.side_effect = LLMError("Gemini unavailable")

    files = {"file": ("test.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    data = response.json()
    assert data["provider"] == "GPT"


@pytest.mark.asyncio
async def test_fallback_primary_secondary_fail_tertiary_succeeds(
    async_client: AsyncClient,
    mock_gemini_api,
    mock_gpt_api,
    mock_claude_api,
    sample_image_bytes: bytes,
):
    """Gemini fails → GPT fails → Claude succeeds."""
    mock_gemini_api.side_effect = LLMError("Gemini down")
    mock_gpt_api.side_effect = LLMError("GPT down")

    files = {"file": ("test.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    data = response.json()
    assert data["provider"] == "CLAUDE"


@pytest.mark.asyncio
async def test_fallback_all_fail(
    async_client: AsyncClient,
    mock_gemini_api,
    mock_gpt_api,
    mock_claude_api,
    sample_image_bytes: bytes,
):
    """All providers fail → 500/503."""
    mock_gemini_api.side_effect = LLMError("Gemini down")
    mock_gpt_api.side_effect = LLMError("GPT down")
    mock_claude_api.side_effect = LLMError("Claude down")

    files = {"file": ("test.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code in [500, 503]
