import pytest
import os
from unittest.mock import patch, AsyncMock, MagicMock
from app.config.settings import Settings

@pytest.mark.asyncio
async def test_health_check(client):
    response = await client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "version" in data
    assert "timestamp" in data

@pytest.mark.asyncio
async def test_health_live(client):
    response = await client.get("/health/live")
    assert response.status_code == 200
    assert response.text == "OK"

@pytest.mark.asyncio
async def test_health_ready(client):
    response = await client.get("/health/ready")
    assert response.status_code == 200
    assert response.text == "OK"

@pytest.mark.asyncio
async def test_dependencies_mock(client):
    # Mock httpx.AsyncClient in the health module to simulate Spring Boot being UP
    # We mock the context manager returned by AsyncClient()
    with patch("app.api.routes.health.httpx.AsyncClient") as MockAsyncClient:
        # Setup the mock client instance that is returned by __aenter__
        mock_client_instance = AsyncMock()
        MockAsyncClient.return_value.__aenter__.return_value = mock_client_instance
        
        # Setup the response matching what we expect from Spring Boot
        mock_response = MagicMock()
        mock_response.status_code = 200
        mock_response.json.return_value = {"status": "UP"}
        
        # Configure the get method to return our mock response
        mock_client_instance.get.return_value = mock_response
        
        response = await client.get("/health/dependencies")
        assert response.status_code == 200
        data = response.json()
        assert data["spring_boot"]["status"] == "up"
        # Check LLM providers keys (should be false in test env unless set)
        assert "llm_providers" in data
