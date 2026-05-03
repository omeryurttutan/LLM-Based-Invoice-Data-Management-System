'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useInvoice, useUpdateInvoice } from '@/hooks/use-invoices';
import { useCategories } from '@/hooks/use-categories';
import { InvoiceForm } from '@/components/invoice/invoice-form';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useTranslations } from 'next-intl';

interface EditInvoicePageProps {
  params: {
    id: string;
  };
}

export default function EditInvoicePage({ params }: EditInvoicePageProps) {
  const router = useRouter();
  const { id } = params;
  const t = useTranslations('invoices');

  const { data: invoice, isLoading: isInvoiceLoading, error } = useInvoice(id);
  const { data: categories, isLoading: isCategoriesLoading } = useCategories();
  const { mutate: updateInvoice, isPending } = useUpdateInvoice();

  useEffect(() => {
    if (invoice && invoice.status !== 'PENDING') {
      toast.error(t('edit.pendingError'));
      router.push(`/invoices/${id}`);
    }
  }, [invoice, router, id, t]);

  if (isInvoiceLoading || isCategoriesLoading) {
    return <div className="flex justify-center items-center h-96"><Loader2 className="h-8 w-8 animate-spin" /></div>;
  }

  if (error || !invoice) {
    return <div className="text-center py-10">{t('notFound')}</div>;
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
        <h1 className="text-3xl font-bold tracking-tight">{t('edit.title')}</h1>
        <p className="text-muted-foreground">{t('edit.subtitle', { invoiceNumber: invoice.invoiceNumber })}</p>
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
