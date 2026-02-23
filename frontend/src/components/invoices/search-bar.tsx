'use client';

import { Input } from '@/components/ui/input';
import { Search, X } from 'lucide-react';
import { useInvoiceFilters } from '@/hooks/use-invoice-filters';
import { useEffect, useState, useCallback } from 'react';
import { useDebounce } from 'use-debounce';
import { useTranslations } from 'next-intl';

export function SearchBar() {
  const t = useTranslations('invoices.filters');
  const { filters, setFilters } = useInvoiceFilters();
  const [searchTerm, setSearchTerm] = useState(filters.search || '');
  const [debouncedSearchTerm] = useDebounce(searchTerm, 400);

  // Sync local state with URL param (in case URL changes via other means)
  useEffect(() => {
    setSearchTerm(filters.search || '');
  }, [filters.search]);

  // Apply search when debounced value changes
  useEffect(() => {
    if (debouncedSearchTerm !== filters.search) {
      setFilters({ ...filters, search: debouncedSearchTerm || undefined });
    }
  }, [debouncedSearchTerm, filters, setFilters]);

  const handleClear = useCallback(() => {
    setSearchTerm('');
    setFilters({ ...filters, search: undefined });
  }, [filters, setFilters]);

  return (
    <div className='relative w-full max-w-xl'>
      <Search className='absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground' />
      <Input
        placeholder={t('searchPlaceholder')}
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        className='pl-9 pr-9'
      />
      {searchTerm && (
        <button
          onClick={handleClear}
          className='absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground'
        >
          <X className='h-4 w-4' />
        </button>
      )}
    </div>
  );
}
