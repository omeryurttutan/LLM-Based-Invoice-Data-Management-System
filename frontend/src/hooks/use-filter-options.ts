import { useQuery } from '@tanstack/react-query';
import { invoiceService } from '@/services/invoice-service';

export function useFilterOptions() {
  return useQuery({
    queryKey: ['invoice-filter-options'],
    queryFn: invoiceService.getFilterOptions,
    staleTime: 1000 * 60 * 5, // 5 minutes
    retry: 1
  });
}
