import pytest
from PIL import Image
import io
import json
import os
try:
    import fitz # PyMuPDF
except ImportError:
    fitz = None
try:
    from reportlab.pdfgen import canvas
except ImportError:
    canvas = None
from httpx import AsyncClient, ASGITransport
from app.main import app

# --- App Fixtures ---

@pytest.fixture
async def client():
    """Async test client for the FastAPI app."""
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        yield c

# --- Image/PDF Fixtures ---

@pytest.fixture(scope="session")
def test_image_bytes():
    """Create a simple JPEG image in bytes."""
    img = Image.new('RGB', (100, 100), color='red')
    buf = io.BytesIO()
    img.save(buf, format='JPEG')
    return buf.getvalue()

@pytest.fixture(scope="session")
def test_png_bytes():
    """Create a simple PNG image in bytes."""
    img = Image.new('RGB', (100, 100), color='blue')
    buf = io.BytesIO()
    img.save(buf, format='PNG')
    return buf.getvalue()

@pytest.fixture(scope="session")
def test_pdf_bytes():
    """Create a simple single-page PDF in bytes."""
    if canvas is None:
        pytest.skip("reportlab not installed")
    buf = io.BytesIO()
    c = canvas.Canvas(buf)
    c.drawString(100, 750, "Hello World")
    c.save()
    return buf.getvalue()

@pytest.fixture(scope="session")
def test_multipage_pdf_bytes():
    """Create a 3-page PDF in bytes."""
    if canvas is None:
        pytest.skip("reportlab not installed")
    buf = io.BytesIO()
    c = canvas.Canvas(buf)
    c.drawString(100, 750, "Page 1")
    c.showPage()
    c.drawString(100, 750, "Page 2")
    c.showPage()
    c.drawString(100, 750, "Page 3")
    c.save()
    return buf.getvalue()

@pytest.fixture(scope="session")
def rotated_image_bytes():
    """Create an image with EXIF rotation."""
    img = Image.new('RGB', (100, 50), color='green')
    exif = img.getexif()
    exif[0x0112] = 6 # Rotated 90 CW
    buf = io.BytesIO()
    img.save(buf, format='JPEG', exif=exif)
    return buf.getvalue()

@pytest.fixture
def mock_upload_file(test_image_bytes):
    from fastapi import UploadFile
    return UploadFile(filename="test.jpg", file=io.BytesIO(test_image_bytes))

# --- LLM Response Fixtures ---

@pytest.fixture
def mock_gemini_response():
    """Return a mock Gemini response."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "llm_responses", "gemini_valid.json")
    with open(path, "r") as f:
        return f.read()

@pytest.fixture
def mock_gpt_response():
    """Return a mock OpenAI response."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "llm_responses", "gpt_valid.json")
    with open(path, "r") as f:
        return f.read()

@pytest.fixture
def mock_claude_response():
    """Return a mock Claude response."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "llm_responses", "claude_valid.json")
    with open(path, "r") as f:
        return f.read()

@pytest.fixture
def mock_malformed_response():
    """Return a malformed JSON response."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "llm_responses", "malformed.json")
    with open(path, "r") as f:
        return f.read()

# --- XML Fixtures ---

@pytest.fixture
def sample_xml_content():
    """Return a sample valid UBL-TR XML content."""
    path = os.path.join(os.path.dirname(__file__), "fixtures", "sample_xml", "valid_invoice.xml")
    with open(path, "r") as f:
        return f.read()
