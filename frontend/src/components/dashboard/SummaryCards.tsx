"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DashboardStats } from "@/services/dashboard.service";
import { useFormatter, useTranslations } from "next-intl";
import { FileText, TrendingUp, Clock, CheckCircle, AlertTriangle } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import Link from "next/link";
import { cn } from "@/lib/utils";

interface SummaryCardsProps {
  stats?: DashboardStats;
  loading: boolean;
  currency: string;
}

export function SummaryCards({ stats, loading, currency }: SummaryCardsProps) {
  const t = useTranslations('dashboard');
  const tStatus = useTranslations('common.status');
  const format = useFormatter();

  const formatCurrency = (amount: number) => {
    return format.number(amount, {
      style: "currency",
      currency: currency,
    });
  };

  if (loading || !stats) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <Card key={i}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                <Skeleton className="h-4 w-[100px]" />
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Skeleton className="h-8 w-[60px] mb-2" />
              <Skeleton className="h-4 w-[140px]" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  const { summary, sourceBreakdown } = stats;

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <Link href="/invoices">
        <Card className="hover:shadow-md transition-shadow cursor-pointer">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{t('cards.totalInvoices')}</CardTitle>
            <FileText className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary.totalInvoices}</div>
            <p className="text-xs text-muted-foreground">
              LLM: {sourceBreakdown.LLM?.percentage ?? 0}% | e-Fatura:{" "}
              {sourceBreakdown.E_INVOICE?.percentage ?? 0}%
            </p>
          </CardContent>
        </Card>
      </Link>

      <Link href="/invoices">
        <Card className="hover:shadow-md transition-shadow cursor-pointer">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{t('cards.totalAmount')}</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatCurrency(summary.totalAmount)}
            </div>
            <p className="text-xs text-muted-foreground">
              {t('cards.average')}: {formatCurrency(summary.averageAmount)}
            </p>
          </CardContent>
        </Card>
      </Link>

      <Link href="/invoices?status=PENDING">
        <Card className="hover:shadow-md transition-shadow cursor-pointer border-l-4 border-l-yellow-500">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{tStatus('pending')}</CardTitle>
            <Clock className="h-4 w-4 text-yellow-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary.pendingCount}</div>
            <p className="text-xs text-muted-foreground">
              {formatCurrency(summary.pendingAmount)}
            </p>
          </CardContent>
        </Card>
      </Link>

      <Link href="/invoices?status=VERIFIED">
        <Card className="hover:shadow-md transition-shadow cursor-pointer border-l-4 border-l-green-500">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{tStatus('verified')}</CardTitle>
            <CheckCircle className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{summary.verifiedCount}</div>
            <p className="text-xs text-muted-foreground">
              {formatCurrency(summary.verifiedAmount)}
            </p>
          </CardContent>
        </Card>
      </Link>
    </div>
  );
}
