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
  Loader2,
  GitCompare
} from 'lucide-react';
import { format } from 'date-fns';
import { tr, enUS } from 'date-fns/locale';
import { useQuery } from '@tanstack/react-query';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
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
import { versionService } from '@/services/version-service';
import { InvoiceVersionSummary, VersionDiff } from '@/types/version-history';
import dynamic from 'next/dynamic';

const VersionTimeline = dynamic(
  () => import('@/components/invoice/VersionTimeline').then((mod) => mod.VersionTimeline),
  {
    loading: () => <div className="flex justify-center p-8"><Loader2 className="animate-spin" /></div>,
    ssr: false
  }
);
const VersionDiffViewer = dynamic(
  () => import('@/components/invoice/VersionDiffViewer').then((mod) => mod.VersionDiffViewer),
  {
    loading: () => <div className="flex justify-center p-8"><Loader2 className="animate-spin" /></div>,
    ssr: false
  }
);
import { RevertDialog } from '@/components/invoice/RevertDialog';
import { useLocale, useTranslations } from 'next-intl';

interface InvoiceDetailPageProps {
  params: {
    id: string;
  };
}

export default function InvoiceDetailPage({ params }: InvoiceDetailPageProps) {
  const t = useTranslations('invoices');
  const tCommon = useTranslations('common');
  const locale = useLocale();
  const dateLocale = locale === 'tr' ? tr : enUS;

  const statusConfig: Record<InvoiceStatus, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
    PENDING: { label: t('status.pending'), variant: 'secondary' },
    PROCESSING: { label: t('status.processing'), variant: 'outline' },
    VERIFIED: { label: t('status.verified'), variant: 'default' },
    REJECTED: { label: t('status.rejected'), variant: 'destructive' },
    QUEUED: { label: 'Queued', variant: 'secondary' },
    FAILED: { label: 'Failed', variant: 'destructive' },
    REVIEW_REQUIRED: { label: 'Review Required', variant: 'outline' },
  };

  const router = useRouter();
  const { id } = params;
  const { user } = useAuthStore();
  const isAdminOrManager = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const isAccountant = user?.role === 'ACCOUNTANT';
  const canEdit = isAdminOrManager || isAccountant;
  const canVerify = isAdminOrManager || isAccountant;

  const { data: invoice, isLoading, error } = useInvoice(id);

  // Version History State
  const [activeTab, setActiveTab] = useState('details');
  const [compareMode, setCompareMode] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState<InvoiceVersionSummary | null>(null);
  const [compareVersion, setCompareVersion] = useState<InvoiceVersionSummary | null>(null);
  const [diffData, setDiffData] = useState<VersionDiff | null>(null);
  const [isRevertDialogOpen, setIsRevertDialogOpen] = useState(false);
  const [versionToRevert, setVersionToRevert] = useState<InvoiceVersionSummary | null>(null);

  // Queries
  const { data: versions, refetch: refetchVersions } = useQuery({
    queryKey: ['invoiceVersions', id],
    queryFn: () => versionService.getVersions(id),
    enabled: activeTab === 'history'
  });

  const { data: diff, isLoading: isDiffLoading } = useQuery({
    queryKey: ['versionDiff', id, compareVersion?.versionNumber, selectedVersion?.versionNumber],
    queryFn: () => versionService.getVersionDiff(
      id,
      compareVersion!.versionNumber,
      selectedVersion!.versionNumber
    ),
    enabled: !!compareVersion && !!selectedVersion && compareMode
  });

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

  const handleCompare = (version: InvoiceVersionSummary) => {
    // Determine latest version from list or current invoice
    const latestVersion = versions?.find(v => v.versionNumber === Math.max(...versions.map(v => v.versionNumber)));

    setCompareVersion(version); // Old version
    setSelectedVersion(latestVersion || version); // New/Current version
    setCompareMode(true);
  };

  const handleRevertRequest = (version: InvoiceVersionSummary) => {
    setVersionToRevert(version);
    setIsRevertDialogOpen(true);
  };

  if (isLoading) {
    return <div className="flex justify-center items-center h-96"><Loader2 className="h-8 w-8 animate-spin" /></div>;
  }

  if (error || !invoice) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-xl text-muted-foreground">{t('notFound')}</p>
        <Button variant="outline" onClick={() => router.push('/invoices')}>{t('backToList')}</Button>
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
                <CheckCircle className="mr-2 h-4 w-4" /> {t('actions.verify')}
              </Button>
              <Button variant="destructive" onClick={() => setIsRejectOpen(true)}>
                <XCircle className="mr-2 h-4 w-4" /> {t('actions.reject')}
              </Button>
            </>
          )}

          {invoice.status === 'REJECTED' && isAdminOrManager && (
            <Button variant="outline" onClick={handleReopen}>
              <RotateCcw className="mr-2 h-4 w-4" /> {t('actions.reopen')}
            </Button>
          )}

          {invoice.status === 'PENDING' && canEdit && (
            <Button variant="outline" onClick={() => router.push(`/invoices/${id}/edit`)}>
              <Pencil className="mr-2 h-4 w-4" /> {t('actions.edit')}
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
          <strong>{t('reject.reasonLabel')}:</strong> {invoice.rejectionReason}
        </div>
      )}

      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="grid w-full grid-cols-2 lg:w-[400px]">
          <TabsTrigger value="details">{t('detail.tabs.details')}</TabsTrigger>
          <TabsTrigger value="history">{t('detail.tabs.history')}</TabsTrigger>
        </TabsList>

        <TabsContent value="details" className="mt-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Main Content - Left */}
            <div className="md:col-span-2 space-y-6">
              {/* Invoice Info */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">{t('detail.invoiceInfo')}</CardTitle>
                </CardHeader>
                <CardContent className="grid grid-cols-2 gap-y-4 text-sm">
                  <div className="space-y-1">
                    <span className="text-muted-foreground flex items-center gap-1"><CalendarIcon className="h-3 w-3" /> {t('table.date')}</span>
                    <span className="font-medium block">{format(new Date(invoice.invoiceDate), 'd MMMM yyyy', { locale: dateLocale })}</span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-muted-foreground flex items-center gap-1"><CalendarIcon className="h-3 w-3" /> {t('table.dueDate')}</span>
                    <span className="font-medium block">
                      {invoice.dueDate ? format(new Date(invoice.dueDate), 'd MMMM yyyy', { locale: dateLocale }) : '-'}
                    </span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-muted-foreground flex items-center gap-1"><CreditCard className="h-3 w-3" /> {t('table.currency')}</span>
                    <span className="font-medium block">{invoice.currency}
                      {invoice.exchangeRate && invoice.exchangeRate !== 1 ? ` (${t('detail.exchangeRate')}: ${invoice.exchangeRate})` : ''}
                    </span>
                  </div>
                  <div className="space-y-1">
                    <span className="text-muted-foreground flex items-center gap-1"><FileText className="h-3 w-3" /> {t('table.category')}</span>
                    <span className="font-medium block">{invoice.categoryName || '-'}</span>
                  </div>
                  {invoice.sourceType === 'LLM' && (
                    <div className="space-y-1">
                      <span className="text-muted-foreground">{t('table.source')} / {t('table.confidence')}</span>
                      <span className="font-medium block">{invoice.llmProvider} / %{Math.round((invoice.confidenceScore || 0) * 100)}</span>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Items Table */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">{t('detail.items')}</CardTitle>
                </CardHeader>
                <CardContent>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[40px]">#</TableHead>
                        <TableHead>{t('detail.itemsTable.description')}</TableHead>
                        <TableHead className="text-right">{t('detail.itemsTable.quantity')}</TableHead>
                        <TableHead className="text-right">{t('detail.itemsTable.unitPrice')}</TableHead>
                        <TableHead className="text-right">{t('detail.itemsTable.tax')}</TableHead>
                        <TableHead className="text-right">{t('detail.itemsTable.total')}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {invoice.items.map((item, index) => (
                        <TableRow key={item.id || index}>
                          <TableCell>{index + 1}</TableCell>
                          <TableCell className="font-medium">
                            {item.description}
                            {item.productCode && <div className="text-xs text-muted-foreground">{t('detail.itemsTable.code')}: {item.productCode}</div>}
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
                      <span className="text-muted-foreground">{t('detail.subtotal')}:</span>
                      <span>{formatCurrency(invoice.subtotal, invoice.currency)}</span>
                    </div>
                    <div className="flex justify-between w-[200px]">
                      <span className="text-muted-foreground">{t('detail.taxAmount')}:</span>
                      <span>{formatCurrency(invoice.taxAmount, invoice.currency)}</span>
                    </div>
                    <div className="flex justify-between w-[200px] font-bold text-lg mt-2">
                      <span>{t('detail.totalAmount')}:</span>
                      <span className="text-primary">{formatCurrency(invoice.totalAmount, invoice.currency)}</span>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {invoice.notes && (
                <Card>
                  <CardHeader>
                    <CardTitle className="text-lg">{t('detail.notes')}</CardTitle>
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
                  <CardTitle className="text-lg">{t('detail.supplier.title')}</CardTitle>
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
                      <span className="text-muted-foreground">{t('detail.supplier.taxNo')}:</span>
                      <span>{invoice.supplierTaxNumber}</span>
                    </div>
                  )}
                  {invoice.supplierTaxOffice && (
                    <div className="grid grid-cols-2 gap-1">
                      <span className="text-muted-foreground">{t('detail.supplier.taxOffice')}:</span>
                      <span>{invoice.supplierTaxOffice}</span>
                    </div>
                  )}
                  {invoice.supplierPhone && (
                    <div className="grid grid-cols-2 gap-1">
                      <span className="text-muted-foreground">{t('detail.supplier.phone')}:</span>
                      <span>{invoice.supplierPhone}</span>
                    </div>
                  )}
                  {invoice.supplierEmail && (
                    <div className="grid grid-cols-2 gap-1">
                      <span className="text-muted-foreground">{t('detail.supplier.email')}:</span>
                      <span className="truncate" title={invoice.supplierEmail}>{invoice.supplierEmail}</span>
                    </div>
                  )}
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">{t('detail.record.title')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3 text-sm">
                  <div className="flex items-center gap-2">
                    <User className="h-4 w-4 text-muted-foreground" />
                    <div>
                      <p className="text-xs text-muted-foreground">{t('detail.record.created')}</p>
                      <p className="font-medium">{invoice.createdByUserName}</p>
                      <p className="text-xs text-muted-foreground">{format(new Date(invoice.createdAt), 'd MMM yyyy HH:mm', { locale: dateLocale })}</p>
                    </div>
                  </div>

                  {invoice.verifiedByUserName && (
                    <div className="flex items-center gap-2 pt-3 border-t">
                      <CheckCircle className="h-4 w-4 text-green-600" />
                      <div>
                        <p className="text-xs text-muted-foreground">{t('detail.record.verified')}</p>
                        <p className="font-medium">{invoice.verifiedByUserName}</p>
                        <p className="text-xs text-muted-foreground">
                          {invoice.verifiedAt ? format(new Date(invoice.verifiedAt), 'd MMM yyyy HH:mm', { locale: dateLocale }) : '-'}
                        </p>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="history" className="mt-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-1">
              <VersionTimeline
                versions={versions || []}
                currentVersion={selectedVersion?.versionNumber}
                onCompare={handleCompare}
                onRevertRequest={handleRevertRequest}
                canRevert={isAdminOrManager}
              />
            </div>
            <div className="lg:col-span-2">
              {compareMode && compareVersion && selectedVersion ? (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-medium">{t('detail.history.compareTitle')}</h3>
                    <Button variant="ghost" size="sm" onClick={() => setCompareMode(false)}>
                      {t('actions.close')}
                    </Button>
                  </div>
                  {isDiffLoading ? (
                    <div className="flex justify-center p-8"><Loader2 className="animate-spin" /></div>
                  ) : diff ? (
                    <VersionDiffViewer
                      diff={diff}
                      previousVersionNum={compareVersion.versionNumber}
                      currentVersionNum={selectedVersion.versionNumber}
                    />
                  ) : (
                    <p>{t('detail.history.diffError')}</p>
                  )}
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center h-[400px] border border-dashed rounded-lg bg-muted/20 p-8 text-center text-muted-foreground">
                  <GitCompare className="h-12 w-12 mb-4 opacity-20" />
                  <h3 className="text-lg font-medium mb-2">{t('detail.history.emptyTitle')}</h3>
                  <p className="max-w-[300px]">
                    {t('detail.history.emptyDescription')}
                  </p>
                </div>
              )}
            </div>
          </div>
        </TabsContent>
      </Tabs>

      {/* Reject Dialog */}
      <Dialog open={isRejectOpen} onOpenChange={setIsRejectOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('reject.title')}</DialogTitle>
            <DialogDescription>
              {t('reject.description')}
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <Textarea
              placeholder={t('reject.reasonPlaceholder')}
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsRejectOpen(false)}>{tCommon('cancel')}</Button>
            <Button variant="destructive" onClick={handleReject} disabled={!rejectionReason}>{t('actions.reject')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Alert */}
      <AlertDialog open={isDeleteOpen} onOpenChange={setIsDeleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{tCommon('areYouSure')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('delete.description')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{tCommon('cancel')}</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive hover:bg-destructive/90">
              {tCommon('delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Revert Dialog */}
      <RevertDialog
        invoiceId={id}
        version={versionToRevert}
        open={isRevertDialogOpen}
        onOpenChange={setIsRevertDialogOpen}
        onSuccess={() => {
          refetchVersions();
          // Also invalidate invoice query would be good, but router refresh also works cleanly
          // queryClient.invalidateQueries(['invoice', id]);
          window.location.reload();
        }}
      />
    </div>
  );
}
