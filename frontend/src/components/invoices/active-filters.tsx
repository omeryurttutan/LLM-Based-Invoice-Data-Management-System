'use client';

import { useInvoiceFilters } from '@/hooks/use-invoice-filters';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { X } from 'lucide-react';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';

export function ActiveFilters() {
  const { filters, setFilters, clearFilters, activeFilterCount } = useInvoiceFilters();

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
      <span className='text-sm text-muted-foreground'>Aktif Filtreler:</span>
      
      {filters.status && renderFilterChip('status', filters.status, 'Durum')}
      {filters.dateFrom && renderFilterChip('dateFrom', format(new Date(filters.dateFrom), 'dd.MM.yyyy'), 'Başlangıç')}
      {filters.dateTo && renderFilterChip('dateTo', format(new Date(filters.dateTo), 'dd.MM.yyyy'), 'Bitiş')}
      {filters.amountMin && renderFilterChip('amountMin', filters.amountMin, 'Min Tutar')}
      {filters.amountMax && renderFilterChip('amountMax', filters.amountMax, 'Max Tutar')}
      {filters.supplierName && renderFilterChip('supplierName', filters.supplierName, 'Tedarikçi')}
      {filters.currency && renderFilterChip('currency', filters.currency, 'Para Birimi')}
      {filters.sourceType && renderFilterChip('sourceType', filters.sourceType, 'Kaynak')}
      
      <Button 
        variant='ghost' 
        size='sm' 
        onClick={clearFilters}
        className='h-7 px-2 text-xs text-muted-foreground hover:text-destructive'
      >
        Tümünü Temizle
      </Button>
    </div>
  );
}
