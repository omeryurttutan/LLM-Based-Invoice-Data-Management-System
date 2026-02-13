import pytest
from app.models.responses import HealthResponse
from datetime import datetime

def test_health_response_model():
    now = datetime.utcnow()
    model = HealthResponse(
        status="healthy",
        service="Test Service",
        version="0.0.1",
        timestamp=now
    )
    assert model.status == "healthy"
    assert model.service == "Test Service"
    
def test_health_response_default_timestamp():
    model = HealthResponse(
        status="healthy", 
        service="Test", 
        version="1"
    )
    assert isinstance(model.timestamp, datetime)
