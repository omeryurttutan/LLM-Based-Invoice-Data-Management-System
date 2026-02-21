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
import dynamic from 'next/dynamic';
import { DashboardHeader } from "@/components/dashboard/DashboardHeader";
import { SummaryCards } from "@/components/dashboard/SummaryCards";
import { PendingActionsList } from "@/components/dashboard/PendingActionsList";

const CategoryDistributionChart = dynamic(
  () => import("@/components/dashboard/CategoryDistributionChart").then((mod) => mod.CategoryDistributionChart),
  { ssr: false }
);
const MonthlyTrendChart = dynamic(
  () => import("@/components/dashboard/MonthlyTrendChart").then((mod) => mod.MonthlyTrendChart),
  { ssr: false }
);
const TopSuppliersChart = dynamic(
  () => import("@/components/dashboard/TopSuppliersChart").then((mod) => mod.TopSuppliersChart),
  { ssr: false }
);
const StatusTimelineChart = dynamic(
  () => import("@/components/dashboard/StatusTimelineChart").then((mod) => mod.StatusTimelineChart),
  { ssr: false }
);
const ExtractionPerformanceCard = dynamic(
  () => import("@/components/dashboard/ExtractionPerformanceCard").then((mod) => mod.ExtractionPerformanceCard),
  { ssr: false }
);
const SystemHealthPanel = dynamic(
  () => import("@/components/dashboard/SystemHealthPanel").then((mod) => mod.SystemHealthPanel),
  { ssr: false }
);

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
        loading={statsQuery.isLoading && !statsQuery.isError}
        currency={currency}
      />

      {/* Main Charts Row 1 */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-7">
        <div className="col-span-3">
          <CategoryDistributionChart
            data={categoriesQuery.data}
            loading={categoriesQuery.isLoading && !categoriesQuery.isError}
            currency={currency}
          />
        </div>
        <div className="col-span-4">
          <MonthlyTrendChart
            data={trendQuery.data}
            loading={trendQuery.isLoading && !trendQuery.isError}
            currency={currency}
          />
        </div>
      </div>

      {/* Main Charts Row 2 */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-7">
        <div className="col-span-3">
          <TopSuppliersChart
            data={suppliersQuery.data}
            loading={suppliersQuery.isLoading && !suppliersQuery.isError}
            currency={currency}
          />
        </div>
        <div className="col-span-4">
          <PendingActionsList
            data={pendingQuery.data}
            loading={pendingQuery.isLoading && !pendingQuery.isError}
          />
        </div>
      </div>

      {/* Status Timeline */}
      <StatusTimelineChart
        data={timelineQuery.data}
        loading={timelineQuery.isLoading && !timelineQuery.isError}
      />

      {/* Admin/Manager Sections */}
      {isAdminOrManager && (
        <ExtractionPerformanceCard
          data={extractionQuery.data}
          loading={extractionQuery.isLoading && !extractionQuery.isError}
        />
      )}

      {/* Admin Only Section */}
      {isAdmin && <SystemHealthPanel />}
    </div>
  );
}
