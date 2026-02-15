from PIL import Image, ImageDraw, ImageFont
import os

def create_image(filename, text="INVOICE", size=(800, 1000), color="white"):
    img = Image.new('RGB', size, color=color)
    d = ImageDraw.Draw(img)
    # Draw huge text
    d.text((10,10), text, fill=(0,0,0))
    
    # Ensure directory exists
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    img.save(filename)
    print(f"Created {filename}")

base_path = "/home/omer/Desktop/fatura-project/Fatura-OCR/extraction-service/tests/fixtures/sample_invoices"

create_image(os.path.join(base_path, "standard_invoice.jpg"), "Standard Invoice")
create_image(os.path.join(base_path, "low_quality_invoice.jpg"), "Low Quality Invoice", size=(200, 250))
create_image(os.path.join(base_path, "rotated_invoice.jpg"), "Rotated Invoice")
