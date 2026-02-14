import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
import os

FIXTURE_DIR = os.path.join(os.path.dirname(__file__), "..", "fixtures", "xml")


def _read_fixture(filename):
    with open(os.path.join(FIXTURE_DIR, filename), "rb") as f:
        return f.read()


@pytest.mark.asyncio
async def test_extract_xml_endpoint():
    """Full flow: XML file → POST /extract → XML parser → validation → ExtractionResult."""
    file_content = _read_fixture("valid_einvoice.xml")

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/extraction/extract",
            files={"file": ("invoice.xml", file_content, "application/xml")}
        )

        assert response.status_code == 200
        data = response.json()

        assert data["provider"] == "XML_PARSER"
        assert data["confidence_score"] > 90
        assert data["data"]["invoice_number"] == "GIB202300000001"
        assert data["data"]["e_invoice_uuid"] == "f47ac10b-58cc-4372-a567-0e02b2c3d479"


@pytest.mark.asyncio
async def test_parse_xml_endpoint():
    """POST /parse/xml with valid XML."""
    file_content = _read_fixture("valid_einvoice.xml")

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/extraction/parse/xml",
            files={"file": ("invoice.xml", file_content, "application/xml")}
        )
        assert response.status_code == 200
        data = response.json()
        assert data["provider"] == "XML_PARSER"
        assert data["data"]["invoice_number"] == "GIB202300000001"


@pytest.mark.asyncio
async def test_parse_xml_rejects_non_xml():
    """POST /parse/xml with image file should be rejected."""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/extraction/parse/xml",
            files={"file": ("test.jpg", b"fakeimage", "image/jpeg")}
        )
        assert response.status_code != 200


@pytest.mark.asyncio
async def test_supported_types_endpoint():
    """GET /parse/xml/supported-types returns type list."""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/api/v1/extraction/parse/xml/supported-types")

        assert response.status_code == 200
        data = response.json()
        assert "types" in data
        assert "SATIS" in data["types"]
        assert "IADE" in data["types"]
        assert "formats" in data


@pytest.mark.asyncio
async def test_extract_xml_source_type():
    """Verify source_type behavior for XML-parsed invoices."""
    file_content = _read_fixture("valid_einvoice.xml")

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/extraction/extract",
            files={"file": ("invoice.xml", file_content, "application/xml")}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["provider"] == "XML_PARSER"


@pytest.mark.asyncio
async def test_extract_multi_item_xml():
    """POST /extract with 3-item invoice returns all items."""
    file_content = _read_fixture("valid_einvoice_standard.xml")

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/extraction/extract",
            files={"file": ("invoice.xml", file_content, "application/xml")}
        )

        assert response.status_code == 200
        data = response.json()
        assert len(data["data"]["items"]) == 3
        assert data["data"]["total_amount"] == 10030.00


@pytest.mark.asyncio
async def test_extract_malformed_xml_returns_error():
    """POST /extract with malformed XML should return error."""
    file_content = _read_fixture("malformed.xml")

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/extraction/extract",
            files={"file": ("bad.xml", file_content, "application/xml")}
        )
        assert response.status_code != 200


@pytest.mark.asyncio
async def test_extract_not_einvoice_returns_error():
    """POST /extract with non-e-Invoice XML should return error."""
    file_content = _read_fixture("not_einvoice.xml")

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/extraction/extract",
            files={"file": ("catalog.xml", file_content, "application/xml")}
        )
        assert response.status_code != 200
