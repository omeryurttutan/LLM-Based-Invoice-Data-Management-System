import time

from fastapi import APIRouter, UploadFile, File, Form, HTTPException, Depends, status, Body
from typing import List, Optional
from pydantic import BaseModel

from app.config.settings import settings
from app.services.extraction.extraction_service import ExtractionService
from app.models.extraction import ExtractionResponse
from app.models.invoice_data import InvoiceData
from app.models.validation import ValidationResult
from app.core.logging import logger
from app.core.exceptions import ExtractionServiceException
from app.services.llm.base_provider import LLMError, LLMTimeoutError, LLMRateLimitError

router = APIRouter(tags=["Extraction"])
extraction_service = ExtractionService()

def get_extraction_service():
    return extraction_service

class Base64ExtractionRequest(BaseModel):
    image_data: str
    mime_type: Optional[str] = "image/jpeg"

@router.post("/parse/xml", response_model=ExtractionResponse)
async def parse_xml(
    file: UploadFile = File(...),
    service: ExtractionService = Depends(get_extraction_service)
):
    """
    Parse an e-Invoice XML file directly.
    Rejects non-XML files.
    """
    return await service.extract_xml(file)

@router.get("/parse/xml/supported-types")
async def get_supported_xml_types():
    return {
        "types": ["SATIS", "IADE", "TEVKIFAT", "ISTISNA", "OZELMATRAH", "IHRACKAYITLI"],
        "formats": ["UBL-TR 2.1"]
    }

@router.post("/extract", response_model=ExtractionResponse)
async def extract_invoice(
    file: UploadFile = File(...),
    provider: Optional[str] = Form(None)
):
    """
    Extract data from a single invoice file (PDF/Image).
    Uses the fallback chain: Gemini 3 Flash → Gemini 2.5 Flash → GPT-5 nano.
    """
    try:
        result = await extraction_service.extract_from_file(file)
        return result

    except LLMTimeoutError as e:
        raise HTTPException(status_code=408, detail=str(e))
    except LLMRateLimitError as e:
        raise HTTPException(status_code=429, detail=str(e))
    except LLMError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except ExtractionServiceException:
        raise
    except Exception as e:
        logger.error("extraction_failed", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/extract/base64", response_model=ExtractionResponse)
async def extract_invoice_base64(
    request: Base64ExtractionRequest = Body(...)
):
    """
    Extract data from base64 encoded image.
    """
    try:
        result = await extraction_service.extract_from_base64(request.image_data)
        return result
    except LLMTimeoutError as e:
        raise HTTPException(status_code=408, detail=str(e))
    except LLMRateLimitError as e:
        raise HTTPException(status_code=429, detail=str(e))
    except LLMError as e:
        raise HTTPException(status_code=503, detail=str(e))
    except Exception as e:
        logger.error("extraction_failed", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/validate", response_model=ValidationResult)
async def validate_invoice(
    invoice_data: InvoiceData = Body(...)
):
    """
    Validate an InvoiceData object directly (without LLM extraction).
    Useful for re-validating data edited by the user.
    """
    from app.services.validation.validator import Validator
    validator = Validator()
    return validator.validate(invoice_data)

@router.get("/validation/config")
async def get_validation_config():
    """
    Get current validation configuration (thresholds, weights).
    """
    from app.config.validation_config import validation_settings
    return validation_settings.model_dump()

@router.get("/extract/prompt-info")
async def get_prompt_info():
    """
    Get current prompt version, metadata, and text.
    """
    from app.services.llm.prompt_manager import PromptManager
    pm = PromptManager()
    info = pm.get_prompt_info()
    info["prompt_text"] = pm.get_prompt(info["version"])
    info["system_instruction"] = pm.get_system_instruction(info["version"])
    return info

@router.get("/extract/pipeline-status")
async def get_pipeline_status():
    """
    Get health status of the extraction pipeline including LLM availability.
    """
    from app.services.llm.provider_health import ProviderHealthManager
    from app.services.llm.fallback_chain import FallbackChain
    
    chain = FallbackChain()
    health_manager = ProviderHealthManager()
    
    provider_statuses = {}
    for name, provider in chain.providers.items():
        health = health_manager.get_health(name)
        provider_statuses[name] = {
            "available": provider.is_available(),
            "health_status": health.status.value,
            "total_successes": health.total_successes,
            "total_failures": health.total_failures,
        }
    
    all_available = any(p.is_available() for p in chain.providers.values())
    
    return {
        "status": "READY" if all_available else "UNAVAILABLE",
        "providers": provider_statuses,
        "chain_order": chain.chain_order,
        "chain_enabled": settings.LLM_CHAIN_ENABLED,
    }

@router.get("/providers")
async def list_providers():
    """
    List configured LLM providers, their status, and chain order.
    """
    from app.services.llm.fallback_chain import FallbackChain
    from app.services.llm.provider_health import ProviderHealthManager

    chain = FallbackChain()
    health_manager = ProviderHealthManager()

    provider_details = []
    for name in chain.chain_order:
        provider = chain.providers.get(name)
        health = health_manager.get_health(name)
        provider_details.append({
            "name": name,
            "available": provider.is_available() if provider else False,
            "health_status": health.status.value,
        })

    return {
        "chain_order": chain.chain_order,
        "chain_enabled": settings.LLM_CHAIN_ENABLED,
        "providers": provider_details,
    }


@router.get("/providers/health")
async def get_providers_health():
    """
    Get detailed health metrics per provider (sliding window stats).
    """
    from app.services.llm.provider_health import ProviderHealthManager
    health_manager = ProviderHealthManager()

    providers = settings.LLM_CHAIN_ORDER.split(",")
    health_details = {}

    for p in providers:
        p_name = p.strip().upper()
        health_details[p_name] = health_manager.get_window_stats(p_name)

    return {
        "providers": health_details,
        "timestamp": time.time()
    }


@router.post("/providers/{provider_name}/test")
async def test_provider(provider_name: str):
    """
    Test a specific provider with a dummy request.
    """
    from app.services.llm.fallback_chain import FallbackChain
    chain = FallbackChain()

    provider_name = provider_name.upper()
    if provider_name not in chain.providers:
        raise HTTPException(status_code=404, detail=f"Provider {provider_name} not found or not configured.")

    provider = chain.providers[provider_name]

    try:
        import base64
        # Small 1x1 pixel PNG
        dummy_image = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAAAAAA6fptVAAAACklEQVR4nGNiAAAABgADNjd8qAAAAABJRU5ErkJggg==")
        response_text, usage = await provider.generate(dummy_image, "Test. Respond 'OK'.")
        return {"provider": provider_name, "status": "SUCCESS", "response": response_text}
    except Exception as e:
        return {"provider": provider_name, "status": "FAILED", "error": str(e)}
