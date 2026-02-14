from lxml import etree
from typing import List, Dict, Optional, Any
from app.core.exceptions import MissingRequiredFieldError
from decimal import Decimal

class UBLFieldExtractor:
    """
    Helper class to extract specific fields from UBL-TR Invoice XML.
    """
    
    NAMESPACES = {
        'ubl': 'urn:oasis:names:specification:ubl:schema:xsd:Invoice-2',
        'cac': 'urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2',
        'cbc': 'urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2',
        'ext': 'urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2'
    }

    def __init__(self, xml_root: etree._Element):
        self.root = xml_root

    def _xpath(self, path: str, element: Optional[etree._Element] = None) -> List[Any]:
        """Executes XPath on the root or provided element with namespaces."""
        el = element if element is not None else self.root
        return el.xpath(path, namespaces=self.NAMESPACES)

    def _text(self, path: str, element: Optional[etree._Element] = None) -> Optional[str]:
        """Returns the text content of the first match or None."""
        results = self._xpath(path, element)
        if results and results[0].text:
            return results[0].text.strip()
        return None
        
    def extract_invoice_number(self) -> str:
        val = self._text('/ubl:Invoice/cbc:ID')
        if not val:
            raise MissingRequiredFieldError("invoice_number (cbc:ID)")
        return val

    def extract_invoice_date(self) -> str:
        # returns YYYY-MM-DD
        val = self._text('/ubl:Invoice/cbc:IssueDate')
        if not val:
            raise MissingRequiredFieldError("invoice_date (cbc:IssueDate)")
        return val

    def extract_due_date(self) -> Optional[str]:
        # Try PaymentMeans first, then PaymentTerms
        val = self._text('/ubl:Invoice/cac:PaymentMeans/cbc:PaymentDueDate')
        if not val:
            val = self._text('/ubl:Invoice/cac:PaymentTerms/cbc:PaymentDueDate')
        return val

    def extract_supplier(self) -> Dict[str, Optional[str]]:
        party = self._xpath('/ubl:Invoice/cac:AccountingSupplierParty/cac:Party')
        if not party:
            raise MissingRequiredFieldError("Supplier Party")
        party = party[0]
        
        name = self._text('cac:PartyName/cbc:Name', party)
        if not name:
             # Fallback to PartyIdentification/cbc:ID if name is missing? 
             # Or Person/FirstName + FamilyName?
             # UBL-TR usually has PartyName/Name or Person/Name
             person_first = self._text('cac:Person/cbc:FirstName', party)
             person_last = self._text('cac:Person/cbc:FamilyName', party)
             if person_first and person_last:
                 name = f"{person_first} {person_last}"
        
        # Tax Number (VKN/TCKN)
        tax_id = self._text("cac:PartyIdentification/cbc:ID[@schemeID='VKN' or @schemeID='TCKN']", party)
        # Verify valid schemeID if needed, but XPath handles filtering
        
        # Address
        address_parts = []
        street = self._text('cac:PostalAddress/cbc:StreetName', party)
        if street: address_parts.append(street)
        
        building = self._text('cac:PostalAddress/cbc:BuildingNumber', party)
        if building: address_parts.append(building)
        
        subcity = self._text('cac:PostalAddress/cbc:CitySubdivisionName', party)
        if subcity: address_parts.append(subcity)
        
        city = self._text('cac:PostalAddress/cbc:CityName', party)
        if city: address_parts.append(city)
        
        country = self._text('cac:PostalAddress/cac:Country/cbc:Name', party)
        if country: address_parts.append(country)
        
        address = ", ".join(address_parts) if address_parts else None

        return {
            "name": name,
            "tax_number": tax_id,
            "address": address
        }

    def extract_buyer(self) -> Dict[str, Optional[str]]:
        party = self._xpath('/ubl:Invoice/cac:AccountingCustomerParty/cac:Party')
        if not party:
            # Maybe not strictly required for validation but usually present
            return {"name": None, "tax_number": None, "address": None}
        party = party[0]

        name = self._text('cac:PartyName/cbc:Name', party)
        if not name:
             person_first = self._text('cac:Person/cbc:FirstName', party)
             person_last = self._text('cac:Person/cbc:FamilyName', party)
             if person_first and person_last:
                 name = f"{person_first} {person_last}"

        tax_id = self._text("cac:PartyIdentification/cbc:ID[@schemeID='VKN' or @schemeID='TCKN']", party)
        
        return {
            "name": name,
            "tax_number": tax_id
        }

    def extract_totals(self) -> Dict[str, float]:
        subtotal = float(self._text('/ubl:Invoice/cac:LegalMonetaryTotal/cbc:LineExtensionAmount') or 0.0)
        tax_amount = float(self._text('/ubl:Invoice/cac:TaxTotal/cbc:TaxAmount') or 0.0)
        total_amount = float(self._text('/ubl:Invoice/cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount') or 0.0)
        if total_amount == 0.0:
             total_amount = float(self._text('/ubl:Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount') or 0.0)
             
        return {
            "subtotal": subtotal,
            "tax_amount": tax_amount,
            "total_amount": total_amount
        }

    def extract_currency(self) -> str:
        return self._text('/ubl:Invoice/cbc:DocumentCurrencyCode') or "TRY"

    def extract_notes(self) -> Optional[str]:
        notes = self._xpath('/ubl:Invoice/cbc:Note')
        if not notes:
            return None
        return "\n".join([n.text.strip() for n in notes if n.text])

    def extract_items(self) -> List[Dict[str, Any]]:
        lines = self._xpath('/ubl:Invoice/cac:InvoiceLine')
        items = []
        for line in lines:
            desc = self._text('cac:Item/cbc:Name', line)
            
            qty_node = self._xpath('cbc:InvoicedQuantity', line)
            qty = 1.0
            unit = None
            if qty_node:
                qty = float(qty_node[0].text or 1.0)
                unit_code = qty_node[0].get('unitCode')
                unit = self._translate_unit_code(unit_code)

            price = float(self._text('cac:Price/cbc:PriceAmount', line) or 0.0)
            
            # Tax rate
            tax_rate = float(self._text('cac:TaxTotal/cac:TaxSubtotal/cbc:Percent', line) or 0.0)
            tax_amt = float(self._text('cac:TaxTotal/cac:TaxSubtotal/cbc:TaxAmount', line) or 0.0)
            
            line_total = float(self._text('cbc:LineExtensionAmount', line) or 0.0)
            
            items.append({
                "description": desc or "Ürün/Hizmet",
                "quantity": qty,
                "unit": unit,
                "unit_price": price,
                "tax_rate": tax_rate,
                "tax_amount": tax_amt,
                "line_total": line_total
            })
        return items
        
    def _translate_unit_code(self, code: Optional[str]) -> str:
        if not code: return "Adet"
        mapping = {
            "C62": "Adet",
            "KGM": "Kg",
            "LTR": "Lt",
            "MTR": "Metre",
            "MTK": "m²",
            "MTQ": "m³",
            "TNE": "Ton",
            "HUR": "Saat",
            "DAY": "Gün",
            "MON": "Ay",
            "ANN": "Yıl",
            "NIU": "Adet", 
            "EA": "Adet"
        }
        return mapping.get(code, code)

    def extract_metadata(self) -> Dict[str, str]:
        uuid = self._text('/ubl:Invoice/cbc:UUID')
        invoice_type = self._text('/ubl:Invoice/cbc:InvoiceTypeCode')
        profile_id = self._text('/ubl:Invoice/cbc:ProfileID')
        
        return {
            "e_invoice_uuid": uuid,
            "e_invoice_ettn": uuid, # Usually same
            "invoice_type_code": invoice_type,
            "profile_id": profile_id
        }
