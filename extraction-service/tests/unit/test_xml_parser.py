import pytest
from app.services.xml_parser_service import XMLParserService
from app.models.response import InvoiceData

class TestXMLParserService:
    
    def test_parse_valid_xml(self, sample_xml_content):
        """Test parsing of a valid UBL-TR XML."""
        result = XMLParserService.parse(sample_xml_content)
        
        assert isinstance(result, InvoiceData)
        assert result.fatura_no == "GIB2024000000001"
        assert result.genel_toplam == 1200.00
        assert result.para_birimi == "TRY"
        assert len(result.kalemler) == 1
        assert result.kalemler[0].aciklama == "Teslimat Hizmeti"

    def test_parse_invalid_xml(self):
        """Test parsing of invalid XML string."""
        invalid_xml = "<Invoice>Invalid Content" # Missing closing tag etc
        
        with pytest.raises(Exception): # lxml or ElementTree error
            XMLParserService.parse(invalid_xml)

    def test_parse_empty_xml(self):
        """Test parsing of empty string."""
        with pytest.raises(Exception):
            XMLParserService.parse("")

    def test_parse_wrong_schema(self):
        """Test XML that is valid XML but not an Invoice."""
        wrong_xml = "<OtherDoc>Content</OtherDoc>"
        
        # Depending on implementation, might raise error or return empty InvoiceData
        # but usually should raise validation error if root is not Invoice
        with pytest.raises(Exception):
             XMLParserService.parse(wrong_xml)
