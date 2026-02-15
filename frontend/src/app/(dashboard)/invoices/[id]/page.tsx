'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { 
  ArrowLeft, 
  Calendar as CalendarIcon, 
  CreditCard, 
  FileText, 
  Building2, 
  User, 
  CheckCircle,
  XCircle,
  RotateCcw,
  Trash2,
  Pencil,
  Loader2
} from 'lucide-react';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

import { useInvoice, useDeleteInvoice, useVerifyInvoice, useRejectInvoice, useReopenInvoice } from '@/hooks/use-invoices';
import { InvoiceStatus } from '@/types/invoice';
import { useAuthStore } from '@/stores/auth-store';
import { formatCurrency } from '@/lib/utils';

const statusConfig: Record<InvoiceStatus, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
  PENDING: { label: 'Beklemede', variant: 'secondary' },
  PROCESSING: { label: 'İşleniyor', variant: 'outline' },
  VERIFIED: { label: 'Onaylı', variant: 'default' },
  REJECTED: { label: 'Reddedildi', variant: 'destructive' },
};

interface InvoiceDetailPageProps {
  params: {
    id: string;
  };
}

export default function InvoiceDetailPage({ params }: InvoiceDetailPageProps) {
  const router = useRouter();
  const { id } = params;
  const { user } = useAuthStore();
  const isAdminOrManager = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const isAccountant = user?.role === 'ACCOUNTANT';
  const canEdit = isAdminOrManager || isAccountant;
  const canVerify = isAdminOrManager || isAccountant;

  const { data: invoice, isLoading, error } = useInvoice(id);

  // Actions
  const deleteMutation = useDeleteInvoice();
  const verifyMutation = useVerifyInvoice();
  const rejectMutation = useRejectInvoice();
  const reopenMutation = useReopenInvoice();

  const [isRejectOpen, setIsRejectOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');

  const handleDelete = () => {
    deleteMutation.mutate(id, {
      onSuccess: () => router.push('/invoices')
    });
  };

  const handleReject = () => {
    if (rejectionReason) {
      rejectMutation.mutate({ id, data: { rejectionReason } }, {
        onSuccess: () => setIsRejectOpen(false)
      });
    }
  };

  const handleVerify = () => {
    verifyMutation.mutate({ id });
  };

  const handleReopen = () => {
    reopenMutation.mutate(id);
  };

  if (isLoading) {
    return <div className="flex justify-center items-center h-96"><Loader2 className="h-8 w-8 animate-spin" /></div>;
  }

  if (error || !invoice) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-xl text-muted-foreground">Fatura bulunamadı veya bir hata oluştu.</p>
        <Button variant="outline" onClick={() => router.push('/invoices')}>Listeye Dön</Button>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-10">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <Button variant="outline" size="icon" onClick={() => router.push('/invoices')}>
             <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold tracking-tight">{invoice.invoiceNumber}</h1>
              <Badge variant={statusConfig[invoice.status].variant}>
                {statusConfig[invoice.status].label}
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground">{invoice.supplierName}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
           {invoice.status === 'PENDING' && canVerify && (
              <>
                 <Button variant="default" className="bg-green-600 hover:bg-green-700" onClick={handleVerify}>
                    <CheckCircle className="mr-2 h-4 w-4" /> Onayla
                 </Button>
                 <Button variant="destructive" onClick={() => setIsRejectOpen(true)}>
                    <XCircle className="mr-2 h-4 w-4" /> Reddet
                 </Button>
              </>
           )}

           {invoice.status === 'REJECTED' && isAdminOrManager && (
              <Button variant="outline" onClick={handleReopen}>
                 <RotateCcw className="mr-2 h-4 w-4" /> Yeniden Aç
              </Button>
           )}

           {invoice.status === 'PENDING' && canEdit && (
              <Button variant="outline" onClick={() => router.push(`/invoices/${id}/edit`)}>
                 <Pencil className="mr-2 h-4 w-4" /> Düzenle
              </Button>
           )}

           {invoice.status !== 'VERIFIED' && isAdminOrManager && (
              <Button variant="ghost" className="text-destructive hover:bg-destructive/10" onClick={() => setIsDeleteOpen(true)}>
                 <Trash2 className="h-4 w-4" />
              </Button>
           )}
        </div>
      </div>

      {invoice.status === 'REJECTED' && invoice.rejectionReason && (
        <div className="bg-destructive/10 border border-destructive/20 rounded-md p-4 text-sm text-destructive">
          <strong>Red Nedeni:</strong> {invoice.rejectionReason}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Main Content - Left */}
        <div className="md:col-span-2 space-y-6">
          {/* Invoice Info */}
          <Card>
             <CardHeader>
               <CardTitle className="text-lg">Fatura Özeti</CardTitle>
             </CardHeader>
             <CardContent className="grid grid-cols-2 gap-y-4 text-sm">
                <div className="space-y-1">
                  <span className="text-muted-foreground flex items-center gap-1"><CalendarIcon className="h-3 w-3" /> Fatura Tarihi</span>
                  <span className="font-medium block">{format(new Date(invoice.invoiceDate), 'd MMMM yyyy', { locale: tr })}</span>
                </div>
                <div className="space-y-1">
                  <span className="text-muted-foreground flex items-center gap-1"><CalendarIcon className="h-3 w-3" /> Vade Tarihi</span>
                  <span className="font-medium block">
                    {invoice.dueDate ? format(new Date(invoice.dueDate), 'd MMMM yyyy', { locale: tr }) : '-'}
                  </span>
                </div>
                <div className="space-y-1">
                  <span className="text-muted-foreground flex items-center gap-1"><CreditCard className="h-3 w-3" /> Para Birimi</span>
                  <span className="font-medium block">{invoice.currency} 
                    {invoice.exchangeRate && invoice.exchangeRate !== 1 ? ` (Kur: ${invoice.exchangeRate})` : ''}
                  </span>
                </div>
                <div className="space-y-1">
                  <span className="text-muted-foreground flex items-center gap-1"><FileText className="h-3 w-3" /> Kategori</span>
                  <span className="font-medium block">{invoice.categoryName || '-'}</span>
                </div>
                {invoice.sourceType === 'LLM' && (
                  <div className="space-y-1">
                     <span className="text-muted-foreground">Kaynak / Güven Skoru</span>
                     <span className="font-medium block">{invoice.llmProvider} / %{Math.round((invoice.confidenceScore || 0) * 100)}</span>
                  </div>
                )}
             </CardContent>
          </Card>

          {/* Items Table */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Fatura Kalemleri</CardTitle>
            </CardHeader>
            <CardContent>
               <Table>
                 <TableHeader>
                   <TableRow>
                     <TableHead className="w-[40px]">#</TableHead>
                     <TableHead>Açıklama</TableHead>
                     <TableHead className="text-right">Miktar</TableHead>
                     <TableHead className="text-right">Birim Fiyat</TableHead>
                     <TableHead className="text-right">KDV</TableHead>
                     <TableHead className="text-right">Toplam</TableHead>
                   </TableRow>
                 </TableHeader>
                 <TableBody>
                   {invoice.items.map((item, index) => (
                     <TableRow key={item.id || index}>
                       <TableCell>{index + 1}</TableCell>
                       <TableCell className="font-medium">
                          {item.description}
                          {item.productCode && <div className="text-xs text-muted-foreground">Kod: {item.productCode}</div>}
                       </TableCell>
                       <TableCell className="text-right">{item.quantity} {item.unit}</TableCell>
                       <TableCell className="text-right">{formatCurrency(item.unitPrice, invoice.currency)}</TableCell>
                       <TableCell className="text-right">%{item.taxRate}</TableCell>
                       <TableCell className="text-right">{formatCurrency(item.totalAmount, invoice.currency)}</TableCell>
                     </TableRow>
                   ))}
                 </TableBody>
               </Table>
               
               <Separator className="my-4" />
               
               <div className="flex flex-col items-end gap-2 text-sm">
                 <div className="flex justify-between w-[200px]">
                   <span className="text-muted-foreground">Ara Toplam:</span>
                   <span>{formatCurrency(invoice.subtotal, invoice.currency)}</span>
                 </div>
                 <div className="flex justify-between w-[200px]">
                   <span className="text-muted-foreground">Toplam KDV:</span>
                   <span>{formatCurrency(invoice.taxAmount, invoice.currency)}</span>
                 </div>
                 <div className="flex justify-between w-[200px] font-bold text-lg mt-2">
                   <span>Genel Toplam:</span>
                   <span className="text-primary">{formatCurrency(invoice.totalAmount, invoice.currency)}</span>
                 </div>
               </div>
            </CardContent>
          </Card>

           {invoice.notes && (
             <Card>
               <CardHeader>
                 <CardTitle className="text-lg">Notlar</CardTitle>
               </CardHeader>
               <CardContent>
                 <p className="text-sm text-muted-foreground">{invoice.notes}</p>
               </CardContent>
             </Card>
           )}
        </div>

        {/* Sidebar - Right */}
        <div className="space-y-6">
           <Card>
             <CardHeader>
               <CardTitle className="text-lg">Tedarikçi Bilgileri</CardTitle>
             </CardHeader>
             <CardContent className="space-y-4 text-sm">
                <div className="flex items-start gap-2">
                  <Building2 className="h-4 w-4 mt-1 text-muted-foreground" />
                  <div>
                    <p className="font-medium">{invoice.supplierName}</p>
                    {invoice.supplierAddress && <p className="text-muted-foreground mt-1">{invoice.supplierAddress}</p>}
                  </div>
                </div>
                {invoice.supplierTaxNumber && (
                   <div className="grid grid-cols-2 gap-1 pt-2 border-t">
                      <span className="text-muted-foreground">Vergi No:</span>
                      <span>{invoice.supplierTaxNumber}</span>
                   </div>
                )}
                {invoice.supplierTaxOffice && (
                   <div className="grid grid-cols-2 gap-1">
                      <span className="text-muted-foreground">Vergi Dairesi:</span>
                      <span>{invoice.supplierTaxOffice}</span>
                   </div>
                )}
                {invoice.supplierPhone && (
                   <div className="grid grid-cols-2 gap-1">
                      <span className="text-muted-foreground">Telefon:</span>
                      <span>{invoice.supplierPhone}</span>
                   </div>
                )}
                {invoice.supplierEmail && (
                   <div className="grid grid-cols-2 gap-1">
                      <span className="text-muted-foreground">E-posta:</span>
                      <span className="truncate" title={invoice.supplierEmail}>{invoice.supplierEmail}</span>
                   </div>
                )}
             </CardContent>
           </Card>

           <Card>
             <CardHeader>
               <CardTitle className="text-lg">Kayıt Bilgileri</CardTitle>
             </CardHeader>
             <CardContent className="space-y-3 text-sm">
                <div className="flex items-center gap-2">
                   <User className="h-4 w-4 text-muted-foreground" />
                   <div>
                      <p className="text-xs text-muted-foreground">Oluşturan</p>
                      <p className="font-medium">{invoice.createdByUserName}</p>
                      <p className="text-xs text-muted-foreground">{format(new Date(invoice.createdAt), 'd MMM yyyy HH:mm', { locale: tr })}</p>
                   </div>
                </div>
                
                {invoice.verifiedByUserName && (
                   <div className="flex items-center gap-2 pt-3 border-t">
                     <CheckCircle className="h-4 w-4 text-green-600" />
                     <div>
                        <p className="text-xs text-muted-foreground">Onaylayan</p>
                        <p className="font-medium">{invoice.verifiedByUserName}</p>
                        <p className="text-xs text-muted-foreground">
                           {invoice.verifiedAt ? format(new Date(invoice.verifiedAt), 'd MMM yyyy HH:mm', { locale: tr }) : '-'}
                        </p>
                     </div>
                   </div>
                )}
             </CardContent>
           </Card>
        </div>
      </div>

       {/* Reject Dialog */}
      <Dialog open={isRejectOpen} onOpenChange={setIsRejectOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Faturayı Reddet</DialogTitle>
            <DialogDescription>
              Faturayı reddetmek üzeresiniz. Lütfen bir neden belirtin.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
             <Textarea 
               placeholder="Red nedeni..." 
               value={rejectionReason}
               onChange={(e) => setRejectionReason(e.target.value)}
             />
          </div>
          <DialogFooter>
             <Button variant="outline" onClick={() => setIsRejectOpen(false)}>İptal</Button>
             <Button variant="destructive" onClick={handleReject} disabled={!rejectionReason}>Reddet</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Alert */}
      <AlertDialog open={isDeleteOpen} onOpenChange={setIsDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
             <AlertDialogTitle>Emin misiniz?</AlertDialogTitle>
             <AlertDialogDescription>
               Bu faturayı silmek üzeresiniz. Bu işlem geri alınamaz.
             </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
             <AlertDialogCancel>İptal</AlertDialogCancel>
             <AlertDialogAction onClick={handleDelete} className="bg-destructive hover:bg-destructive/90">
               Sil
             </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
