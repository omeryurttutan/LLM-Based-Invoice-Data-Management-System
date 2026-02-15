'use client';

import { useRouter } from 'next/navigation';
import { useCreateInvoice } from '@/hooks/use-invoices';
import { useCategories } from '@/hooks/use-categories';
import { InvoiceForm } from '@/components/invoice/invoice-form';
import { Loader2 } from 'lucide-react';

export default function CreateInvoicePage() {
  const router = useRouter();
  const { mutate: createInvoice, isPending } = useCreateInvoice();
  const { data: categories, isLoading: isCategoriesLoading } = useCategories();

  const handleSubmit = (data: any) => {
    createInvoice(data, {
       onSuccess: (response) => {
         router.push(`/invoices/${response.id}`);
       }
    });
  };

  if (isCategoriesLoading) {
     return <div className="flex justify-center items-center h-96"><Loader2 className="h-8 w-8 animate-spin" /></div>;
  }

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-10">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Yeni Fatura Oluştur</h1>
        <p className="text-muted-foreground">Fatura bilgilerini girerek yeni bir kayıt oluşturun.</p>
      </div>

      <InvoiceForm 
        categories={categories || []} 
        onSubmit={handleSubmit} 
        isLoading={isPending} 
      />
    </div>
  );
}
