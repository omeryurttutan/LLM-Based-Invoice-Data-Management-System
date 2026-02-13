import pytest
from app.services.preprocessing.pipeline import PreprocessingPipeline
from app.models.preprocessing import ProcessingOptions, ProcessedImage

@pytest.mark.asyncio
async def test_pipeline_jpeg(test_image_bytes):
    pipeline = PreprocessingPipeline()
    result = await pipeline.process(test_image_bytes, "test.jpg")
    
    assert isinstance(result, ProcessedImage)
    assert result.mime_type == "image/jpeg"
    assert result.metadata.was_pdf is False
    assert len(result.image_data) > 0

@pytest.mark.asyncio
async def test_pipeline_pdf(test_pdf_bytes):
    pipeline = PreprocessingPipeline()
    result = await pipeline.process(test_pdf_bytes, "test.pdf")
    
    assert result.metadata.was_pdf is True
    assert result.metadata.pdf_page_count == 1
    assert result.width > 0

@pytest.mark.asyncio
async def test_pipeline_bypass(test_image_bytes):
    pipeline = PreprocessingPipeline()
    options = ProcessingOptions(skip_preprocessing=True)
    result = await pipeline.process(test_image_bytes, "test.jpg", options)
    
    assert "bypass" in result.metadata.processing_steps
    assert result.metadata.was_enhanced is False

@pytest.mark.asyncio
async def test_pipeline_resize(test_image_bytes):
    # Default max dim is 4096, test image is small (100x100)
    # Set max dim small to force resize
    pipeline = PreprocessingPipeline()
    options = ProcessingOptions(max_dimension=50)
    result = await pipeline.process(test_image_bytes, "test.jpg", options)
    
    assert result.metadata.was_resized is True
    assert result.width == 50
