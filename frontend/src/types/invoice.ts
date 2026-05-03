export type InvoiceStatus = 'PENDING' | 'PROCESSING' | 'VERIFIED' | 'REJECTED' | 'QUEUED' | 'FAILED' | 'REVIEW_REQUIRED';
export type SourceType = 'LLM' | 'E_INVOICE' | 'MANUAL';
export type LlmProvider = 'GEMINI' | 'GPT' | 'CLAUDE';
export type Currency = 'TRY' | 'USD' | 'EUR' | 'GBP';
export type UnitType = 'ADET' | 'KG' | 'LT' | 'M' | 'M2' | 'M3' | 'PAKET' | 'KUTU' | 'SAAT' | 'GUN' | 'TON';

export interface InvoiceItem {
  id?: string;
  lineNumber: number;
  description: string;
  quantity: number;
  unit: UnitType;
  unitPrice: number;
  taxRate: number;
  taxAmount: number;
  subtotal: number;
  totalAmount: number;
  productCode?: string;
  barcode?: string;
}

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
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  supplierName: string;
  supplierTaxNumber?: string;
  supplierTaxOffice?: string;
  supplierAddress?: string;
  supplierPhone?: string;
  supplierEmail?: string;
  buyerName?: string;
  buyerTaxNumber?: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  currency: Currency;
  exchangeRate: number;
  status: InvoiceStatus;
  sourceType: SourceType;
  llmProvider?: LlmProvider;
  confidenceScore?: number;
  validationIssues?: ValidationIssue[];
  categoryId?: string;
  categoryName?: string;
  notes?: string;
  rejectionReason?: string;
  createdByUserId: string;
  createdByUserName: string;
  verifiedByUserId?: string;
  verifiedByUserName?: string;
  verifiedAt?: string;
  rejectedAt?: string;
  createdAt: string;
  updatedAt: string;
  items: InvoiceItem[];
}

export interface CreateInvoiceRequest {
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  supplierName: string;
  supplierTaxNumber?: string;
  supplierTaxOffice?: string;
  supplierAddress?: string;
  supplierPhone?: string;
  supplierEmail?: string;
  currency: Currency;
  exchangeRate?: number;
  categoryId?: string;
  notes?: string;
  items: CreateInvoiceItemRequest[];
}

export interface CreateInvoiceItemRequest {
  id?: string;
  description: string;
  quantity: number;
  unit: UnitType;
  unitPrice: number;
  taxRate: number;
  productCode?: string;
}

// Update UpdateInvoiceRequest to include corrections
export interface UpdateInvoiceRequest extends CreateInvoiceRequest {
  extractionCorrections?: ExtractionCorrection[];
}

export interface VerifyInvoiceRequest { notes?: string; }
export interface RejectInvoiceRequest { rejectionReason: string; }

export interface PaginatedResponse<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number; };
}

export interface InvoiceListParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: string; // CSV
  categoryId?: string; // CSV
  dateFrom?: string;
  dateTo?: string;
  amountMin?: number;
  amountMax?: number;
  currency?: string; // CSV
  sourceType?: string; // CSV
  llmProvider?: string; // CSV
  confidenceMin?: number;
  confidenceMax?: number;
  supplierName?: string; // CSV
  search?: string;
  createdByUserId?: string;
  createdFrom?: string;
  createdTo?: string;
}

export interface BatchFileStatus {
  id?: string;
  fileName: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  invoiceId?: string;
  error?: string;
}

export interface BatchStatusResponse {
  batchId: string;
  totalFiles: number;
  completedFiles: number;
  failedFiles: number;
  status: 'PROCESSING' | 'COMPLETED' | 'PARTIALLY_COMPLETED' | 'FAILED';
  files: BatchFileStatus[];
}

export interface FilterOptionsResponse {
  statuses: { value: InvoiceStatus; label: string }[];
  categories: { id: string; name: string; color?: string }[];
  currencies: Currency[];
  sourceTypes: SourceType[];
  llmProviders: LlmProvider[];
  amountRange: { min: number; max: number };
  dateRange: { min: string; max: string };
  confidenceRange: { min: number; max: number };
}

export interface ValidationIssue {
  field: string;
  issue: string;
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
}

export interface ExtractionCorrection {
  field: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  originalValue: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  correctedValue: any;
}

export interface ValidationResult {
  confidenceScore: number;
  validationIssues: ValidationIssue[];
}

export interface InvoiceData {
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  supplierName: string;
  supplierTaxNumber?: string;
  supplierAddress?: string;
  buyerName?: string;
  buyerTaxNumber?: string;
  items: InvoiceItem[];
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  currency: Currency;
  notes?: string;
}


