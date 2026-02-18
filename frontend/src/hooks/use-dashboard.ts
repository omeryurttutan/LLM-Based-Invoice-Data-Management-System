import { useQuery } from '@tanstack/react-query';
import * as dashboardService from '@/services/dashboard.service';

export const useDashboardStats = (params?: { dateFrom?: string; dateTo?: string; currency?: string }) => {
  return useQuery({
    queryKey: ['dashboard-stats', params],
    queryFn: () => dashboardService.getDashboardStats(params),
    staleTime: 60 * 1000,
  });
};

export const useCategoryDistribution = (params?: { dateFrom?: string; dateTo?: string; currency?: string }) => {
  return useQuery({
    queryKey: ['dashboard-categories', params],
    queryFn: () => dashboardService.getCategoryDistribution(params),
    staleTime: 5 * 60 * 1000,
  });
};

export const useMonthlyTrend = (params?: { months?: number; currency?: string }) => {
  return useQuery({
    queryKey: ['dashboard-trends', params],
    queryFn: () => dashboardService.getMonthlyTrend(params),
    staleTime: 60 * 1000,
  });
};

export const useTopSuppliers = (params?: { dateFrom?: string; dateTo?: string; currency?: string; limit?: number }) => {
  return useQuery({
    queryKey: ['dashboard-suppliers', params],
    queryFn: () => dashboardService.getTopSuppliers(params),
    staleTime: 60 * 1000,
  });
};

export const usePendingActions = (params?: { limit?: number }) => {
  return useQuery({
    queryKey: ['dashboard-pending', params],
    queryFn: () => dashboardService.getPendingActions(params),
    staleTime: 60 * 1000,
  });
};

export const useStatusTimeline = (params?: { days?: number }) => {
  return useQuery({
    queryKey: ['dashboard-timeline', params],
    queryFn: () => dashboardService.getStatusTimeline(params),
    staleTime: 60 * 1000,
  });
};

export const useExtractionPerformance = (params?: { dateFrom?: string; dateTo?: string }) => {
  return useQuery({
    queryKey: ['dashboard-extraction', params],
    queryFn: () => dashboardService.getExtractionPerformance(params),
    staleTime: 60 * 1000,
  });
};

export const useSystemStatus = (enabled: boolean = false) => {
  return useQuery({
    queryKey: ['admin-system-status'],
    queryFn: () => dashboardService.getSystemStatus(),
    staleTime: 10000, // Shorter stale time for system status
    enabled,
    retry: 1, // Don't retry too much if system is down
  });
};
