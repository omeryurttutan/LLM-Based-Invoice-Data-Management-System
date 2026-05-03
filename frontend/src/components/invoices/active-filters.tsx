'use client';

import { useInvoiceFilters } from '@/hooks/use-invoice-filters';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { X } from 'lucide-react';
import { format } from 'date-fns';
import { useTranslations } from 'next-intl';

export function ActiveFilters() {
  const { filters, setFilters, clearFilters, activeFilterCount } = useInvoiceFilters();
  const t = useTranslations('invoices.filter');

  if (activeFilterCount === 0) return null;

  const removeFilter = (key: keyof typeof filters) => {
    setFilters({ ...filters, [key]: undefined });
  };

  const renderFilterChip = (key: string, value: any, label: string) => {
    if (!value) return null;
    return (
      <Badge variant='secondary' className='flex items-center gap-1 rounded-sm px-2 py-1'>
        <span className='font-normal text-muted-foreground'>{label}:</span>
        <span>{value}</span>
        <button
          onClick={() => removeFilter(key as any)}
          className='ml-1 rounded-full p-0.5 hover:bg-muted'
        >
          <X className='h-3 w-3' />
        </button>
      </Badge>
    );
  };

  return (
    <div className='flex flex-wrap items-center gap-2 pt-2'>
      <span className='text-sm text-muted-foreground'>{t('activeFilters')}:</span>

      {filters.status && renderFilterChip('status', filters.status, t('status'))}
      {filters.dateFrom && renderFilterChip('dateFrom', format(new Date(filters.dateFrom), 'dd.MM.yyyy'), t('startDate'))}
      {filters.dateTo && renderFilterChip('dateTo', format(new Date(filters.dateTo), 'dd.MM.yyyy'), t('endDate'))}
      {filters.amountMin && renderFilterChip('amountMin', filters.amountMin, t('minAmount'))}
      {filters.amountMax && renderFilterChip('amountMax', filters.amountMax, t('maxAmount'))}
      {filters.supplierName && renderFilterChip('supplierName', filters.supplierName, t('supplier'))}
      {filters.currency && renderFilterChip('currency', filters.currency, t('currency'))}
      {filters.sourceType && renderFilterChip('sourceType', filters.sourceType, t('sourceType'))}

      <Button
        variant='ghost'
        size='sm'
        onClick={clearFilters}
        className='h-7 px-2 text-xs text-muted-foreground hover:text-destructive'
      >
        {t('clearAll')}
      </Button>
    </div>
  );
}
