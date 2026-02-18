from fastapi import APIRouter, status, Response, Request
from datetime import datetime
import httpx
from app.config.settings import settings
from app.models.responses import HealthResponse, DependencyHealthResponse, DependencyStatus

router = APIRouter(tags=["Health"])

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Basic health check"""
    return HealthResponse(
        status="healthy",
        service=settings.APP_NAME,
        version=settings.APP_VERSION,
        timestamp=datetime.utcnow()
    )

@router.get("/health/live")
async def liveness_check():
    """Liveness check - simple ping to confirm service is running"""
    return Response(status_code=status.HTTP_200_OK, content="OK")

@router.get("/health/integration")
async def integration_check():
    """Integration test - round-trip connectivity with Spring Boot backend"""
    import time as _time
    result = {
        "python_to_spring_boot": {"status": "unknown", "latency_ms": None},
        "timestamp": datetime.utcnow().isoformat()
    }
    try:
        start = _time.time()
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(f"{settings.SPRING_BOOT_URL.replace('/api/v1', '')}/actuator/health")
            latency = (_time.time() - start) * 1000
            if response.status_code == 200:
                result["python_to_spring_boot"] = {
                    "status": "connected",
                    "latency_ms": round(latency, 2),
                    "response": response.json()
                }
            else:
                result["python_to_spring_boot"] = {
                    "status": "error",
                    "latency_ms": round(latency, 2),
                    "status_code": response.status_code
                }
    except Exception as e:
        result["python_to_spring_boot"] = {
            "status": "unreachable",
            "latency_ms": None,
            "error": str(e)
        }
    return result

@router.get("/health/ready")
async def readiness_check(request: Request):
    """Readiness check including RabbitMQ consumer status"""
    is_ready = False
    
    # Check Consumer Status
    if hasattr(request.app.state, "consumer"):
        is_ready = request.app.state.consumer.is_ready
    
    if is_ready:
        return Response(status_code=status.HTTP_200_OK, content="OK")
    else:
        return Response(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, content="Consumer Not Ready")

@router.get("/health/dependencies", response_model=DependencyHealthResponse)
async def dependency_check(request: Request):
    """Check dependencies (Spring Boot, LLM Providers, RabbitMQ)"""
    
    # Check Spring Boot Backend
    spring_boot_status = "unknown"
    spring_boot_details = {}
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            # Using actuator health endpoint
            response = await client.get(f"{settings.SPRING_BOOT_URL.replace('/api/v1', '')}/actuator/health")
            if response.status_code == 200:
                spring_boot_status = "up"
                spring_boot_details = response.json()
            else:
                spring_boot_status = "down"
                spring_boot_details = {"status_code": response.status_code}
    except Exception as e:
        spring_boot_status = "down"
        spring_boot_details = {"error": str(e)}

    # Check RabbitMQ Consumer
    consumer_status = "down"
    consumer_details = {"connected": False, "active": False}
    
    if hasattr(request.app.state, "consumer"):
        consumer = request.app.state.consumer
        consumer_details["connected"] = consumer.connection_manager._connection is not None and not consumer.connection_manager._connection.is_closed
        consumer_details["active"] = consumer.is_ready
        if consumer.is_ready:
            consumer_status = "up"

    # Check LLM Keys Presence
    llm_providers = {
        "gemini": bool(settings.GEMINI_API_KEY),
        "openai": bool(settings.OPENAI_API_KEY),
        "anthropic": bool(settings.ANTHROPIC_API_KEY)
    }

    return DependencyHealthResponse(
        spring_boot=DependencyStatus(
            status=spring_boot_status,
            details=spring_boot_details
        ),
        rabbitmq=DependencyStatus(
            status=consumer_status,
            details=consumer_details
        ),
        llm_providers=llm_providers
    )
