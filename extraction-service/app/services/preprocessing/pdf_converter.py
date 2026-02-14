from typing import List
import fitz  # PyMuPDF
from PIL import Image
import io
from app.core.exceptions import PDFConversionError
from app.core.logging import logger

class PdfConverter:
    """Handles conversion of PDF pages to Pillow Images using PyMuPDF."""

    @staticmethod
    def convert_to_images(
        pdf_content: bytes, 
        dpi: int = 200, 
        max_pages: int = 10,
        page_selection: str = "first"
    ) -> List[Image.Image]:
        """
        Convert PDF bytes to a list of Pillow Images.
        """
        images = []
        try:
            doc = fitz.open(stream=pdf_content, filetype="pdf")
            
            if doc.page_count == 0:
                raise PDFConversionError("PDF has no pages")

            pages_to_process = []
            
            if page_selection == "first":
                pages_to_process = [0]
            elif page_selection == "all":
                pages_to_process = range(min(doc.page_count, max_pages))
            else:
                # Comma separated list of page numbers (1-based)
                try:
                    pages = [int(p.strip()) - 1 for p in page_selection.split(",")]
                    pages_to_process = [p for p in pages if 0 <= p < doc.page_count]
                except ValueError:
                    logger.warning("invalid_page_selection", selection=page_selection)
                    pages_to_process = [0] # Fallback to first

            zoom = dpi / 72.0
            matrix = fitz.Matrix(zoom, zoom)

            for page_num in pages_to_process:
                page = doc.load_page(page_num)
                pix = page.get_pixmap(matrix=matrix, alpha=False)
                
                # Convert to Pillow Image
                img_data = pix.tobytes("ppm")
                img = Image.open(io.BytesIO(img_data))
                images.append(img)
                
            doc.close()
            return images

        except Exception as e:
            logger.error("pdf_conversion_failed", error=str(e))
            raise PDFConversionError(f"Failed to convert PDF: {str(e)}")
