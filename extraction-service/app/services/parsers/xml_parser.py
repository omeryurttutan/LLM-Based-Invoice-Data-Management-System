from lxml import etree
import time
from typing import Union, BinaryIO
from app.models.invoice_data import InvoiceData, InvoiceItem
from app.services.parsers.ubl_field_extractor import UBLFieldExtractor
from app.core.exceptions import XMLParseError, NotEInvoiceError
from app.core.logging import logger

class XMLParser:
    """
    Parses UBL-TR e-Invoice XML files into InvoiceData objects.
    """
    
    def parse(self, xml_content: bytes) -> InvoiceData:
        """
        Parses XML bytes and returns InvoiceData.
        """
        try:
            # Parse XML
            parser = etree.XMLParser(huge_tree=True) # Valid GIB invoices can be large
            root = etree.fromstring(xml_content, parser=parser)
            
            # Verify it's a UBL Invoice
            if not root.tag.endswith('Invoice'):
                 raise NotEInvoiceError("Root element is not Invoice")
            
            # Initialize Extractor
            extractor = UBLFieldExtractor(root)
            
            # Extract Fields
            invoice_number = extractor.extract_invoice_number()
            invoice_date = extractor.extract_invoice_date()
            due_date = extractor.extract_due_date()
            
            supplier = extractor.extract_supplier()
            buyer = extractor.extract_buyer()
            totals = extractor.extract_totals()
            currency = extractor.extract_currency()
            notes = extractor.extract_notes()
            
            items_data = extractor.extract_items()
            items = [InvoiceItem(**item) for item in items_data]
            
            metadata = extractor.extract_metadata()
            # Note: Metadata fields like uuid/ettn are not in InvoiceData
            # They should be handled by caller or we need to add them to InvoiceData.
            # Base InvoiceData doesn't have them, but ExtractionResult does? 
            # Actually ExtractionResult has source_type. 
            # The prompt says: "These fields map to the existing database columns... 
            # parse it directly... extract additional e-invoice specific data".
            
            # Since InvoiceData is shared strict model, we might need to extend it 
            # or pass metadata separately. 
            # However, looking at InvoiceData definition (Phase 15), it might not have these fields.
            # Let's check InvoiceData definition again if needed.
            # For now, we populate what matches InvoiceData.
            
            return InvoiceData(
                invoice_number=invoice_number,
                invoice_date=invoice_date,
                due_date=due_date,
                supplier_name=supplier['name'],
                supplier_tax_number=supplier['tax_number'],
                supplier_address=supplier['address'],
                buyer_name=buyer['name'],
                buyer_tax_number=buyer['tax_number'],
                items=items,
                subtotal=totals['subtotal'],
                tax_amount=totals['tax_amount'],
                total_amount=totals['total_amount'],
                currency=currency,
                e_invoice_uuid=metadata['e_invoice_uuid'],
                e_invoice_ettn=metadata['e_invoice_ettn'],
                invoice_type_code=metadata['invoice_type_code'],
                profile_id=metadata['profile_id'],
                notes=notes
            )
            
        except etree.XMLSyntaxError as e:
            logger.error("xml_parse_error", error=str(e))
            raise XMLParseError(f"Invalid XML syntax: {str(e)}")
        except Exception as e:
            logger.error("xml_other_error", error=str(e))
            raise e
