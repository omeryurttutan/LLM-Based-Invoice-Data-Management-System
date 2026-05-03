import pytest
import io
import json
from app.models.preprocessing import ProcessingOptions

@pytest.mark.asyncio
async def test_api_process_image(client, test_image_bytes):
    response = await client.post(
        "/api/v1/preprocessing/process",
        files={"file": ("test.jpg", test_image_bytes, "image/jpeg")},
        data={"options": "{}"}
    )
    assert response.status_code == 200
    data = response.json()
    assert "image_data" in data
    assert data["mime_type"] == "image/jpeg"
    assert data["width"] == 100
    assert data["height"] == 100

@pytest.mark.asyncio
async def test_api_process_image_with_options(client, test_image_bytes):
    options = ProcessingOptions(max_dimension=50, enhance_contrast=True)
    response = await client.post(
        "/api/v1/preprocessing/process",
        files={"file": ("test.jpg", test_image_bytes, "image/jpeg")},
        data={"options": options.model_dump_json()}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["width"] == 50
    assert data["metadata"]["was_resized"] is True

@pytest.mark.asyncio
async def test_api_process_invalid_file(client):
    response = await client.post(
        "/api/v1/preprocessing/process",
        files={"file": ("test.txt", b"not an image", "text/plain")},
        data={"options": "{}"}
    )
    assert response.status_code == 400
    # ErrorResponse structure puts detail in 'message' for HTTP exceptions if handled by http_exception_handler
    # Or 'detail' if raw fastapi exception passes through? 
    # Our main.py handles StarletteHTTPException and maps 'detail' to 'message'.
    data = response.json()
    assert "Unsupported" in data["message"]

@pytest.mark.asyncio
async def test_api_process_bad_json(client, test_image_bytes):
    response = await client.post(
        "/api/v1/preprocessing/process",
        files={"file": ("test.jpg", test_image_bytes, "image/jpeg")},
        data={"options": "{bad_json"}
    )
    assert response.status_code == 422
