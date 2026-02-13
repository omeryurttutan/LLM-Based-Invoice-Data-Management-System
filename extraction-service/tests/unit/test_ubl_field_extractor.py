import pytest
import os
from lxml import etree
from app.services.parsers.ubl_field_extractor import UBLFieldExtractor
from app.core.exceptions import MissingRequiredFieldError


FIXTURE_DIR = os.path.join(os.path.dirname(__file__), "..", "fixtures", "xml")


def _load_xml(filename):
    path = os.path.join(FIXTURE_DIR, filename)
    with open(path, "rb") as f:
        return etree.fromstring(f.read())


class TestUBLFieldExtractor:
    """Unit tests for individual field extraction from UBL-TR XML."""

    def setup_method(self):
        self.root = _load_xml("valid_einvoice.xml")
        self.extractor = UBLFieldExtractor(self.root)

    # --- Invoice Number ---
    def test_extract_invoice_number(self):
        assert self.extractor.extract_invoice_number() == "GIB202300000001"

    def test_extract_invoice_number_missing_raises(self):
        root = _load_xml("missing_required.xml")
        ext = UBLFieldExtractor(root)
        with pytest.raises(MissingRequiredFieldError):
            ext.extract_invoice_number()

    # --- Invoice Date ---
    def test_extract_invoice_date(self):
        assert self.extractor.extract_invoice_date() == "2023-10-27"

    def test_invoice_date_format_is_iso(self):
        date = self.extractor.extract_invoice_date()
        # YYYY-MM-DD format check
        parts = date.split("-")
        assert len(parts) == 3
        assert len(parts[0]) == 4
        assert len(parts[1]) == 2
        assert len(parts[2]) == 2

    # --- Due Date ---
    def test_extract_due_date_from_payment_terms(self):
        # valid_einvoice.xml has PaymentTerms/PaymentDueDate
        assert self.extractor.extract_due_date() == "2023-11-26"

    def test_extract_due_date_from_payment_means(self):
        root = _load_xml("valid_einvoice_usd.xml")
        ext = UBLFieldExtractor(root)
        assert ext.extract_due_date() == "2024-08-19"

    def test_extract_due_date_missing_returns_none(self):
        root = _load_xml("valid_einvoice_no_due_date.xml")
        ext = UBLFieldExtractor(root)
        assert ext.extract_due_date() is None

    # --- Supplier ---
    def test_extract_supplier_name(self):
        supplier = self.extractor.extract_supplier()
        assert supplier["name"] == "Test Tedarikçi A.Ş."

    def test_extract_supplier_tax_number(self):
        supplier = self.extractor.extract_supplier()
        assert supplier["tax_number"] == "1234567890"

    def test_extract_supplier_address(self):
        supplier = self.extractor.extract_supplier()
        assert supplier["address"] is not None
        assert "Merkez Mah." in supplier["address"]
        assert "Ankara" in supplier["address"]

    def test_extract_supplier_turkish_chars_preserved(self):
        supplier = self.extractor.extract_supplier()
        assert "Tedarikçi" in supplier["name"]
        assert "Ş" in supplier["name"]

    # --- Buyer ---
    def test_extract_buyer_name(self):
        buyer = self.extractor.extract_buyer()
        assert buyer["name"] == "Alıcı Firma Ltd. Şti."

    def test_extract_buyer_tax_number(self):
        buyer = self.extractor.extract_buyer()
        assert buyer["tax_number"] == "9876543210"

    def test_extract_buyer_tckn(self):
        root = _load_xml("valid_einvoice_no_due_date.xml")
        ext = UBLFieldExtractor(root)
        buyer = ext.extract_buyer()
        assert buyer["tax_number"] == "12345678901"

    # --- Totals ---
    def test_extract_totals(self):
        totals = self.extractor.extract_totals()
        assert totals["subtotal"] == 1000.00
        assert totals["tax_amount"] == 180.00
        assert totals["total_amount"] == 1180.00

    def test_extract_totals_amounts_as_float(self):
        totals = self.extractor.extract_totals()
        assert isinstance(totals["subtotal"], float)
        assert isinstance(totals["tax_amount"], float)
        assert isinstance(totals["total_amount"], float)

    # --- Currency ---
    def test_extract_currency_try(self):
        assert self.extractor.extract_currency() == "TRY"

    def test_extract_currency_usd(self):
        root = _load_xml("valid_einvoice_usd.xml")
        ext = UBLFieldExtractor(root)
        assert ext.extract_currency() == "USD"

    # --- Notes ---
    def test_extract_notes_single(self):
        root = _load_xml("valid_einvoice_iade.xml")
        ext = UBLFieldExtractor(root)
        notes = ext.extract_notes()
        assert notes is not None
        assert "İade" in notes

    def test_extract_notes_multiple_concatenated(self):
        root = _load_xml("valid_einvoice_standard.xml")
        ext = UBLFieldExtractor(root)
        notes = ext.extract_notes()
        assert notes is not None
        assert "3 kalem" in notes
        assert "Teslimat" in notes
        # Multiple notes should be newline-separated
        assert "\n" in notes

    def test_extract_notes_none_when_absent(self):
        # valid_einvoice.xml may not have a note at root level
        # Use no_due_date fixture which lacks notes
        root = _load_xml("valid_einvoice_no_due_date.xml")
        ext = UBLFieldExtractor(root)
        notes = ext.extract_notes()
        assert notes is None

    # --- Items ---
    def test_extract_single_item(self):
        items = self.extractor.extract_items()
        assert len(items) == 1

    def test_extract_multi_items(self):
        root = _load_xml("valid_einvoice_standard.xml")
        ext = UBLFieldExtractor(root)
        items = ext.extract_items()
        assert len(items) == 3

    def test_item_fields_complete(self):
        items = self.extractor.extract_items()
        item = items[0]
        assert item["description"] == "Örnek Ürün"
        assert item["quantity"] == 10.0
        assert item["unit_price"] == 100.00
        assert item["tax_rate"] == 18.0
        assert item["line_total"] == 1000.00

    def test_item_unit_code_translation_c62(self):
        items = self.extractor.extract_items()
        assert items[0]["unit"] == "Adet"

    def test_item_unit_code_translation_kgm(self):
        root = _load_xml("valid_einvoice_standard.xml")
        ext = UBLFieldExtractor(root)
        items = ext.extract_items()
        # Third item uses KGM
        assert items[2]["unit"] == "Kg"

    def test_item_unit_code_translation_ltr(self):
        root = _load_xml("valid_einvoice_multi_tax.xml")
        ext = UBLFieldExtractor(root)
        items = ext.extract_items()
        # Second item uses LTR
        assert items[1]["unit"] == "Lt"

    def test_item_unit_code_translation_hour(self):
        root = _load_xml("valid_einvoice_multi_tax.xml")
        ext = UBLFieldExtractor(root)
        items = ext.extract_items()
        # Third item uses HUR
        assert items[2]["unit"] == "Saat"

    def test_item_unit_code_translation_tne(self):
        root = _load_xml("valid_einvoice_usd.xml")
        ext = UBLFieldExtractor(root)
        items = ext.extract_items()
        assert items[0]["unit"] == "Ton"

    def test_multi_tax_rates(self):
        root = _load_xml("valid_einvoice_multi_tax.xml")
        ext = UBLFieldExtractor(root)
        items = ext.extract_items()
        rates = [item["tax_rate"] for item in items]
        assert 1.0 in rates
        assert 10.0 in rates
        assert 20.0 in rates

    # --- Metadata ---
    def test_extract_metadata_uuid(self):
        metadata = self.extractor.extract_metadata()
        assert metadata["e_invoice_uuid"] == "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        assert metadata["e_invoice_ettn"] == metadata["e_invoice_uuid"]

    def test_extract_metadata_invoice_type(self):
        metadata = self.extractor.extract_metadata()
        assert metadata["invoice_type_code"] == "SATIS"

    def test_extract_metadata_iade_type(self):
        root = _load_xml("valid_einvoice_iade.xml")
        ext = UBLFieldExtractor(root)
        metadata = ext.extract_metadata()
        assert metadata["invoice_type_code"] == "IADE"

    def test_extract_metadata_profile_id(self):
        metadata = self.extractor.extract_metadata()
        assert metadata["profile_id"] == "TICARIFATURA"

    # --- Address Concatenation ---
    def test_address_concatenation_format(self):
        supplier = self.extractor.extract_supplier()
        address = supplier["address"]
        # Should contain street, subcity, city, country separated by commas
        parts = [p.strip() for p in address.split(",")]
        assert len(parts) >= 3  # At least street, subcity, city
