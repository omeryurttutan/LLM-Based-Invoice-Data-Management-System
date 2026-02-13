from typing import List, Optional
from pydantic import BaseModel, Field

class InvoiceItem(BaseModel):
    description: Optional[str] = Field(None, description="Ürün/hizmet açıklaması")
    quantity: Optional[float] = Field(None, description="Miktar")
    unit: Optional[str] = Field(None, description="Birim (adet, kg, vb.)")
    unit_price: Optional[float] = Field(None, description="Birim fiyat (KDV hariç)")
    tax_rate: Optional[float] = Field(None, description="KDV oranı (%)")
    tax_amount: Optional[float] = Field(None, description="KDV tutarı")
    line_total: Optional[float] = Field(None, description="Satır toplamı (KDV dahil)")

class InvoiceData(BaseModel):
    invoice_number: Optional[str] = Field(None, description="Fatura numarası")
    invoice_date: Optional[str] = Field(None, description="Fatura tarihi (YYYY-MM-DD)")
    due_date: Optional[str] = Field(None, description="Vade tarihi (YYYY-MM-DD)")
    
    supplier_name: Optional[str] = Field(None, description="Tedarikçi firma adı")
    supplier_tax_number: Optional[str] = Field(None, description="Tedarikçi VKN/TCKN")
    supplier_address: Optional[str] = Field(None, description="Tedarikçi adresi")
    
    buyer_name: Optional[str] = Field(None, description="Alıcı firma adı")
    buyer_tax_number: Optional[str] = Field(None, description="Alıcı VKN/TCKN")
    
    items: List[InvoiceItem] = Field(default_factory=list, description="Fatura kalemleri")
    
    subtotal: Optional[float] = Field(None, description="Ara toplam (KDV hariç)")
    tax_amount: Optional[float] = Field(None, description="Toplam KDV tutarı")
    total_amount: Optional[float] = Field(None, description="Genel toplam (KDV dahil)")
    currency: Optional[str] = Field("TRY", description="Para birimi (TRY, USD, EUR, GBP)")
    
    # E-Invoice Specific Fields
    e_invoice_uuid: Optional[str] = Field(None, description="E-Fatura UUID (GİB)")
    e_invoice_ettn: Optional[str] = Field(None, description="ETTN (Elektronik Takip Numarası)")
    invoice_type_code: Optional[str] = Field(None, description="Fatura Tipi (SATIS, IADE, vb.)")
    profile_id: Optional[str] = Field(None, description="Senaryo (TICARIFATURA, vb.)")
    
    notes: Optional[str] = Field(None, description="Fatura notları")
