"use client";

import React, { useState, useMemo, useEffect } from "react";
import { DateRange } from "react-day-picker";
import { startOfMonth, endOfMonth, format } from "date-fns";
import { useAuthStore } from "@/stores/auth-store";
import {
  useDashboardStats,
  useCategoryDistribution,
  useMonthlyTrend,
  useTopSuppliers,
  usePendingActions,
  useStatusTimeline,
  useExtractionPerformance,
} from "@/hooks/use-dashboard";
import { DashboardHeader } from "@/components/dashboard/DashboardHeader";
import { SummaryCards } from "@/components/dashboard/SummaryCards";
import { CategoryDistributionChart } from "@/components/dashboard/CategoryDistributionChart";
import { MonthlyTrendChart } from "@/components/dashboard/MonthlyTrendChart";
import { TopSuppliersChart } from "@/components/dashboard/TopSuppliersChart";
import { PendingActionsList } from "@/components/dashboard/PendingActionsList";
import { StatusTimelineChart } from "@/components/dashboard/StatusTimelineChart";
import { ExtractionPerformanceCard } from "@/components/dashboard/ExtractionPerformanceCard";
import { SystemHealthPanel } from "@/components/dashboard/SystemHealthPanel";

export default function DashboardPage() {
  // State
  const [dateRange, setDateRange] = useState<DateRange | undefined>({
    from: startOfMonth(new Date()),
    to: endOfMonth(new Date()),
  });
  const [currency, setCurrency] = useState("TRY");
  
  // Auth
  const { user } = useAuthStore();
  const isAdminOrManager = user?.role === "ADMIN" || user?.role === "MANAGER";
  const isAdmin = user?.role === "ADMIN";

  // Derived params
  const dateParams = useMemo(() => {
    return {
      dateFrom: dateRange?.from ? format(dateRange.from, "yyyy-MM-dd") : undefined,
      dateTo: dateRange?.to ? format(dateRange.to, "yyyy-MM-dd") : undefined,
      currency,
    };
  }, [dateRange, currency]);

  // Data Fetching
  const statsQuery = useDashboardStats(dateParams);
  const categoriesQuery = useCategoryDistribution(dateParams);
  // Trend usually shows last 12 months regardless of filter, but we can pass currency
  const trendQuery = useMonthlyTrend({ currency }); 
  const suppliersQuery = useTopSuppliers({ ...dateParams, limit: 10 });
  const pendingQuery = usePendingActions({ limit: 10 });
  const timelineQuery = useStatusTimeline({ days: 30 }); // Default 30 days
  
  // Conditional fetching for admin/manager
  const extractionQuery = useExtractionPerformance(
    isAdminOrManager ? { ...dateParams } : undefined
  );

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      {/* Header */}
      <DashboardHeader
        dateRange={dateRange}
        setDateRange={setDateRange}
        currency={currency}
        setCurrency={setCurrency}
      />

      {/* KPI Cards */}
      <SummaryCards
        stats={statsQuery.data}
        loading={statsQuery.isLoading}
        currency={currency}
      />

      {/* Main Charts Row 1 */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-7">
        <div className="col-span-3">
          <CategoryDistributionChart
            data={categoriesQuery.data}
            loading={categoriesQuery.isLoading}
            currency={currency}
          />
        </div>
        <div className="col-span-4">
          <MonthlyTrendChart
            data={trendQuery.data}
            loading={trendQuery.isLoading}
            currency={currency}
          />
        </div>
      </div>

      {/* Main Charts Row 2 */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-7">
        <div className="col-span-3">
          <TopSuppliersChart
            data={suppliersQuery.data}
            loading={suppliersQuery.isLoading}
            currency={currency}
          />
        </div>
        <div className="col-span-4">
          <PendingActionsList
            data={pendingQuery.data}
            loading={pendingQuery.isLoading}
          />
        </div>
      </div>

      {/* Status Timeline */}
      <StatusTimelineChart
        data={timelineQuery.data}
        loading={timelineQuery.isLoading}
      />

      {/* Admin/Manager Sections */}
      {isAdminOrManager && (
        <ExtractionPerformanceCard
          data={extractionQuery.data}
          loading={extractionQuery.isLoading}
        />
      )}

      {/* Admin Only Section */}
      {isAdmin && <SystemHealthPanel />}
    </div>
  );
}
