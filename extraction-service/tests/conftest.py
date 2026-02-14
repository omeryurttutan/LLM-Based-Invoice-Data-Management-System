import pytest
from PIL import Image
import io
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
    exif[0x0112] = 6 # Rotated 90 CW (Requires 90 CW to fix? Or implies 90 CW state?)
    # Tag 6 = The 0th row is the visual right-hand side of the image
    buf = io.BytesIO()
    img.save(buf, format='JPEG', exif=exif)
    return buf.getvalue()

@pytest.fixture
def mock_upload_file(test_image_bytes):
    from fastapi import UploadFile
    return UploadFile(filename="test.jpg", file=io.BytesIO(test_image_bytes))
