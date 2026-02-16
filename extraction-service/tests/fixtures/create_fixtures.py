from PIL import Image
import os
from reportlab.pdfgen import canvas

fixtures_dir = "/home/omer/Desktop/fatura-project/Fatura-OCR/extraction-service/tests/fixtures/sample_invoices"
os.makedirs(fixtures_dir, exist_ok=True)

# Create receipt_image.png
img = Image.new('RGB', (100, 200), color='white')
img.save(os.path.join(fixtures_dir, "receipt_image.png"), format='PNG')

# Create multi_page_invoice.pdf
c = canvas.Canvas(os.path.join(fixtures_dir, "multi_page_invoice.pdf"))
c.drawString(100, 750, "Page 1")
c.showPage()
c.drawString(100, 750, "Page 2")
c.save()

print("Created binary fixtures")
