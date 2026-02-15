"use client";

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  Plus,
  MoreHorizontal,
  Eye,
  Pencil,
  CheckCircle,
  XCircle,
  RotateCcw,
  Trash2,
  Loader2,
  Upload,
  Download
} from 'lucide-react';
import { format } from 'date-fns';
import { tr, enUS } from 'date-fns/locale';

import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Badge } from '@/components/ui/badge';
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

import { useInvoices, useDeleteInvoice, useVerifyInvoice, useRejectInvoice, useReopenInvoice } from '@/hooks/use-invoices';
import { InvoiceStatus, InvoiceListItem } from '@/types/invoice';
import { useAuthStore } from '@/stores/auth-store';
import { formatCurrency } from '@/lib/utils';
import { DataTablePagination } from '@/components/common/data-table-pagination';

import { useInvoiceFilters } from '@/hooks/use-invoice-filters';
import { FilterPanel } from '@/components/invoices/filter-panel';
import { SearchBar } from '@/components/invoices/search-bar';
import { ActiveFilters } from '@/components/invoices/active-filters';
import { ExportDialog } from '@/components/invoices/export-dialog';
import { useTranslations, useFormatter } from 'next-intl';

export default function InvoicesPage() {
  const t = useTranslations('invoices');
  const tCommon = useTranslations('common');
  const format = useFormatter();

  const statusConfig: Record<InvoiceStatus, { label: string; variant: "default" | "secondary" | "destructive" | "outline" }> = {
    PENDING: { label: t('status.pending'), variant: 'secondary' },
    PROCESSING: { label: t('status.processing'), variant: 'outline' },
    VERIFIED: { label: t('status.verified'), variant: 'default' },
    REJECTED: { label: t('status.rejected'), variant: 'destructive' },
  };

  const router = useRouter();
  const { user } = useAuthStore();
  const isAdminOrManager = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const isAccountant = user?.role === 'ACCOUNTANT';
  const canEdit = isAdminOrManager || isAccountant;
  const canVerify = isAdminOrManager || isAccountant;
  const canExport = isAdminOrManager || isAccountant;

  const { filters, setFilter, setFilters } = useInvoiceFilters();

  const { data: invoicesData, isLoading } = useInvoices(filters);

  // Actions
  const deleteMutation = useDeleteInvoice();
  const verifyMutation = useVerifyInvoice();
  const rejectMutation = useRejectInvoice();
  const reopenMutation = useReopenInvoice();

  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [isExportDialogOpen, setIsExportDialogOpen] = useState(false);

  // Keyboard shortcut for Export
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
        e.preventDefault();
        if (canExport) {
          setIsExportDialogOpen(true);
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [canExport]);

  const handleDelete = () => {
    if (deletingId) {
      deleteMutation.mutate(deletingId, {
        onSuccess: () => setDeletingId(null)
      });
    }
  };

  const handleReject = () => {
    if (rejectingId && rejectionReason) {
      rejectMutation.mutate({ id: rejectingId, data: { rejectionReason } }, {
        onSuccess: () => {
          setRejectingId(null);
          setRejectionReason('');
        }
      });
    }
  };

  const handleVerify = (id: string) => {
    verifyMutation.mutate({ id });
  };

  const handleReopen = (id: string) => {
    reopenMutation.mutate(id);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">{t('title')}</h1>
        <div className="flex gap-2">

          {/* Export Button */}
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <div className="inline-block">
                  <Button
                    variant="outline"
                    onClick={() => setIsExportDialogOpen(true)}
                    disabled={!canExport}
                  >
                    <Download className="mr-2 h-4 w-4" /> {t('export')}
                  </Button>
                </div>
              </TooltipTrigger>
              {!canExport && (
                <TooltipContent>
                  <p>{t('noExportPermission')}</p>
                </TooltipContent>
              )}
            </Tooltip>
          </TooltipProvider>

          <Button variant="outline" onClick={() => router.push('/invoices/upload')}>
            <Upload className="mr-2 h-4 w-4" /> {t('uploadInvoice')}
          </Button>
          <Button onClick={() => router.push('/invoices/new')}>
            <Plus className="mr-2 h-4 w-4" /> {t('newInvoice')}
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <SearchBar />
          <Select value={filters.sort || 'createdAt,desc'} onValueChange={(v) => setFilter('sort', v)}>
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder={t('sort.label')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="createdAt,desc">{t('sort.newest')}</SelectItem>
              <SelectItem value="createdAt,asc">{t('sort.oldest')}</SelectItem>
              <SelectItem value="invoiceDate,desc">{t('sort.dateDesc')}</SelectItem>
              <SelectItem value="invoiceDate,asc">{t('sort.dateAsc')}</SelectItem>
              <SelectItem value="totalAmount,desc">{t('sort.amountDesc')}</SelectItem>
              <SelectItem value="totalAmount,asc">{t('sort.amountAsc')}</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <ActiveFilters />
        <FilterPanel />
      </div>

      {/* Table */}
      <div className="border rounded-md">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('table.invoiceNumber')}</TableHead>
              <TableHead>{t('table.supplier')}</TableHead>
              <TableHead>{t('table.date')}</TableHead>
              <TableHead>{t('table.amount')}</TableHead>
              <TableHead>{t('table.category')}</TableHead>
              <TableHead>{t('table.status')}</TableHead>
              <TableHead className="text-right">{t('table.actions')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} className="h-24 text-center">
                  <Loader2 className="mx-auto h-8 w-8 animate-spin" />
                </TableCell>
              </TableRow>
            ) : invoicesData?.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="h-24 text-center">
                  {Object.keys(filters).length > 2 ? ( // page and size always present
                    <div className="flex flex-col items-center gap-2">
                      <p>{t('empty.filtered')}</p>
                      <Button variant="outline" size="sm" onClick={() => setFilters({})}>{t('filter.clear')}</Button>
                    </div>
                  ) : (
                    t('empty.title')
                  )}
                </TableCell>
              </TableRow>
            ) : (
              invoicesData?.content.map((invoice: InvoiceListItem) => (
                <TableRow key={invoice.id} className="cursor-pointer hover:bg-muted/50" onClick={() => router.push(`/invoices/${invoice.id}`)}>
                  <TableCell className="font-medium">{invoice.invoiceNumber}</TableCell>
                  <TableCell>{invoice.supplierName}</TableCell>
                  <TableCell>{format.dateTime(new Date(invoice.invoiceDate), { dateStyle: 'medium' })}</TableCell>
                  <TableCell>{format.number(invoice.totalAmount, { style: 'currency', currency: invoice.currency })}</TableCell>
                  <TableCell>
                    {invoice.categoryName ? (
                      <Badge variant="outline">{invoice.categoryName}</Badge>
                    ) : (
                      <span className="text-muted-foreground text-xs">-</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusConfig[invoice.status].variant}>
                      {statusConfig[invoice.status].label}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                          <span className="sr-only">{t('actions.openMenu')}</span>
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuLabel>{t('table.actions')}</DropdownMenuLabel>
                        <DropdownMenuItem onClick={() => router.push(`/invoices/${invoice.id}`)}>
                          <Eye className="mr-2 h-4 w-4" /> {t('actions.view')}
                        </DropdownMenuItem>

                        {invoice.status === 'PENDING' && canEdit && (
                          <DropdownMenuItem onClick={() => router.push(`/invoices/${invoice.id}/edit`)}>
                            <Pencil className="mr-2 h-4 w-4" /> {t('actions.edit')}
                          </DropdownMenuItem>
                        )}

                        <DropdownMenuSeparator />

                        {invoice.status === 'PENDING' && canVerify && (
                          <>
                            <DropdownMenuItem onClick={() => handleVerify(invoice.id)}>
                              <CheckCircle className="mr-2 h-4 w-4 text-green-600" /> {t('actions.verify')}
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => setRejectingId(invoice.id)}>
                              <XCircle className="mr-2 h-4 w-4 text-destructive" /> {t('actions.reject')}
                            </DropdownMenuItem>
                          </>
                        )}

                        {invoice.status === 'REJECTED' && isAdminOrManager && (
                          <DropdownMenuItem onClick={() => handleReopen(invoice.id)}>
                            <RotateCcw className="mr-2 h-4 w-4 text-orange-600" /> {t('actions.reopen')}
                          </DropdownMenuItem>
                        )}

                        {invoice.status !== 'VERIFIED' && isAdminOrManager && (
                          <DropdownMenuItem onClick={() => setDeletingId(invoice.id)} className="text-destructive">
                            <Trash2 className="mr-2 h-4 w-4" /> {t('actions.delete')}
                          </DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {invoicesData && (
        <DataTablePagination
          pageIndex={invoicesData.page.number}
          pageSize={invoicesData.page.size}
          totalElements={invoicesData.page.totalElements}
          totalPages={invoicesData.page.totalPages}
          onPageChange={(p) => setFilter('page', p)}
          onPageSizeChange={(s) => setFilter('size', s)}
        />
      )}

      {/* Reject Dialog */}
      <Dialog open={!!rejectingId} onOpenChange={(open) => !open && setRejectingId(null)}>
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
            <Button variant="outline" onClick={() => setRejectingId(null)}>{tCommon('cancel')}</Button>
            <Button variant="destructive" onClick={handleReject} disabled={!rejectionReason}>{t('actions.reject')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Alert */}
      <AlertDialog open={!!deletingId} onOpenChange={(open) => !open && setDeletingId(null)}>
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

      {/* Export Dialog */}
      <ExportDialog
        open={isExportDialogOpen}
        onOpenChange={setIsExportDialogOpen}
        totalCount={invoicesData?.page.totalElements || 0}
      />
    </div>
  );
}
