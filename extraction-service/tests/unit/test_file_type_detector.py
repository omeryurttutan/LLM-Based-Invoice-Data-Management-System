import pytest
from app.services.parsers.file_type_detector import FileTypeDetector

class TestFileTypeDetector:
    
    def test_detect_xml_by_byte_marker(self):
        content = b'<?xml version="1.0"?><Invoice>...</Invoice>'
        assert FileTypeDetector.detect("invoice.xml", content) == "XML"
        
        content_simple = b'<Invoice>...</Invoice>'
        assert FileTypeDetector.detect("invoice.xml", content_simple) == "XML"

    def test_detect_xml_by_content_type(self):
        content = b'<Invoice>...</Invoice>'
        assert FileTypeDetector.detect("invoice", content, content_type="application/xml") == "XML"
        assert FileTypeDetector.detect("invoice", content, content_type="text/xml") == "XML"

    def test_detect_xml_by_extension(self):
        # Even if bytes are not strictly starting with < (maybe whitespace), extension helps
        content = b'  <Invoice>...</Invoice>'
        assert FileTypeDetector.detect("invoice.xml", content) == "XML"

    def test_detect_image_by_magic_bytes(self):
        # PDF magic bytes
        assert FileTypeDetector.detect("file.pdf", b'%PDF-1.4...') == "IMAGE"
        # JPEG magic bytes
        assert FileTypeDetector.detect("file.jpg", b'\xff\xd8\xff...') == "IMAGE"
        # PNG magic bytes
        assert FileTypeDetector.detect("file.png", b'\x89PNG\r\n\x1a\n...') == "IMAGE"

    def test_detect_image_by_extension(self):
        assert FileTypeDetector.detect("image.jpg", b'somebytes') == "IMAGE"
        assert FileTypeDetector.detect("image.png", b'somebytes') == "IMAGE"

    def test_unknown_type(self):
        with pytest.raises(ValueError):
            FileTypeDetector.detect("unknown.xyz", b'randombytes')
