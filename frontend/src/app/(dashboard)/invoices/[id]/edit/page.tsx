'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useInvoice, useUpdateInvoice } from '@/hooks/use-invoices';
import { useCategories } from '@/hooks/use-categories';
import { InvoiceForm } from '@/components/invoice/invoice-form';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface EditInvoicePageProps {
  params: {
    id: string;
  };
}

export default function EditInvoicePage({ params }: EditInvoicePageProps) {
  const router = useRouter();
  const { id } = params;

  const { data: invoice, isLoading: isInvoiceLoading, error } = useInvoice(id);
  const { data: categories, isLoading: isCategoriesLoading } = useCategories();
  const { mutate: updateInvoice, isPending } = useUpdateInvoice();

  useEffect(() => {
    if (invoice && invoice.status !== 'PENDING') {
      toast.error('Sadece beklemedeki faturalar düzenlenebilir.');
      router.push(`/invoices/${id}`);
    }
  }, [invoice, router, id]);

  if (isInvoiceLoading || isCategoriesLoading) {
    return <div className="flex justify-center items-center h-96"><Loader2 className="h-8 w-8 animate-spin" /></div>;
  }

  if (error || !invoice) {
    return <div className="text-center py-10">Fatura bulunamadı veya yüklenirken bir hata oluştu.</div>;
  }

  if (invoice.status !== 'PENDING') {
    return null; // Redirecting in useEffect
  }

  const handleSubmit = (data: any) => {
    updateInvoice({ id, data }, {
      onSuccess: () => {
        router.push(`/invoices/${id}`);
      }
    });
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-10">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Faturayı Düzenle</h1>
        <p className="text-muted-foreground">{invoice.invoiceNumber} numaralı faturayı düzenliyorsunuz.</p>
      </div>

      <InvoiceForm 
        initialData={invoice}
        categories={categories || []} 
        onSubmit={handleSubmit} 
        isLoading={isPending} 
      />
    </div>
  );
}
