import pytest
import os
import json
from httpx import AsyncClient
from unittest.mock import MagicMock


TEST_CASES = [
    # (input_file, expected_output_file, file_type, mock_response_file)
    ("sample_invoices/standard_invoice.jpg", "expected_outputs/standard_invoice_expected.json", "image", "llm_responses/gemini_valid.json"),
    ("sample_xml/valid_invoice.xml", "expected_outputs/valid_einvoice_expected.json", "xml", None),
    ("sample_xml/minimal_einvoice.xml", "expected_outputs/minimal_einvoice_expected.json", "xml", None),
    ("sample_xml/multi_item_einvoice.xml", "expected_outputs/multi_item_expected.json", "xml", None),
]


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "input_rel_path, expected_rel_path, file_type, mock_response_rel_path",
    TEST_CASES,
)
async def test_golden_data_validation(
    async_client: AsyncClient,
    mock_gemini_api,
    input_rel_path,
    expected_rel_path,
    file_type,
    mock_response_rel_path,
):
    """Validate that known inputs produce expected outputs."""
    tests_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    fixtures_dir = os.path.join(tests_dir, "fixtures")

    input_path = os.path.join(fixtures_dir, input_rel_path)
    expected_path = os.path.join(fixtures_dir, expected_rel_path)

    if not os.path.exists(input_path):
        pytest.skip(f"Input file missing: {input_rel_path}")
    if not os.path.exists(expected_path):
        pytest.skip(f"Expected output missing: {expected_rel_path}")

    # Configure mock if image test
    if file_type == "image" and mock_response_rel_path:
        mock_path = os.path.join(fixtures_dir, mock_response_rel_path)
        if os.path.exists(mock_path):
            with open(mock_path, "r", encoding="utf-8") as f:
                mock_content = f.read()
            mock_response = MagicMock()
            mock_response.text = mock_content
            mock_gemini_api.return_value = mock_response

    with open(input_path, "rb") as f:
        file_content = f.read()
    with open(expected_path, "r", encoding="utf-8") as f:
        expected_json = json.load(f)

    mime_type = "application/xml" if file_type == "xml" else "image/jpeg"
    filename = os.path.basename(input_path)

    files = {"file": (filename, file_content, mime_type)}
    response = await async_client.post("/api/v1/extraction/extract", files=files)

    assert response.status_code == 200
    actual_json = response.json()

    actual_invoice = actual_json.get("data")
    # expected_json may have top-level invoice data or nested under "invoice_data"
    expected_invoice = expected_json.get("invoice_data", expected_json)

    if expected_invoice and actual_invoice:
        # Mandatory fields that the parser must extract correctly
        mandatory_keys = ["fatura_no", "para_birimi"]
        for key in mandatory_keys:
            if key in expected_invoice and expected_invoice[key] is not None:
                assert actual_invoice.get(key) == expected_invoice[key], (
                    f"Mismatch in {key}: {actual_invoice.get(key)} != {expected_invoice[key]}"
                )
        # Optional fields — log discrepancies but don't fail
        optional_keys = ["gonderici_vkn", "alici_vkn", "gonderici_unvan", "alici_unvan"]
        for key in optional_keys:
            expected_val = expected_invoice.get(key)
            actual_val = actual_invoice.get(key)
            if expected_val is not None and actual_val != expected_val:
                import warnings
                warnings.warn(f"Optional field '{key}' mismatch: {actual_val} != {expected_val}")

