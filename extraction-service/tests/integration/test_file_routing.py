import pytest
from httpx import AsyncClient
from unittest.mock import MagicMock


@pytest.mark.asyncio
async def test_routing_image_files(
    async_client: AsyncClient,
    mock_gemini_api,
    sample_image_bytes: bytes,
):
    """Image files → LLM pipeline."""
    files = {"file": ("test.jpg", sample_image_bytes, "image/jpeg")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    assert response.json()["provider"] == "GEMINI"


@pytest.mark.asyncio
async def test_routing_xml_files(
    async_client: AsyncClient,
    mock_gemini_api,
    sample_xml_content: str,
):
    """XML files → XML parser, LLM NOT called."""
    mock_gemini_api.reset_mock()

    files = {"file": ("invoice.xml", sample_xml_content.encode("utf-8"), "application/xml")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    data = response.json()

    # XML parser sets provider to "XML_PARSER"
    assert data["provider"] == "XML_PARSER"
    # Gemini should NOT have been called
    mock_gemini_api.assert_not_called()


@pytest.mark.asyncio
async def test_routing_unsupported_file(async_client: AsyncClient):
    """Unsupported file type → error status."""
    files = {"file": ("test.txt", b"some text", "text/plain")}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code in [400, 500]
