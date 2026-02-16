import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_api_auth_missing_key(async_client: AsyncClient, sample_image_bytes):
    """Request without API key → 401."""
    if "X-Internal-API-Key" in async_client.headers:
        del async_client.headers["X-Internal-API-Key"]

    files = {"file": ("test.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 401
    assert response.json()["error"] == "UNAUTHORIZED"


@pytest.mark.asyncio
async def test_api_auth_invalid_key(async_client: AsyncClient, sample_image_bytes):
    """Request with wrong API key → 401."""
    async_client.headers["X-Internal-API-Key"] = "wrong-key"

    files = {"file": ("test.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 401


@pytest.mark.asyncio
async def test_api_valid_request(
    async_client: AsyncClient, mock_gemini_api, sample_image_bytes
):
    """Valid request → 200 with ExtractionResponse."""
    files = {"file": ("test.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    data = response.json()
    assert "data" in data
    # Middleware sets X-Request-ID header
    assert "X-Request-ID" in response.headers


@pytest.mark.asyncio
async def test_api_invalid_file_type(async_client: AsyncClient):
    """Unsupported file type → 400 or 500 (pipeline raises ValueError/PreprocessingError)."""
    files = {"file": ("test.txt", b"some text content", "text/plain")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    # Pipeline may return 400 (ExtractionServiceException) or 500 (generic Exception)
    assert response.status_code in [400, 500]
