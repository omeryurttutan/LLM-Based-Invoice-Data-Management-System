import { z } from 'zod';

export const invoiceItemSchema = z.object({
  id: z.string().optional(),
  description: z.string().min(1, 'Açıklama gereklidir'),
  quantity: z.number().min(0, 'Miktar 0 veya daha büyük olmalıdır'),
  unit: z.string().min(1, 'Birim gereklidir'), // Using string to allow flexible input, or enum if strict
  unitPrice: z.number().min(0, 'Birim fiyat 0 veya daha büyük olmalıdır'),
  taxRate: z.number().min(0, 'KDV oranı 0 veya daha büyük olmalıdır'),
  taxAmount: z.number().min(0, 'KDV tutarı 0 veya daha büyük olmalıdır'),
  subtotal: z.number().min(0, 'Satır toplamı 0 veya daha büyük olmalıdır'),
});

export const verificationSchema = z.object({
  invoiceNumber: z.string().min(1, 'Fatura numarası gereklidir'),
  invoiceDate: z.string().min(1, 'Fatura tarihi gereklidir'), // YYYY-MM-DD
  dueDate: z.string().optional().or(z.literal('')),
  
  // Supplier
  supplierName: z.string().min(1, 'Tedarikçi adı gereklidir'),
  supplierTaxNumber: z.string().optional().or(z.literal('')),
  supplierAddress: z.string().optional().or(z.literal('')),
  
  // Buyer
  buyerName: z.string().optional().or(z.literal('')),
  buyerTaxNumber: z.string().optional().or(z.literal('')),
  
  // Items
  items: z.array(invoiceItemSchema),
  
  // Amounts
  subtotal: z.number().min(0, 'Ara toplam 0 veya daha büyük olmalıdır'),
  taxAmount: z.number().min(0, 'KDV tutarı 0 veya daha büyük olmalıdır'),
  totalAmount: z.number().min(0, 'Genel toplam 0 veya daha büyük olmalıdır'),
  currency: z.enum(['TRY', 'USD', 'EUR', 'GBP']),
  
  // Notes
  notes: z.string().optional().or(z.literal('')),
});

export type VerificationFormData = z.infer<typeof verificationSchema>;
export type VerificationItemData = z.infer<typeof invoiceItemSchema>;
