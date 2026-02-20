# PHASE 14: IMAGE PREPROCESSING PIPELINE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 - LLM-based extraction
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-13 Completed)
- ✅ Phase 0-12: Full stack infrastructure, auth, CRUD, frontend
- ✅ Phase 13: Python FastAPI service setup with health endpoints, logging, error handling, Docker configuration

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)


---

## OBJECTIVE

Build an image preprocessing pipeline using Pillow and PyMuPDF that prepares invoice images for LLM API consumption. The pipeline handles multiple input formats (PDF, JPEG, PNG), applies quality enhancements, optimizes file size for API limits, and outputs base64-encoded images ready for multimodal LLM calls.

---

## WHY PREPROCESSING MATTERS

### LLM API Requirements
- **File Size Limits**: Most LLM APIs have limits (e.g., Gemini: 20MB per request)
- **Image Quality**: Better quality = better extraction accuracy
- **Format Compatibility**: APIs may prefer certain formats (JPEG, PNG)
- **Resolution**: Too high wastes tokens/cost, too low loses detail

### Common Invoice Image Issues
- Scanned at wrong angle (skewed)
- Poor lighting (too dark/bright)
- Low contrast (faded text)
- Wrong orientation (rotated 90°, 180°)
- Multi-page PDFs
- Oversized high-resolution scans

---

## DETAILED REQUIREMENTS

### 1. Project Structure

Add preprocessing module to the existing extraction-service:

```
extraction-service/app/
├── services/
│   ├── preprocessing/
│   │   ├── __init__.py
│   │   ├── pipeline.py           # Main preprocessing orchestrator
│   │   ├── image_loader.py       # Load images from various formats
│   │   ├── pdf_converter.py      # PDF to image conversion
│   │   ├── image_enhancer.py     # Quality enhancements
│   │   ├── image_optimizer.py    # Size/format optimization
│   │   └── base64_encoder.py     # Base64 encoding for API
│   └── ...
├── models/
│   ├── preprocessing.py          # Preprocessing DTOs
│   └── ...
└── ...
```

### 2. Supported Input Formats

The pipeline must handle these input types:

| Format | Extension | Notes |
|--------|-----------|-------|
| JPEG | .jpg, .jpeg | Most common for photos |
| PNG | .png | Lossless, good for scans |
| PDF | .pdf | May be multi-page |
| TIFF | .tif, .tiff | Sometimes used in enterprise |
| WebP | .webp | Modern web format |
| BMP | .bmp | Legacy format |

**MIME Type Detection:**
- Don't rely solely on file extension
- Use magic bytes / file signature detection
- Validate actual format matches claimed format

### 3. Pipeline Architecture

Design the pipeline as a sequence of optional steps:

```
Input File
    │
    ▼
┌─────────────────────┐
│  1. Format Detection │  ← Identify actual file type
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  2. PDF Conversion  │  ← Convert PDF pages to images (if PDF)
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  3. Orientation Fix │  ← Rotate if needed (90°, 180°, 270°)
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  4. Deskew          │  ← Fix slight rotation (1-15°)
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  5. Enhancement     │  ← Contrast, brightness, sharpness
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  6. Size Optimize   │  ← Resize if too large
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  7. Format Convert  │  ← Convert to JPEG/PNG
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  8. Base64 Encode   │  ← Prepare for API
└─────────────────────┘
    │
    ▼
Output: ProcessedImage
```

### 4. Pipeline Configuration

Make the pipeline configurable via settings:

**Processing Options:**
- `skip_preprocessing`: boolean - bypass all processing (for already optimized images)
- `auto_rotate`: boolean - attempt automatic orientation detection
- `auto_deskew`: boolean - attempt automatic skew correction
- `enhance_contrast`: boolean - apply contrast enhancement
- `enhance_sharpness`: boolean - apply sharpening
- `target_format`: string - output format (JPEG or PNG)
- `jpeg_quality`: integer - JPEG quality 1-100 (default: 85)
- `max_dimension`: integer - max width/height in pixels (default: 4096)
- `max_file_size_kb`: integer - target max size in KB (default: 5000)

**Default Presets:**
- `QUALITY_FIRST`: Minimal processing, preserve quality
- `BALANCED`: Standard processing, good balance
- `SIZE_OPTIMIZED`: Aggressive compression for large files
- `FAST`: Skip enhancement steps, just resize/convert

### 5. Image Loading Module

**Responsibilities:**
- Accept file as bytes, file path, or UploadFile
- Detect actual format using magic bytes
- Validate file is a supported image type
- Return Pillow Image object

**Magic Bytes Reference:**
- JPEG: FF D8 FF
- PNG: 89 50 4E 47
- PDF: 25 50 44 46 (%PDF)
- TIFF: 49 49 2A 00 or 4D 4D 00 2A
- WebP: 52 49 46 46 ... 57 45 42 50
- BMP: 42 4D

**Error Handling:**
- `UnsupportedFormatError`: Unknown or unsupported format
- `CorruptedFileError`: File appears damaged
- `FileTooLargeError`: Exceeds maximum allowed size

### 6. PDF Conversion Module

**Using PyMuPDF (fitz):**
- Open PDF document
- Iterate through pages
- Render each page as image at specified DPI
- Return list of Pillow Image objects

**Configuration:**
- `pdf_dpi`: rendering resolution (default: 200 DPI)
- `pdf_max_pages`: maximum pages to process (default: 10)
- `pdf_page_selection`: "all", "first", or specific page numbers

**Multi-page Handling:**
For multi-page invoices, options:
1. Process only first page (most common for single invoices)
2. Process all pages, return list
3. Stitch pages vertically into single image

Default behavior: Process first page only, log warning if multiple pages detected.

### 7. Orientation Detection and Rotation

**Automatic Orientation:**
- Check EXIF orientation tag (for photos)
- Apply rotation based on EXIF data
- Remove EXIF orientation after applying (to prevent double-rotation)

**Manual Rotation Support:**
- Accept rotation parameter: 0, 90, 180, 270 degrees
- Apply clockwise rotation

**EXIF Orientation Values:**
- 1: Normal
- 3: Rotated 180°
- 6: Rotated 90° CW
- 8: Rotated 90° CCW

### 8. Deskew (Skew Correction)

**Purpose:** Fix slightly rotated scanned documents

**Algorithm Approach (simple):**
1. Convert to grayscale
2. Apply edge detection or thresholding
3. Use Hough transform to detect dominant lines
4. Calculate average angle of lines
5. Rotate image to correct the skew

**Limitations:**
- Only attempt for small angles (< 15°)
- Skip if no clear text lines detected
- Mark as "deskew_attempted" in metadata

**Alternative:** Use a simple threshold-based approach:
- If image appears skewed based on text line analysis
- Apply small rotation correction
- This is a "best effort" enhancement, not critical

### 9. Image Enhancement Module

**Contrast Enhancement:**
- Use Pillow's ImageEnhance.Contrast
- Factor range: 0.5 (less) to 2.0 (more)
- Default: 1.2 (slight increase)

**Brightness Adjustment:**
- Use Pillow's ImageEnhance.Brightness
- Factor range: 0.5 (darker) to 2.0 (brighter)
- Default: 1.0 (no change) or auto-detect

**Sharpness Enhancement:**
- Use Pillow's ImageEnhance.Sharpness
- Factor range: 0.0 (blur) to 2.0 (sharpen)
- Default: 1.3 (slight sharpen for scanned docs)

**Auto-Enhancement Logic:**
- Analyze image histogram
- If too dark: increase brightness
- If low contrast: increase contrast
- If blurry (low high-frequency content): increase sharpness

### 10. Size Optimization Module

**Resize Logic:**
- If either dimension exceeds max_dimension, scale down proportionally
- Maintain aspect ratio
- Use high-quality resampling (LANCZOS)

**File Size Optimization:**
- Start with target quality (e.g., 85 for JPEG)
- If result exceeds max_file_size_kb:
  - Reduce quality in steps (85 → 75 → 65 → 55)
  - If still too large, reduce dimensions by 10%
  - Repeat until target size achieved or minimum quality reached

**Minimum Quality Threshold:** 50 (below this, quality loss is too severe)

### 11. Format Conversion

**Output Format Selection:**
- Default: JPEG (smaller file size)
- Use PNG if: image has transparency, or lossless required
- Convert from any input format to target format

**JPEG Settings:**
- Quality: configurable (default 85)
- Optimize: True (better compression)
- Progressive: True (for large images)

**PNG Settings:**
- Compression level: 6 (balanced)
- Remove alpha channel if not needed (reduces size)

### 12. Base64 Encoding

**Process:**
1. Save image to bytes buffer in target format
2. Encode bytes to base64 string
3. Optionally prefix with data URI scheme

**Output Options:**
- Raw base64 string
- Data URI format: `data:image/jpeg;base64,{base64_string}`

**Include MIME type** in output for API consumption.

### 13. Pipeline Response Model

The pipeline should return a structured response:

**ProcessedImage:**
- image_data: base64 encoded string
- mime_type: string (image/jpeg or image/png)
- original_filename: string
- original_format: string
- original_size_bytes: integer
- processed_size_bytes: integer
- width: integer (pixels)
- height: integer (pixels)
- processing_steps: list of applied steps
- processing_time_ms: integer
- warnings: list of any issues encountered

**ProcessingMetadata:**
- was_pdf: boolean
- pdf_page_count: integer (if PDF)
- pdf_page_processed: integer
- was_rotated: boolean
- rotation_degrees: integer
- was_deskewed: boolean
- deskew_angle: float
- was_enhanced: boolean
- was_resized: boolean
- original_dimensions: tuple (width, height)
- compression_ratio: float

### 14. API Endpoint

Add preprocessing endpoint to the extraction routes:

**POST /api/v1/preprocessing/process**

Request:
- file: uploaded image/PDF file (multipart/form-data)
- options: optional JSON with processing options

Response:
- success: boolean
- processed_image: ProcessedImage object
- metadata: ProcessingMetadata object
- errors: list (if any)

**POST /api/v1/preprocessing/process-batch**

Request:
- files: list of uploaded files
- options: processing options (applied to all)

Response:
- results: list of ProcessedImage objects
- failed: list of filenames that failed
- total_processing_time_ms: integer

### 15. Bypass Mode

For already-optimized images, support bypass mode:

**When to Bypass:**
- `skip_preprocessing` option is true
- File is already JPEG/PNG and under size limit
- Image dimensions are within acceptable range

**Bypass Behavior:**
- Still validate format
- Still encode to base64
- Skip all enhancement/optimization steps
- Mark in metadata: `processing_steps: ["bypass"]`

### 16. Error Handling

**Specific Exceptions:**
- `PreprocessingError`: Base exception for all preprocessing errors
- `UnsupportedFormatError`: File format not supported
- `CorruptedFileError`: File is corrupted or unreadable
- `FileTooLargeError`: File exceeds maximum size limit
- `PDFConversionError`: Failed to convert PDF to image
- `ImageProcessingError`: Pillow operation failed

**Graceful Degradation:**
- If enhancement fails, continue without enhancement
- If deskew fails, continue without deskew
- Log warnings but don't fail the entire pipeline
- Only fail on critical errors (can't read file, can't encode)

### 17. Logging

Log these events with appropriate levels:

**INFO:**
- Pipeline started with options
- Each processing step completed
- Pipeline completed with summary

**WARNING:**
- Step skipped due to error
- File larger than recommended
- PDF has multiple pages (only processing first)
- Deskew angle detection uncertain

**ERROR:**
- Failed to read file
- Unsupported format
- Critical processing failure

**DEBUG:**
- Detailed step parameters
- Intermediate image sizes
- Histogram analysis results

---

## TESTING REQUIREMENTS

### Test Images

Prepare a test image set in `tests/fixtures/images/`:

1. **Standard Cases:**
   - clean_invoice.jpg - Well-lit, straight, clear
   - clean_invoice.png - PNG format
   - clean_invoice.pdf - Single page PDF

2. **Rotation Cases:**
   - rotated_90.jpg - Rotated 90° clockwise
   - rotated_180.jpg - Upside down
   - rotated_270.jpg - Rotated 90° counter-clockwise

3. **Quality Issues:**
   - dark_scan.jpg - Underexposed/dark
   - low_contrast.jpg - Faded/washed out
   - skewed_scan.jpg - Slightly rotated (5-10°)
   - blurry_scan.jpg - Out of focus

4. **Size Cases:**
   - large_highres.jpg - 6000x8000 pixels
   - small_lowres.jpg - 800x600 pixels

5. **Format Cases:**
   - invoice.tiff - TIFF format
   - invoice.webp - WebP format
   - invoice.bmp - BMP format

6. **PDF Cases:**
   - single_page.pdf - One page invoice
   - multi_page.pdf - 5 page document

7. **Edge Cases:**
   - corrupted.jpg - Invalid file
   - fake_extension.pdf - JPEG with .pdf extension
   - empty.pdf - PDF with no content

### Unit Tests

**test_image_loader.py:**
- Test loading each supported format
- Test magic byte detection
- Test unsupported format rejection
- Test corrupted file handling

**test_pdf_converter.py:**
- Test single page PDF conversion
- Test multi-page PDF handling
- Test different DPI settings
- Test page selection options

**test_image_enhancer.py:**
- Test contrast enhancement
- Test brightness adjustment
- Test sharpness enhancement
- Test auto-enhancement logic

**test_image_optimizer.py:**
- Test resize maintains aspect ratio
- Test file size reduction
- Test quality iteration logic
- Test minimum quality threshold

**test_pipeline.py:**
- Test full pipeline with standard image
- Test pipeline with each preset
- Test bypass mode
- Test error handling and graceful degradation

### Performance Tests

- Process 100 images, measure average time
- Target: < 500ms per image for standard processing
- Target: < 2000ms for PDF conversion

---

## VERIFICATION CHECKLIST

### Image Loading
- [ ] JPEG loading works
- [ ] PNG loading works
- [ ] PDF loading works
- [ ] TIFF loading works
- [ ] WebP loading works
- [ ] Magic byte detection accurate
- [ ] Corrupted files rejected gracefully

### PDF Conversion
- [ ] Single page PDF converts correctly
- [ ] Multi-page PDF handles first page
- [ ] DPI setting respected
- [ ] Page selection works

### Orientation & Deskew
- [ ] EXIF rotation applied correctly
- [ ] Manual rotation parameter works
- [ ] Deskew improves skewed images
- [ ] Deskew doesn't harm straight images

### Enhancement
- [ ] Contrast enhancement works
- [ ] Brightness adjustment works
- [ ] Sharpness enhancement works
- [ ] Auto-enhancement logic reasonable

### Optimization
- [ ] Large images resized correctly
- [ ] Aspect ratio maintained
- [ ] File size reduced to target
- [ ] Quality not below minimum

### Output
- [ ] Base64 encoding correct
- [ ] MIME type accurate
- [ ] Metadata complete
- [ ] Processing time measured

### API
- [ ] POST /preprocessing/process works
- [ ] Batch endpoint works
- [ ] Options parameter respected
- [ ] Error responses proper format

### Error Handling
- [ ] Unsupported format error
- [ ] Corrupted file error
- [ ] Graceful degradation works
- [ ] Warnings logged appropriately

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_14_result.md`

Include:

### 1. Execution Status
- Overall: Success / Partial / Failed
- Date and actual time vs estimated (2 days)

### 2. Completed Tasks
Checklist with status

### 3. Files Created
List all new files in preprocessing module

### 4. Test Image Results
| Image | Original Size | Processed Size | Time (ms) | Steps Applied |
|-------|---------------|----------------|-----------|---------------|
| clean_invoice.jpg | 2.1 MB | 450 KB | 120 | resize, compress |
| ... | ... | ... | ... | ... |

### 5. Performance Metrics
- Average processing time per image type
- Compression ratios achieved
- Success rate across test set

### 6. Test Results
pytest output summary

### 7. Issues Encountered
Problems and solutions

### 8. Quality Assessment
Visual comparison of before/after for enhanced images

### 9. Next Steps
What Phase 15 (Gemini Integration) needs from preprocessing

### 10. Time Spent
Actual vs estimated

---

## DEPENDENCIES

### Requires
- **Phase 13**: FastAPI service structure, configuration, error handling

### Required By
- **Phase 15**: Gemini Integration (uses preprocessed images)
- **Phase 16**: GPT Fallback (uses preprocessed images)
- **Phase 17**: Claude Fallback (uses preprocessed images)
- **Phase 20**: File Upload UI (triggers preprocessing)

---

## SUCCESS CRITERIA

1. ✅ All supported formats load correctly
2. ✅ PDF to image conversion works
3. ✅ Orientation correction works
4. ✅ Enhancement improves image quality
5. ✅ Size optimization meets targets
6. ✅ Base64 output ready for LLM APIs
7. ✅ Bypass mode works for optimized images
8. ✅ Error handling graceful
9. ✅ Processing time < 500ms average
10. ✅ All tests pass
11. ✅ API endpoints functional
12. ✅ Result file created

---

## IMPORTANT NOTES

1. **No LLM Calls**: This phase only preprocesses images. LLM integration is Phase 15.

2. **Pillow, Not OpenCV**: Use Pillow for simplicity. OpenCV is overkill for these operations.

3. **PyMuPDF for PDFs**: Use PyMuPDF (fitz) for PDF conversion. It's faster and more reliable than pdf2image.

4. **Quality vs Size**: Default to quality preservation. Only compress aggressively when needed.

5. **Deskew is Optional**: Deskew is a "nice to have" enhancement. Don't fail if it can't detect angle.

6. **Test with Real Invoices**: Use actual Turkish invoice images for testing, not just synthetic test images.

7. **Memory Management**: Large images can consume significant memory. Process one at a time, release resources promptly.

8. **Turkish Text**: Ensure enhancement doesn't damage Turkish characters (Ç, Ğ, İ, Ö, Ş, Ü).

---

**Phase 14 Completion Target**: A robust image preprocessing pipeline that accepts various invoice image formats, applies quality enhancements, optimizes for LLM API consumption, and outputs base64-encoded images ready for multimodal extraction in Phase 15.
