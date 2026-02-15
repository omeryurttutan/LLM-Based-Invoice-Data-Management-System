from typing import List, Optional
from pydantic import BaseModel, Field

class InvoiceItem(BaseModel):
    description: Optional[str] = Field(None, description="Ürün/hizmet açıklaması", alias="aciklama")
    quantity: Optional[float] = Field(None, description="Miktar", alias="miktar")
    unit: Optional[str] = Field(None, description="Birim (adet, kg, vb.)", alias="birim")
    unit_price: Optional[float] = Field(None, description="Birim fiyat (KDV hariç)", alias="birim_fiyat")
    tax_rate: Optional[float] = Field(None, description="KDV oranı (%)", alias="kdv_orani")
    tax_amount: Optional[float] = Field(None, description="KDV tutarı", alias="kdv_tutari")
    line_total: Optional[float] = Field(None, description="Satır toplamı (KDV dahil)", alias="toplam_tutar")
    
    model_config = {"populate_by_name": True}

class InvoiceData(BaseModel):
    invoice_number: Optional[str] = Field(None, description="Fatura numarası", alias="fatura_no")
    invoice_date: Optional[str] = Field(None, description="Fatura tarihi (YYYY-MM-DD)", alias="tarih")
    due_date: Optional[str] = Field(None, description="Vade tarihi (YYYY-MM-DD)", alias="vade_tarihi")
    
    supplier_name: Optional[str] = Field(None, description="Tedarikçi firma adı", alias="gonderici_unvan")
    supplier_tax_number: Optional[str] = Field(None, description="Tedarikçi VKN/TCKN", alias="gonderici_vkn")
    supplier_address: Optional[str] = Field(None, description="Tedarikçi adresi", alias="gonderici_adres")
    
    buyer_name: Optional[str] = Field(None, description="Alıcı firma adı", alias="alici_unvan")
    buyer_tax_number: Optional[str] = Field(None, description="Alıcı VKN/TCKN", alias="alici_vkn")
    
    items: List[InvoiceItem] = Field(default_factory=list, description="Fatura kalemleri", alias="kalemler")
    
    subtotal: Optional[float] = Field(None, description="Ara toplam (KDV hariç)", alias="ara_toplam")
    tax_amount: Optional[float] = Field(None, description="Toplam KDV tutarı", alias="vergi_toplam")
    total_amount: Optional[float] = Field(None, description="Genel toplam (KDV dahil)", alias="genel_toplam")
    currency: Optional[str] = Field("TRY", description="Para birimi (TRY, USD, EUR, GBP)", alias="para_birimi")
    
    # E-Invoice Specific Fields
    e_invoice_uuid: Optional[str] = Field(None, description="E-Fatura UUID (GİB)", alias="ettn") # Sometimes ETTN is UUID
    e_invoice_ettn: Optional[str] = Field(None, description="ETTN (Elektronik Takip Numarası)")
    invoice_type_code: Optional[str] = Field(None, description="Fatura Tipi (SATIS, IADE, vb.)", alias="fatura_tipi")
    profile_id: Optional[str] = Field(None, description="Senaryo (TICARIFATURA, vb.)", alias="senaryo")
    
    notes: Optional[str] = Field(None, description="Fatura notları", alias="notlar")
    
    model_config = {"populate_by_name": True}
