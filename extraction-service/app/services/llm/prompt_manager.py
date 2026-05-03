from typing import Dict, Any


class PromptManager:
    """
    Manages extraction prompt templates and versions.
    Supports system instruction (role) and user prompt (extraction instructions) split.
    """

    LATEST_VERSION = "v1"

    # ─── Public API ─────────────────────────────────────────────────────────────

    @staticmethod
    def get_system_instruction(version: str = "v1") -> str:
        """
        Get the system instruction (role definition) for the LLM.
        Set via the model's system_instruction parameter in Gemini.
        """
        if version == "v1":
            return PromptManager._v1_system_instruction()
        return PromptManager._v1_system_instruction()

    @staticmethod
    def get_prompt(version: str = "v1") -> str:
        """
        Get the user-facing extraction prompt for the specified version.
        This is sent alongside the invoice image in the content array.
        """
        if version == "v1":
            return PromptManager._v1_user_prompt()
        return PromptManager._v1_user_prompt()

    @staticmethod
    def get_prompt_info(version: str = None) -> Dict[str, Any]:
        """
        Return metadata about the current prompt version.
        Useful for debugging and result tracking.
        """
        ver = version or PromptManager.LATEST_VERSION
        return {
            "version": ver,
            "latest_version": PromptManager.LATEST_VERSION,
            "system_instruction_length": len(PromptManager.get_system_instruction(ver)),
            "user_prompt_length": len(PromptManager.get_prompt(ver)),
        }

    @staticmethod
    def get_openai_messages(version: str = "v1") -> list:
        """
        Get the prompt formatted as OpenAI chat messages array.
        Same content as Gemini prompts, but packaged in OpenAI's message format.
        
        Returns:
            List of message dicts with 'role' and 'content' keys.
            The user message content will need the image block appended by the provider.
        """
        system_instruction = PromptManager.get_system_instruction(version)
        user_prompt = PromptManager.get_prompt(version)
        return [
            {"role": "system", "content": system_instruction},
            {"role": "user", "content": user_prompt},
        ]

    # ─── V1 Templates ──────────────────────────────────────────────────────────

    @staticmethod
    def _v1_system_instruction() -> str:
        return (
            "You are an expert Turkish invoice data extraction system. "
            "You receive invoice images — which may be scans, photographs, or digital documents "
            "with varying quality — and must visually analyze the document to extract ALL relevant "
            "fields into a specific JSON format. "
            "You act as a professional data entry specialist for Turkish accounting offices. "
            "You verify every field twice before producing output. "
            "You MUST preserve all Turkish characters exactly as they appear on the invoice: "
            "Ç, ç, Ğ, ğ, I, ı, İ, i, Ö, ö, Ş, ş, Ü, ü."
        )

    @staticmethod
    def _v1_user_prompt() -> str:
        return """Analyze the attached invoice image and extract ALL data into the following JSON schema.

### EXPECTED JSON OUTPUT SCHEMA
Return a single JSON object with this exact schema:
{
  "invoice_number": "string — fatura numarası",
  "invoice_date": "string — format: YYYY-MM-DD",
  "due_date": "string or null — format: YYYY-MM-DD — vade tarihi",
  "supplier_name": "string — tedarikçi/satıcı firma adı",
  "supplier_tax_number": "string — VKN (10 hane) veya TCKN (11 hane)",
  "supplier_address": "string or null — tedarikçi adresi",
  "buyer_name": "string or null — alıcı firma adı",
  "buyer_tax_number": "string or null — alıcı VKN/TCKN",
  "items": [
    {
      "description": "string — ürün/hizmet açıklaması",
      "quantity": "number — miktar",
      "unit": "string or null — birim (adet, kg, lt, vb.)",
      "unit_price": "number — birim fiyat (KDV hariç)",
      "tax_rate": "number — KDV oranı (%, örn: 20)",
      "tax_amount": "number — KDV tutarı",
      "line_total": "number — satır toplamı (KDV dahil)"
    }
  ],
  "subtotal": "number — ara toplam (KDV hariç)",
  "tax_amount": "number — toplam KDV tutarı",
  "total_amount": "number — genel toplam (KDV dahil)",
  "currency": "string — para birimi: TRY, USD, EUR, GBP",
  "notes": "string or null — varsa fatura üzerindeki notlar"
}

### TURKISH INVOICE-SPECIFIC INSTRUCTIONS
- **Field Labels to look for**: "Fatura No", "Fatura Tarihi", "Vade Tarihi", "Toplam", "KDV", "Ara Toplam", "Genel Toplam", "Vergi Dairesi", "VKN", "TCKN".
- **Number Format**: Turkish invoices use COMMA (,) as decimal separator (e.g., 1.234,56). Convert to standard dot format (1234.56) in the JSON.
- **Date Format**: Turkish invoices use DD.MM.YYYY or DD/MM/YYYY. Always convert to YYYY-MM-DD in the JSON.
- **Currency**: If not explicitly stated, default to "TRY".
- **Tax Rates**: Common Turkish KDV rates are 1%, 10%, 20%.
- **Tax Inclusive/Exclusive**: "KDV Dahil" = Tax Inclusive, "KDV Hariç" = Tax Exclusive.
- **Turkish Characters**: Preserve ALL Turkish characters exactly (Ç, ç, Ğ, ğ, I, ı, İ, i, Ö, ö, Ş, ş, Ü, ü). Never transliterate.

### OUTPUT RULES
- Return **ONLY valid JSON**. No markdown code blocks (```json), no explanations, no extra text.
- If a field is not visible or not found in the image, set it to **null** (not empty string "").
- All monetary amounts must be **numbers** (not strings), using dot as decimal separator.
- If individual items cannot be identified, create a single item with the totals.
- Validate that subtotal + tax_amount ≈ total_amount."""
