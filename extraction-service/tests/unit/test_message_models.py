import pytest
from app.messaging.message_models import ExtractionRequest, ExtractionResultMessage
from pydantic import ValidationError
import uuid

def test_extraction_request_valid():
    data = {
        "message_id": str(uuid.uuid4()),
        "invoice_id": 123,
        "company_id": 1,
        "user_id": 1,
        "file_path": "/data/invoices/test.pdf",
        "file_name": "test.pdf",
        "file_type": "application/pdf",
        "file_size": 1024,
        "timestamp": "2024-01-01T12:00:00Z",
        "correlation_id": str(uuid.uuid4())
    }
    request = ExtractionRequest(**data)
    assert request.invoice_id == 123
    assert request.priority == "NORMAL" # Default
    assert request.attempt == 1 # Default

def test_extraction_request_invalid_missing_fields():
    data = {
        "invoice_id": 123
        # Missing required fields
    }
    with pytest.raises(ValidationError):
        ExtractionRequest(**data)

def test_extraction_result_success():
    data = {
        "correlation_id": str(uuid.uuid4()),
        "invoice_id": 123,
        "status": "COMPLETED",
        "invoice_data": {"invoice_number": "TR123"},
        "confidence_score": 95.5,
        "provider": "GEMINI",
        "suggested_status": "AUTO_VERIFIED",
        "processing_duration_ms": 1500
    }
    result = ExtractionResultMessage(**data)
    assert result.status == "COMPLETED"
    assert result.message_id is not None # Auto-generated
    assert result.timestamp is not None # Auto-generated

def test_extraction_result_failed():
    data = {
        "correlation_id": str(uuid.uuid4()),
        "invoice_id": 123,
        "status": "FAILED",
        "error_code": "FILE_NOT_FOUND",
        "error_message": "File not found",
        "processing_duration_ms": 100
    }
    result = ExtractionResultMessage(**data)
    assert result.status == "FAILED"
    assert result.error_code == "FILE_NOT_FOUND"
