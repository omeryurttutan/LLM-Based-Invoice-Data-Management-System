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
    Handles markdown stripping, JSON extraction, Turkish format normalization.
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
                    logger.warning("response_cleanup_applied", reason="JSON extracted from surrounding text")
                except json.JSONDecodeError:
                    logger.error("llm_parse_error", error=str(e), raw_text=raw_text[:500])
                    raise LLMResponseError("Failed to parse JSON from LLM response")
            else:
                logger.error("llm_parse_error", error=str(e), raw_text=raw_text[:500])
                raise LLMResponseError("Invalid JSON received from LLM")

        # Normalize data (dates, numbers, etc.)
        if not isinstance(data, dict):
            logger.error("llm_parse_error", error="JSON is not a dict", raw_text=raw_text[:500])
            raise LLMResponseError("LLM response JSON is not an object (dict)")

        normalized_data = ResponseParser._normalize(data)

        try:
            return InvoiceData(**normalized_data)
        except Exception as e:
            logger.error("llm_validation_error", error=str(e))
            raise LLMResponseError(f"Data validation failed: {str(e)}")

    @staticmethod
    def _clean_text(text: str) -> str:
        """Strip markdown code fences, repair malformed JSON, and handle truncation."""
        text = text.strip()
        # Remove ```json ... ``` or ``` ... ```
        if text.startswith("```"):
            first_newline = text.find("\n")
            if first_newline != -1:
                last_fence = text.rfind("```")
                if last_fence > first_newline:
                    cleaned = text[first_newline + 1:last_fence].strip()
                    logger.warning("response_cleanup_applied", reason="Markdown code fences stripped")
                    text = cleaned

        # Fix missing closing brace before comma in arrays: },\n    , → },\n    {  
        # Pattern: a value (number/string/null/bool) followed by newline+spaces+comma+newline+spaces+open-brace
        # This catches cases where LLM forgets } before , in array items
        text = re.sub(r'(\d+\.?\d*)\s*\n(\s*),\s*\n(\s*)\{', r'\1\n\2},\n\3{', text)
        text = re.sub(r'(null|true|false)\s*\n(\s*),\s*\n(\s*)\{', r'\1\n\2},\n\3{', text)
        text = re.sub(r'"\s*\n(\s*),\s*\n(\s*)\{', r'"\n\1},\n\2{', text)

        # Fix truncated numbers at the end (e.g., "3295." → "3295.0")
        text = re.sub(r'(\d+)\.$', r'\g<1>.0', text)
        text = re.sub(r'(\d+)\.\s*$', r'\g<1>.0', text)

        # Handle truncated JSON: auto-close unclosed brackets and braces
        if text.startswith('{') or text.startswith('['):
            open_braces = text.count('{') - text.count('}')
            open_brackets = text.count('[') - text.count(']')
            if open_braces > 0 or open_brackets > 0:
                logger.warning("response_cleanup_applied", reason=f"Auto-closing {open_brackets} brackets and {open_braces} braces")
                # Remove trailing comma if present
                text = text.rstrip()
                text = re.sub(r',\s*$', '', text)
                # Also remove trailing incomplete key-value like '"key":' or '"key': 
                text = re.sub(r',?\s*"[^"]*"\s*:\s*$', '', text)
                text += ']' * open_brackets
                text += '}' * open_braces

        return text

    @staticmethod
    def _normalize(data: Dict[str, Any]) -> Dict[str, Any]:
        """Normalize fields like dates, numbers, empty strings, and Turkish formats."""

        def to_float(val):
            if val is None:
                return 0.0
            if isinstance(val, (float, int)):
                return float(val)
            if isinstance(val, str):
                # Remove currency symbols or whitespace
                val = val.replace("TL", "").replace("TRY", "").replace("₺", "").strip()
                if not val:
                    return 0.0
                # Handle Turkish/European vs US number format
                if "," in val and "." in val:
                    if val.rfind(",") > val.rfind("."):
                        # TR/EU Format: 1.234,56 → 1234.56
                        val = val.replace(".", "").replace(",", ".")
                        logger.warning("turkish_number_format_converted", original_hint="TR/EU comma decimal")
                    else:
                        # US Format: 1,234.56 → 1234.56
                        val = val.replace(",", "")
                elif "," in val:
                    # Ambiguous: default to TR logic (comma = decimal)
                    val = val.replace(",", ".")
                    logger.warning("turkish_number_format_converted", original_hint="Comma-only assumed decimal")
                try:
                    return float(val)
                except ValueError:
                    return 0.0
            return 0.0

        def to_date(val):
            if not val:
                return None
            if isinstance(val, str):
                val = val.strip()
                if not val:
                    return None
            for fmt in ("%d.%m.%Y", "%d/%m/%Y", "%Y-%m-%d"):
                try:
                    dt = datetime.strptime(val, fmt)
                    converted = dt.strftime("%Y-%m-%d")
                    if fmt != "%Y-%m-%d":
                        logger.warning("turkish_date_format_converted", original=val, converted=converted)
                    return converted
                except ValueError:
                    continue
            return val  # Return as-is if cannot parse

        def clean_string(val):
            """Convert empty strings to None and strip whitespace."""
            if val is None:
                return None
            if isinstance(val, str):
                val = val.strip()
                return val if val else None
            return val

        # Normalize string fields → empty string to None
        string_fields = [
            "invoice_number", "supplier_name", "supplier_tax_number",
            "supplier_address", "buyer_name", "buyer_tax_number", "notes",
        ]
        for field in string_fields:
            if field in data:
                data[field] = clean_string(data[field])

        # Normalize numeric fields
        if "total_amount" in data:
            data["total_amount"] = to_float(data["total_amount"])
        if "tax_amount" in data:
            data["tax_amount"] = to_float(data["tax_amount"])
        if "subtotal" in data:
            data["subtotal"] = to_float(data["subtotal"])

        # Normalize dates
        if "invoice_date" in data:
            data["invoice_date"] = to_date(data["invoice_date"])
        if "due_date" in data:
            data["due_date"] = to_date(data["due_date"])

        # Normalize currency
        if "currency" in data:
            currency = str(data.get("currency", "TRY")).upper().strip()
            if currency in ("TL", ""):
                currency = "TRY"
            if currency not in ("TRY", "USD", "EUR", "GBP"):
                currency = "TRY"
            data["currency"] = currency
        else:
            data["currency"] = "TRY"

        # Normalize items
        if "items" in data and isinstance(data["items"], list):
            for item in data["items"]:
                if "unit_price" in item:
                    item["unit_price"] = to_float(item["unit_price"])
                if "line_total" in item:
                    item["line_total"] = to_float(item["line_total"])
                if "tax_amount" in item:
                    item["tax_amount"] = to_float(item["tax_amount"])
                if "tax_rate" in item:
                    item["tax_rate"] = to_float(item["tax_rate"])
                if "quantity" in item:
                    item["quantity"] = to_float(item["quantity"])
                # Clean string fields in items
                if "description" in item:
                    item["description"] = clean_string(item["description"])
                if "unit" in item:
                    item["unit"] = clean_string(item["unit"])

        return data
