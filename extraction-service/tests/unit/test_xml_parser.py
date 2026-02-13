import pytest
import os
from app.services.parsers.xml_parser import XMLParser
from app.core.exceptions import NotEInvoiceError, MissingRequiredFieldError, XMLParseError

FIXTURE_DIR = os.path.join(os.path.dirname(__file__), "..", "fixtures", "xml")


def _read_fixture(filename):
    with open(os.path.join(FIXTURE_DIR, filename), "rb") as f:
        return f.read()


class TestXMLParser:
    def setup_method(self):
        self.parser = XMLParser()

    # --- Valid Invoice Parsing ---
    def test_parse_valid_invoice(self):
        content = _read_fixture("valid_einvoice.xml")
        result = self.parser.parse(content)

        assert result.invoice_number == "GIB202300000001"
        assert result.invoice_date == "2023-10-27"
        assert result.due_date == "2023-11-26"
        assert result.supplier_name == "Test Tedarikçi A.Ş."
        assert result.supplier_tax_number == "1234567890"
        assert "Merkez Mah." in result.supplier_address
        assert result.buyer_name == "Alıcı Firma Ltd. Şti."
        assert result.total_amount == 1180.00
        assert result.currency == "TRY"
        assert result.e_invoice_uuid == "f47ac10b-58cc-4372-a567-0e02b2c3d479"

        assert len(result.items) == 1
        item = result.items[0]
        assert item.description == "Örnek Ürün"
        assert item.quantity == 10.0
        assert item.unit in ["Adet", "C62"]
        assert item.unit_price == 100.00
        assert item.tax_rate == 18.0

    def test_parse_multi_item_invoice(self):
        content = _read_fixture("valid_einvoice_standard.xml")
        result = self.parser.parse(content)

        assert result.invoice_number == "GIB202400000055"
        assert len(result.items) == 3
        assert result.items[0].description == "Dizüstü Bilgisayar Kılıfı"
        assert result.items[1].description == "USB-C Şarj Kablosu (2m)"
        assert result.items[2].description == "Termal Macun (Endüstriyel)"
        assert result.subtotal == 8500.00
        assert result.total_amount == 10030.00

    def test_parse_multi_tax_rates(self):
        content = _read_fixture("valid_einvoice_multi_tax.xml")
        result = self.parser.parse(content)

        assert len(result.items) == 3
        rates = [item.tax_rate for item in result.items]
        assert 1.0 in rates
        assert 10.0 in rates
        assert 20.0 in rates

    def test_parse_no_due_date(self):
        content = _read_fixture("valid_einvoice_no_due_date.xml")
        result = self.parser.parse(content)

        assert result.due_date is None
        assert result.invoice_number is not None

    def test_parse_usd_currency(self):
        content = _read_fixture("valid_einvoice_usd.xml")
        result = self.parser.parse(content)

        assert result.currency == "USD"
        assert result.total_amount == 1180.00

    def test_parse_iade_invoice_type(self):
        content = _read_fixture("valid_einvoice_iade.xml")
        result = self.parser.parse(content)

        assert result.invoice_type_code == "IADE"
        assert result.invoice_number == "GIB202400000111"

    # --- Turkish Characters ---
    def test_turkish_characters_preserved(self):
        content = _read_fixture("valid_einvoice.xml")
        result = self.parser.parse(content)

        assert "Tedarikçi" in result.supplier_name
        assert "Ş" in result.supplier_name
        assert "Alıcı" in result.buyer_name
        assert "Şti" in result.buyer_name

    # --- E-Invoice Specific Fields ---
    def test_e_invoice_uuid_extracted(self):
        content = _read_fixture("valid_einvoice.xml")
        result = self.parser.parse(content)

        assert result.e_invoice_uuid is not None
        assert result.e_invoice_ettn is not None
        assert result.e_invoice_uuid == result.e_invoice_ettn

    def test_profile_id_extracted(self):
        content = _read_fixture("valid_einvoice.xml")
        result = self.parser.parse(content)

        assert result.profile_id == "TICARIFATURA"

    # --- Unit Code Translation ---
    def test_unit_code_c62_translated(self):
        content = _read_fixture("valid_einvoice.xml")
        result = self.parser.parse(content)
        assert result.items[0].unit == "Adet"

    def test_unit_code_kgm_translated(self):
        content = _read_fixture("valid_einvoice_standard.xml")
        result = self.parser.parse(content)
        assert result.items[2].unit == "Kg"

    def test_unit_code_tne_translated(self):
        content = _read_fixture("valid_einvoice_usd.xml")
        result = self.parser.parse(content)
        assert result.items[0].unit == "Ton"

    # --- Multiple Notes ---
    def test_multiple_notes_concatenated(self):
        content = _read_fixture("valid_einvoice_standard.xml")
        result = self.parser.parse(content)

        assert result.notes is not None
        assert "\n" in result.notes
        assert "3 kalem" in result.notes

    # --- Namespace Handling ---
    def test_namespace_handling_with_standard_prefixes(self):
        """XML with standard cac/cbc prefixes should parse correctly."""
        content = _read_fixture("valid_einvoice.xml")
        result = self.parser.parse(content)
        assert result.invoice_number is not None
        assert result.supplier_name is not None

    # --- Error Cases ---
    def test_parse_invalid_xml(self):
        with pytest.raises(XMLParseError):
            self.parser.parse(b"<Invoice>Unclosed tag")

    def test_parse_malformed_xml_fixture(self):
        content = _read_fixture("malformed.xml")
        with pytest.raises(XMLParseError):
            self.parser.parse(content)

    def test_parse_not_invoice(self):
        with pytest.raises(NotEInvoiceError):
            self.parser.parse(b"<Order>...</Order>")

    def test_parse_not_einvoice_fixture(self):
        content = _read_fixture("not_einvoice.xml")
        with pytest.raises(NotEInvoiceError):
            self.parser.parse(content)

    def test_missing_required_field(self):
        content = _read_fixture("missing_required.xml")
        with pytest.raises(MissingRequiredFieldError):
            self.parser.parse(content)

    def test_missing_required_field_inline(self):
        xml = b"""<?xml version="1.0" encoding="UTF-8"?>
<Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
    <cbc:IssueDate>2023-10-27</cbc:IssueDate>
</Invoice>"""
        with pytest.raises(MissingRequiredFieldError):
            self.parser.parse(xml)

    def test_empty_content_raises(self):
        with pytest.raises(XMLParseError):
            self.parser.parse(b"")

    # --- Edge Cases ---
    def test_xml_with_bom(self):
        """XML with UTF-8 BOM should still parse correctly."""
        bom = b'\xef\xbb\xbf'
        content = _read_fixture("valid_einvoice.xml")
        result = self.parser.parse(bom + content)
        assert result.invoice_number == "GIB202300000001"

    def test_xml_with_whitespace_prefix(self):
        """XML content without declaration but with leading whitespace should parse."""
        # Note: whitespace before <?xml declaration is invalid per XML spec.
        # Test with XML that has no declaration but starts with whitespace + root element.
        xml = b"""  <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
            xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
            xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
            <cbc:ID>WSTEST001</cbc:ID>
            <cbc:IssueDate>2024-01-01</cbc:IssueDate>
            <cbc:DocumentCurrencyCode>TRY</cbc:DocumentCurrencyCode>
            <cac:AccountingSupplierParty><cac:Party><cac:PartyName><cbc:Name>Test</cbc:Name></cac:PartyName></cac:Party></cac:AccountingSupplierParty>
            <cac:LegalMonetaryTotal><cbc:LineExtensionAmount currencyID="TRY">0</cbc:LineExtensionAmount><cbc:TaxInclusiveAmount currencyID="TRY">0</cbc:TaxInclusiveAmount></cac:LegalMonetaryTotal>
            <cac:TaxTotal><cbc:TaxAmount currencyID="TRY">0</cbc:TaxAmount></cac:TaxTotal>
        </Invoice>"""
        result = self.parser.parse(xml)
        assert result.invoice_number == "WSTEST001"
