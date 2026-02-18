import time
from typing import List, Union, BinaryIO
from fastapi import UploadFile
from PIL import Image

from app.models.preprocessing import (
    ProcessingOptions, 
    ProcessedImage, 
    ProcessingMetadata,
    ProcessingPreset
)
from app.services.preprocessing.image_loader import ImageLoader
from app.services.preprocessing.pdf_converter import PdfConverter
from app.services.preprocessing.image_enhancer import ImageEnhancer
from app.services.preprocessing.image_optimizer import ImageOptimizer
from app.services.preprocessing.base64_encoder import Base64Encoder
from app.core.logging import logger
from app.core.exceptions import PreprocessingError, FileTooLargeError

class PreprocessingPipeline:
    """
    Orchestrates the image preprocessing workflow.
    """

    def __init__(self):
        pass

    async def process(
        self, 
        file_content: bytes, 
        filename: str, 
        options: ProcessingOptions = ProcessingOptions()
    ) -> ProcessedImage:
        """
        Main entry point for processing a single file (image or PDF).
        """
        start_time = time.time()
        metadata = ProcessingMetadata()
        warnings = []
        steps = []
        
        try:
            # 1. Load Image / Convert PDF
            # Check format first
            detected_format = ImageLoader.detect_format(file_content)
            original_size = len(file_content)
            
            # Check size limit (rough check before loading)
            if original_size > 20 * 1024 * 1024: # 20MB hard limit for input
                 raise FileTooLargeError("Input file exceeds 20MB limit")

            images_to_process: List[Image.Image] = []
            
            if detected_format == "application/pdf":
                metadata.was_pdf = True
                steps.append("pdf_conversion")
                logger.info("processing_pdf", filename=filename)
                
                pdf_images = PdfConverter.convert_to_images(
                    file_content, 
                    dpi=options.pdf_dpi,
                    max_pages=options.pdf_max_pages,
                    page_selection=options.pdf_page_selection
                )
                metadata.pdf_page_count = len(pdf_images) # approximate or passed from converter if modified
                
                if not pdf_images:
                     raise PreprocessingError("No images extracted from PDF")
                
                # Default behavior: take first page only for now as per requirements
                # "Default behavior: Process first page only, log warning if multiple pages detected."
                if len(pdf_images) > 1:
                    warnings.append(f"PDF had {len(pdf_images)} pages, only first page processed")
                
                images_to_process = [pdf_images[0]]
                metadata.pdf_page_processed = 1
                
            else:
                img, fmt = ImageLoader.load_image(file_content)
                metadata.was_pdf = False
                images_to_process = [img]
                detected_format = fmt # Update if ImageLoader refined it

            # Process the single selected image
            img = images_to_process[0]
            metadata.original_dimensions = img.size
            if detected_format:
                 img.format = detected_format.split('/')[-1].upper() if '/' in detected_format else detected_format

            # Bypass check
            if options.skip_preprocessing:
                steps.append("bypass")
                # Just optimize/convert to target format without enhancement
                # But actually "bypass" implies we trust the input. 
                # However, requirements say: "Still encode to base64", "Validate format"
                # So we just encode effectively if it matches target? 
                # For simplicity, we skip enhancement/resize but still do optimize step to ensure format.
            else:
                # 2. Orientation Fix
                if options.auto_rotate:
                    img, rotated, degrees = ImageEnhancer.fix_orientation(img)
                    if rotated:
                        metadata.was_rotated = True
                        metadata.rotation_degrees = degrees
                        steps.append("auto_rotate")

                # 3. Deskew
                if options.auto_deskew:
                    img, deskewed, angle = ImageEnhancer.deskew_image(img)
                    if deskewed:
                        metadata.was_deskewed = True
                        metadata.deskew_angle = angle
                        steps.append("deskew")

                # 4. Enhancement
                if options.enhance_contrast or options.enhance_sharpness:
                    img, enhanced = ImageEnhancer.enhance_image(img, options)
                    if enhanced:
                        metadata.was_enhanced = True
                        steps.append("enhancement")

            # 5. Resize
            step_start = time.time()
            img, resized = ImageOptimizer.resize_image(img, options.max_dimension)
            if resized:
                 metadata.was_resized = True
                 steps.append("resize")
            logger.debug("step_completed", step="resize", duration_ms=int((time.time() - step_start) * 1000))

            metadata.final_dimensions = img.size

            # 6. Optimize & Convert Format
            step_start = time.time()
            final_bytes, compression_ratio = ImageOptimizer.optimize_image(
                img, 
                target_format=options.target_format,
                max_size_kb=options.max_file_size_kb,
                initial_quality=options.jpeg_quality
            )
            metadata.compression_ratio = compression_ratio
            steps.append("optimize")
            logger.debug("step_completed", step="optimize", duration_ms=int((time.time() - step_start) * 1000))

            # 7. Base64 Encode
            step_start = time.time()
            b64_string = Base64Encoder.encode(final_bytes)
            logger.debug("step_completed", step="base64", duration_ms=int((time.time() - step_start) * 1000))
            
            processing_time = int((time.time() - start_time) * 1000)
            metadata.processing_steps = steps

            return ProcessedImage(
                image_data=b64_string,
                mime_type=options.target_format, # We forced this in optimizer
                original_filename=filename,
                original_format=detected_format or "unknown",
                original_size_bytes=original_size,
                processed_size_bytes=len(final_bytes),
                width=img.width,
                height=img.height,
                metadata=metadata,
                warnings=warnings,
                processing_time_ms=processing_time
            )

        except Exception as e:
            logger.error("pipeline_processing_error", filename=filename, error=str(e))
            raise e
