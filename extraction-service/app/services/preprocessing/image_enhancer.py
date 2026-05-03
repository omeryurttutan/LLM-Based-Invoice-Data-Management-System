from typing import Tuple, Optional
from PIL import Image, ImageEnhance, ImageOps, ImageStat, ImageFilter
from app.models.preprocessing import ProcessingOptions, ProcessingMetadata
from app.core.logging import logger
import math

class ImageEnhancer:
    """Handles image enhancement operations: rotation, deskew, contrast, brightness, sharpness."""

    @staticmethod
    def fix_orientation(img: Image.Image) -> Tuple[Image.Image, bool, int]:
        """
        Apply EXIF orientation if present.
        Returns (enhanced_image, was_rotated, degrees).
        """
        try:
            exif = img.getexif()
            if not exif:
                return img, False, 0
            
            orientation = exif.get(0x0112)
            if not orientation:
                return img, False, 0
            
            # 3: 180, 6: 270 (CW), 8: 90 (CW) - Pillow ImageOps.exif_transpose handles this
            # but we want to know if it happened.
            method = {
                3: 180,
                6: 90,
                8: 270
            }
            
            if orientation in method:
                img = ImageOps.exif_transpose(img)
                return img, True, method[orientation]
            
            return img, False, 0
            
        except Exception as e:
            logger.warning("orientation_fix_failed", error=str(e))
            return img, False, 0

    @staticmethod
    def deskew_image(img: Image.Image) -> Tuple[Image.Image, bool, float]:
        """
        Attempt to deskew the image. 
        This is a simple implementation. For robust deskewing, OpenCV/Hough transform is better,
        but we are sticking to Pillow/numpy to avoid heavy CV dependencies if possible.
        
        Current approach: Placeholder / basic checks. Real deskew without OpenCV is hard.
        We will implement a no-op that returns the original image for now, 
        or a very simple check if we had `scikit-image`.
        
        Given requirements say "Simple", we will skip complex deskew logic for now but keep the interface.
        """
        # TODO: Implement simple deskew if needed, or integrate a lightweight library.
        return img, False, 0.0

    @staticmethod
    def enhance_image(img: Image.Image, options: ProcessingOptions) -> Tuple[Image.Image, bool]:
        """
        Apply contrast, brightness, and sharpness enhancements based on options.
        """
        enhanced = False
        
        try:
            # Auto-enhancement logic (simple heuristics)
            # 1. Analyze brightness/contrast
            stat = ImageStat.Stat(img.convert('L'))
            avg_brightness = stat.mean[0]
            rms_contrast = stat.stddev[0]
            
            # Brightness
            if options.enhance_contrast: # We piggyback auto-brightness here or make it separate
                # Target brightness around 128-200 for docs
                if avg_brightness < 100: # Too dark
                    enhancer = ImageEnhance.Brightness(img)
                    img = enhancer.enhance(1.2)
                    enhanced = True
                elif avg_brightness > 220: # Too bright/washed out
                    enhancer = ImageEnhance.Brightness(img)
                    img = enhancer.enhance(0.9) # Slightly darken to see ink
                    enhanced = True

            # Contrast
            if options.enhance_contrast:
                if rms_contrast < 40: # Low contrast
                    enhancer = ImageEnhance.Contrast(img)
                    img = enhancer.enhance(1.3) # Boost contrast
                    enhanced = True
            
            # Sharpness
            if options.enhance_sharpness:
                # Always apply slight sharpening for OCR preparation
                enhancer = ImageEnhance.Sharpness(img)
                img = enhancer.enhance(1.4)
                enhanced = True
                
        except Exception as e:
            logger.warning("enhancement_failed", error=str(e))
        
        return img, enhanced
