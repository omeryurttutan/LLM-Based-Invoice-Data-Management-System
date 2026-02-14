export type InvoiceStatus = 'PENDING' | 'PROCESSING' | 'VERIFIED' | 'REJECTED';
export type SourceType = 'MANUAL' | 'LLM' | 'E_INVOICE';
export type Currency = 'TRY' | 'USD' | 'EUR' | 'GBP';

export interface InvoiceListItem {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  supplierName: string;
  totalAmount: number;
  currency: Currency;
  status: InvoiceStatus;
  sourceType: SourceType;
  categoryName?: string;
  itemCount: number;
  createdByUserName: string;
  createdAt: string;
}

export interface InvoiceDetail {
    id: string;
    // Add more details as needed in Phase 12
    invoiceNumber: string;
    supplierName: string;
}
