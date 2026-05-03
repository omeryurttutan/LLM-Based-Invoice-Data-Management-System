import { useSearchParams, useRouter, usePathname } from 'next/navigation';
import { useCallback, useMemo } from 'react';
import { InvoiceListParams, InvoiceStatus, SourceType, LlmProvider, Currency } from '@/types/invoice';

export function useInvoiceFilters() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  // Parse current filters from URL
  const filters: InvoiceListParams = useMemo(() => {
    const params: InvoiceListParams = {};
    searchParams.forEach((value, key) => {
      if (value) {
        if (key === 'page' || key === 'size' || key === 'amountMin' || key === 'amountMax' || key === 'confidenceMin' || key === 'confidenceMax') {
          params[key as keyof InvoiceListParams] = Number(value) as any;
        } else {
          params[key as keyof InvoiceListParams] = value as any;
        }
      }
    });
    return params;
  }, [searchParams]);

  // Update a single filter
  const setFilter = useCallback((key: keyof InvoiceListParams, value: any) => {
    const params = new URLSearchParams(searchParams.toString());
    
    if (value === null || value === undefined || value === '') {
      params.delete(key);
    } else {
      params.set(key, String(value));
    }
    
    // Reset page to 0 on filter change (except pagination changes)
    if (key !== 'page') {
      params.set('page', '0');
    }

    router.replace(`${pathname}?${params.toString()}`);
  }, [searchParams, router, pathname]);

  // Update multiple filters at once
  const setFilters = useCallback((newFilters: Partial<InvoiceListParams>) => {
    const params = new URLSearchParams(searchParams.toString());
    
    Object.entries(newFilters).forEach(([key, value]) => {
      if (value === null || value === undefined || value === '') {
        params.delete(key);
      } else {
        params.set(key, String(value));
      }
    });

    // Reset page to 0
    params.set('page', '0');

    router.replace(`${pathname}?${params.toString()}`);
  }, [searchParams, router, pathname]);

  // Clear all filters
  const clearFilters = useCallback(() => {
    const params = new URLSearchParams();
    // Keep defaults if needed, or just clear everything
    params.set('page', '0');
    params.set('size', '20');
    params.set('sort', 'createdAt,desc');
    router.replace(`${pathname}?${params.toString()}`);
  }, [router, pathname]);

  const activeFilterCount = useMemo(() => {
    let count = 0;
    searchParams.forEach((_, key) => {
      if (!['page', 'size', 'sort'].includes(key)) {
        count++;
      }
    });
    return count;
  }, [searchParams]);

  return {
    filters,
    setFilter,
    setFilters,
    clearFilters,
    activeFilterCount
  };
}
