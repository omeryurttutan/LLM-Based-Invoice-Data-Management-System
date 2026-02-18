"use client";

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { notFound, useRouter } from 'next/navigation';
import { invoiceService } from '@/services/invoice-service';

import dynamic from 'next/dynamic';
import { Loader2 } from 'lucide-react';

const VerificationLayout = dynamic(
  () => import('@/components/invoice/verification/verification-layout').then((mod) => mod.VerificationLayout),
  {
    ssr: false,
    loading: () => <div className="flex h-screen items-center justify-center"><Loader2 className="h-8 w-8 animate-spin" /></div>
  }
);
const DocumentViewer = dynamic(
  () => import('@/components/invoice/verification/document-viewer').then((mod) => mod.DocumentViewer),
  { ssr: false }
);
const VerificationForm = dynamic(
  () => import('@/components/invoice/verification/verification-form').then((mod) => mod.VerificationForm),
  { ssr: false }
);
import { Button } from '@/components/ui/button';

interface VerifyPageProps {
  params: {
    id: string;
  };
}

export default function VerifyPage({ params }: VerifyPageProps) {
  const router = useRouter();
  const { id } = params;

  const { data: invoice, isLoading, error } = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => invoiceService.getInvoice(id),
    retry: 1,
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-screen gap-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="text-gray-500">Fatura verileri yükleniyor...</p>
      </div>
    );
  }

  if (error) {
     const is404 = (error as any)?.response?.status === 404;
     if (is404) {
         notFound();
     }

    return (
      <div className="flex flex-col items-center justify-center h-screen gap-4">
        <p className="text-destructive font-medium">Fatura yüklenirken bir hata oluştu.</p>
        <Button onClick={() => window.location.reload()} variant="outline">
          Tekrar Dene
        </Button>
        <Button onClick={() => router.push('/invoices')} variant="link">
          Listeye Dön
        </Button>
      </div>
    );
  }

  if (!invoice) {
    return null; // Should be handled by error or loading
  }

  return (
    <VerificationLayout
      documentPanel={<DocumentViewer invoiceId={id} />}
      formPanel={<VerificationForm invoice={invoice} />}
    />
  );
}
