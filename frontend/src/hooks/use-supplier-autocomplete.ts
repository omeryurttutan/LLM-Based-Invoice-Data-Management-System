import { useQuery } from '@tanstack/react-query';
import { invoiceService } from '@/services/invoice-service';
import { useState } from 'react';
import { useDebounce } from 'use-debounce';

export function useSupplierAutocomplete() {
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm] = useDebounce(searchTerm, 300);

  const query = useQuery({
    queryKey: ['supplier-autocomplete', debouncedSearchTerm],
    queryFn: () => invoiceService.getSuppliers(debouncedSearchTerm),
    enabled: debouncedSearchTerm.length >= 2,
    staleTime: 1000 * 60, // 1 minute
  });

  return {
    searchTerm,
    setSearchTerm,
    suppliers: query.data || [],
    isLoading: query.isLoading,
  };
}
