from app.models.invoice_data import InvoiceData, InvoiceItem
from app.services.validation.field_validator import FieldValidator
from app.services.validation.validator import Validator

def debug_field_validator():
    print("--- Debugging Critical Field ---")
    data = InvoiceData(
        invoice_number="", # Missing
        invoice_date="2023-10-25",
        supplier_name="Test Supplier",
        total_amount=100.0,
        supplier_tax_number="1234567890",
        subtotal=100.0,
        tax_amount=0.0,
        currency="TRY"
    )
    score, issues = FieldValidator.validate(data)
    print(f"Score: {score}")
    for i in issues:
        print(f"Issue: {i.field} - {i.message} ({i.severity})")

    print("\n--- Debugging Important Field ---")
    data2 = InvoiceData(
        invoice_number="INV-001",
        invoice_date="2023-10-25",
        supplier_name="Test Supplier",
        total_amount=100.0,
        supplier_tax_number="", # Missing important
        subtotal=100.0,
        tax_amount=0.0,
        currency="TRY"
    )
    score2, issues2 = FieldValidator.validate(data2)
    print(f"Score: {score2}")
    for i in issues2:
        print(f"Issue: {i.field} - {i.message} ({i.severity})")

if __name__ == "__main__":
    debug_field_validator()
