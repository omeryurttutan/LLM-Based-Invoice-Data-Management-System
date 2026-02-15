import json
import re
from typing import Dict, Any, Optional
from datetime import datetime
from app.models.invoice_data import InvoiceData
from app.core.logging import logger
from app.services.llm.base_provider import LLMResponseError

class ResponseParser:
    """
    Parses and normalizes raw LLM text responses into InvoiceData models.
    """
    
    @staticmethod
    def parse(raw_text: str) -> InvoiceData:
        """
        Parse raw JSON string from LLM into InvoiceData object.
        Handles markdown stripping and basic cleaning.
        """
        cleaned_text = ResponseParser._clean_text(raw_text)
        
        try:
            data = json.loads(cleaned_text)
        except json.JSONDecodeError as e:
            # Attempt to find JSON blob if mixed with text
            match = re.search(r'\{.*\}', cleaned_text, re.DOTALL)
            if match:
                try:
                    data = json.loads(match.group(0))
                except json.JSONDecodeError:
                    logger.error("llm_parse_error", error=str(e), raw_text=raw_text[:500])
                    raise LLMResponseError("Failed to parse JSON from LLM response")
            else:
                logger.error("llm_parse_error", error=str(e), raw_text=raw_text[:500])
                raise LLMResponseError("Invalid JSON received from LLM")
                
        # Normalize data (dates, numbers, etc.)
        normalized_data = ResponseParser._normalize(data)
        
        try:
            return InvoiceData(**normalized_data)
        except Exception as e:
             logger.error("llm_validation_error", error=str(e))
             raise LLMResponseError(f"Data validation failed: {str(e)}")

    @staticmethod
    def _clean_text(text: str) -> str:
        """Strip markdown code fences and whitespace."""
        text = text.strip()
        # Remove ```json ... ``` or ``` ... ```
        if text.startswith("```"):
            # Find first newline
            first_newline = text.find("\n")
            if first_newline != -1:
                # Check if it ends with ```
                last_fence = text.rfind("```")
                if last_fence > first_newline:
                    return text[first_newline+1:last_fence].strip()
        return text

    @staticmethod
    def _normalize(data: Dict[str, Any]) -> Dict[str, Any]:
        """Normalize fields like dates and numbers."""
        
        # Helper to strict float conversion
        def to_float(val):
            if val is None: return 0.0
            if isinstance(val, (float, int)): return float(val)
            if isinstance(val, str):
                # Remove currency symbols or whitespace
                val = val.replace("TL", "").replace("TRY", "").strip()
                # If comma is used as decimal (Turkish format), replace
                if "," in val and "." in val:
                    if val.rfind(",") > val.rfind("."):
                         # TR/EU Format: 1.234,56 -> Remove dot, replace comma with dot
                         val = val.replace(".", "").replace(",", ".")
                    else:
                         # US Format: 1,234.56 -> Remove comma
                         val = val.replace(",", "")
                elif "," in val:
                     # Ambiguous case: 1234,56 or 1,234?
                     # Standardize on comma being decimal if no dot present? 
                     # Or assume TR default?
                     # app defaults to TR logic: 1234,56 -> 1234.56
                     val = val.replace(",", ".")
                try:
                    return float(val)
                except:
                    return 0.0
            return 0.0

        # Helper to normalize date
        def to_date(val):
            if not val: return None
            # Try parsing DD.MM.YYYY, DD/MM/YYYY
            for fmt in ("%d.%m.%Y", "%d/%m/%Y", "%Y-%m-%d"):
                try:
                    dt = datetime.strptime(val, fmt)
                    return dt.strftime("%Y-%m-%d")
                except ValueError:
                    continue
            return val # Return as is if cannot parse (Pydantic might catch it or fail)

        # Normalize root fields
        if "total_amount" in data: data["total_amount"] = to_float(data["total_amount"])
        if "tax_amount" in data: data["tax_amount"] = to_float(data["tax_amount"])
        if "subtotal" in data: data["subtotal"] = to_float(data["subtotal"])
        
        if "invoice_date" in data: data["invoice_date"] = to_date(data["invoice_date"])
        if "due_date" in data: data["due_date"] = to_date(data["due_date"])
        
        if "currency" in data: 
             data["currency"] = str(data.get("currency", "TRY")).upper()
             if data["currency"] not in ["TRY", "USD", "EUR", "GBP"]:
                 data["currency"] = "TRY"

        # Normalize items
        if "items" in data and isinstance(data["items"], list):
            for item in data["items"]:
                if "unit_price" in item: item["unit_price"] = to_float(item["unit_price"])
                if "line_total" in item: item["line_total"] = to_float(item["line_total"])
                if "tax_amount" in item: item["tax_amount"] = to_float(item["tax_amount"])
                if "tax_rate" in item: item["tax_rate"] = to_float(item["tax_rate"])
                if "quantity" in item: item["quantity"] = to_float(item["quantity"])

        return data
