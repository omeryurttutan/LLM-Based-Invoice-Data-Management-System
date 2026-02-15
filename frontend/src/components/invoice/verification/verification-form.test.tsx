import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { VerificationForm } from './verification-form';
import { InvoiceDetail } from '@/types/invoice';

// Mock invoice data
const mockInvoice: InvoiceDetail = {
  id: '1',
  invoiceNumber: 'INV-001',
  invoiceDate: '2023-10-27',
  supplierName: 'Test Supplier',
  currency: 'TRY',
  status: 'PENDING',
  sourceType: 'LLM',
  items: [
    {
      lineNumber: 1,
      description: 'Test Item',
      quantity: 1,
      unit: 'ADET',
      unitPrice: 100,
      taxRate: 20,
      taxAmount: 20,
      subtotal: 100,
      totalAmount: 120,
    }
  ],
  subtotal: 100,
  taxAmount: 20,
  totalAmount: 120,
  createdAt: '2023-10-27T10:00:00Z',
  updatedAt: '2023-10-27T10:00:00Z',
  createdByUserId: 'user1',
  createdByUserName: 'Test User',
};

// Mock useRouter
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
    back: jest.fn(),
  }),
}));

// Mock invoice service
jest.mock('@/services/invoice-service', () => ({
  invoiceService: {
    updateInvoice: jest.fn(),
    verifyInvoice: jest.fn(),
    rejectInvoice: jest.fn(),
    validateExtraction: jest.fn(),
  },
}));

describe('VerificationForm', () => {
  it('renders invoice details correctly', () => {
    render(<VerificationForm invoice={mockInvoice} />);
    
    expect(screen.getByDisplayValue('INV-001')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Test Supplier')).toBeInTheDocument();
    expect(screen.getByDisplayValue('100')).toBeInTheDocument(); // Subtotal
  });

  it('renders action buttons for pending invoice', () => {
    render(<VerificationForm invoice={mockInvoice} />);
    
    expect(screen.getByText(/Onayla ve Kaydet/i)).toBeInTheDocument();
    expect(screen.getByText(/Reddet/i)).toBeInTheDocument();
    expect(screen.getByText(/Taslak Kaydet/i)).toBeInTheDocument();
  });

  it('does not render action buttons for verified invoice', () => {
    const verifiedInvoice = { ...mockInvoice, status: 'VERIFIED' asconst };
    render(<VerificationForm invoice={verifiedInvoice} />);
    
    expect(screen.queryByText(/Onayla ve Kaydet/i)).not.toBeInTheDocument();
  });
});
