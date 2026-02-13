from fastapi import APIRouter, UploadFile, File, Form, HTTPException, status
from typing import List, Annotated
import json
import time

from app.models.responses import ErrorResponse
from app.models.preprocessing import ProcessingOptions, ProcessedImage
from app.services.preprocessing.pipeline import PreprocessingPipeline
from app.core.exceptions import PreprocessingError, UnsupportedFormatError, CorruptedFileError, FileTooLargeError
from app.core.logging import logger

router = APIRouter(prefix="/v1/preprocessing", tags=["preprocessing"])

@router.post(
    "/process",
    response_model=ProcessedImage,
    responses={
        400: {"model": ErrorResponse},
        422: {"model": ErrorResponse},
        500: {"model": ErrorResponse}
    }
)
async def process_image(
    file: UploadFile = File(...),
    options: Annotated[str, Form()] = "{}" 
):
    """
    Process a single image or PDF file.
    Options should be passed as a JSON string in the 'options' form field.
    """
    try:
        # Parse options
        try:
            options_dict = json.loads(options)
            processing_options = ProcessingOptions(**options_dict)
        except json.JSONDecodeError:
            raise HTTPException(status_code=422, detail="Invalid JSON in 'options' field")
        except Exception as e:
             raise HTTPException(status_code=422, detail=f"Invalid options: {str(e)}")

        pipeline = PreprocessingPipeline()
        content = await file.read()
        
        result = await pipeline.process(
            file_content=content,
            filename=file.filename or "unknown",
            options=processing_options
        )
        
        return result

    except HTTPException as he:
        raise he
    except (UnsupportedFormatError, CorruptedFileError, FileTooLargeError) as e:
        logger.warning("preprocessing_bad_request", error=str(e))
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error("preprocessing_internal_error", error=str(e))
        raise HTTPException(status_code=500, detail="Internal server error during image processing")

@router.post(
    "/process-batch",
    response_model=List[ProcessedImage], # Simplified response for now, ideally a BatchResponse model
    responses={
        400: {"model": ErrorResponse},
        500: {"model": ErrorResponse}
    }
)
async def process_batch(
    files: List[UploadFile] = File(...),
    options: Annotated[str, Form()] = "{}"
):
    """
    Process multiple images in batch.
    """
    # TODO: Implement full batch logic with partial success handling if needed.
    # For now, simplistic iteration.
    results = []
    
    try:
        try:
            options_dict = json.loads(options)
            processing_options = ProcessingOptions(**options_dict)
        except json.JSONDecodeError:
            raise HTTPException(status_code=422, detail="Invalid JSON in 'options' field")
            
        pipeline = PreprocessingPipeline()
        
        for file in files:
            content = await file.read()
            try:
                result = await pipeline.process(
                    file_content=content,
                    filename=file.filename or "unknown",
                    options=processing_options
                )
                results.append(result)
            except Exception as e:
                # Log error but maybe don't fail entire batch? 
                # For now we fail fast or return error object in list?
                # Using simple fail-fast for now as per "Results: list of ProcessedImage objects" return type expectation
                # To support partial failure, return type should be different.
                # Assuming all must pass or throw for MVP.
                logger.error("batch_processing_item_failed", filename=file.filename, error=str(e))
                raise HTTPException(status_code=400, detail=f"Failed to process {file.filename}: {str(e)}")
                
        return results

    except HTTPException as he:
        raise he
    except Exception as e:
        logger.error("batch_processing_error", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))
