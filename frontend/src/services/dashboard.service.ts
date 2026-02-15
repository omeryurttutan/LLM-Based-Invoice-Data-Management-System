import { api } from './api';

export interface DashboardStats {
  period: {
    dateFrom: string | null;
    dateTo: string | null;
    currency: string;
  };
  summary: {
    totalInvoices: number;
    totalAmount: number;
    averageAmount: number;
    pendingCount: number;
    pendingAmount: number;
    verifiedCount: number;
    verifiedAmount: number;
    rejectedCount: number;
    processingCount: number;
  };
  sourceBreakdown: Record<string, { count: number; percentage: number }>;
  confidenceStats: {
    averageScore: number;
    highConfidence: number;
    mediumConfidence: number;
    lowConfidence: number;
  };
}

export interface CategoryDistribution {
  categoryId: string | null;
  categoryName: string;
  categoryColor: string;
  invoiceCount: number;
  totalAmount: number;
  percentage: number;
}

export interface MonthlyTrend {
  month: string;
  label: string;
  invoiceCount: number;
  totalAmount: number;
  verifiedAmount: number;
  averageAmount: number;
}

export interface SupplierStats {
  supplierName: string;
  supplierTaxNumber: string;
  invoiceCount: number;
  totalAmount: number;
  percentage: number;
}

export interface TopSuppliers {
  suppliers: SupplierStats[];
  othersCount: number;
  othersAmount: number;
}

export interface PendingInvoice {
  id: string;
  invoiceNumber: string;
  supplierName: string;
  totalAmount: number;
  currency: string;
  sourceType: string;
  confidenceScore: number | null;
  createdAt: string;
  daysPending: number;
}

export interface PendingActions {
  totalPending: number;
  invoices: PendingInvoice[];
}

export interface StatusTimeline {
  date: string;
  created: number;
  verified: number;
  rejected: number;
}

export interface ExtractionPerformance {
  totalExtractions: number;
  successRate: number;
  averageConfidence: number;
  averageDuration: number;
  byProvider: {
    provider: string;
    attempts: number;
    successCount: number;
    failureCount: number;
    averageConfidence: number;
    fallbackCount: number;
  }[];
  failureReasons: { reason: string; count: number }[];
}

export const getDashboardStats = async (params?: { dateFrom?: string; dateTo?: string; currency?: string }) => {
  const { data } = await api.get<DashboardStats>('/dashboard/stats', { params });
  return data;
};

export const getCategoryDistribution = async (params?: { dateFrom?: string; dateTo?: string; currency?: string }) => {
  const { data } = await api.get<CategoryDistribution[]>('/dashboard/categories', { params });
  return data;
};

export const getMonthlyTrend = async (params?: { months?: number; currency?: string }) => {
  const { data } = await api.get<MonthlyTrend[]>('/dashboard/monthly-trend', { params });
  return data;
};

export const getTopSuppliers = async (params?: { dateFrom?: string; dateTo?: string; currency?: string; limit?: number }) => {
  const { data } = await api.get<TopSuppliers>('/dashboard/top-suppliers', { params });
  return data;
};

export const getPendingActions = async (params?: { limit?: number }) => {
  const { data } = await api.get<PendingActions>('/dashboard/pending-actions', { params });
  return data;
};

export const getStatusTimeline = async (params?: { days?: number }) => {
  const { data } = await api.get<StatusTimeline[]>('/dashboard/status-timeline', { params });
  return data;
};

export const getExtractionPerformance = async (params?: { dateFrom?: string; dateTo?: string }) => {
  const { data } = await api.get<ExtractionPerformance>('/dashboard/extraction-performance', { params });
  return data;
};

export interface SystemStatus {
  services: {
    name: string;
    status: "UP" | "DOWN" | "UNKNOWN";
    message?: string;
  }[];
  resources: {
    jvmHeapUsage: number;
    jvmHeapMax: number;
    dbActiveConnections: number;
    dbMaxConnections: number;
    diskUsage: number;
    diskTotal: number;
  };
  llmCost: {
    currentMonthCost: number;
    monthlyLimit: number;
    dailyCost: number;
    dailyLimit: number;
    byProvider: { provider: string; cost: number }[];
  };
  alerts: {
    severity: "CRITICAL" | "HIGH" | "WARN" | "INFO";
    message: string;
    timestamp: string;
  }[];
}

export const getSystemStatus = async () => {
  const { data } = await api.get<SystemStatus>('/admin/system/status');
  return data;
};
