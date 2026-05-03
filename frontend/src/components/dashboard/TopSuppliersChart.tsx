/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { TopSuppliers } from "@/services/dashboard.service";
import { Skeleton } from "@/components/ui/skeleton";
import { useRouter } from "next/navigation";
import { useFormatter, useTranslations } from "next-intl";

interface TopSuppliersChartProps {
  data?: TopSuppliers;
  loading: boolean;
  currency: string;
}

export function TopSuppliersChart({
  data,
  loading,
  currency,
}: TopSuppliersChartProps) {
  const router = useRouter();
  const t = useTranslations('dashboard.charts');
  const tCards = useTranslations('dashboard.cards');
  const format = useFormatter();

  if (loading) {
    return (
      <Card className="col-span-1">
        <CardHeader>
          <CardTitle>{t('topSuppliers')}</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px] flex items-center justify-center">
          <Skeleton className="h-[250px] w-full" />
        </CardContent>
      </Card>
    );
  }

  const chartData = data
    ? [
      ...data.suppliers,
      {
        supplierName: t('other'),
        totalAmount: data.othersAmount,
        invoiceCount: data.othersCount,
        percentage: 0, // calculate if needed or show different tooltip
      },
    ].filter((item) => item.totalAmount > 0)
    : [];

  const formatCurrency = (amount: number) => {
    return format.number(amount, {
      style: "currency",
      currency: currency,
      maximumFractionDigits: 0,
    });
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const tooltipFormatter = (value: any) => {
    return [formatCurrency(value), tCards('totalAmount')] as [string, string];
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleBarClick = (entry: any) => {
    if (entry && entry.supplierName && entry.supplierName !== "Diğer") {
      router.push(`/invoices?supplierName=${entry.supplierName}`);
    }
  };

  return (
    <Card className="col-span-1">
      <CardHeader>
        <CardTitle>{t('topSuppliers')}</CardTitle>
      </CardHeader>
      <CardContent className="h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            layout="vertical"
            data={chartData}
            margin={{
              top: 5,
              right: 30,
              left: 40,
              bottom: 5,
            }}
            onClick={(data: any) => {
              if (data && data.activePayload) {
                handleBarClick(data.activePayload[0].payload);
              }
            }}
            className="cursor-pointer"

          >
            <CartesianGrid strokeDasharray="3 3" horizontal={false} />
            <XAxis type="number" hide />
            <YAxis
              dataKey="supplierName"
              type="category"
              width={120}
              tick={{ fontSize: 12 }}
              tickFormatter={(value) =>
                value.length > 15 ? `${value.substring(0, 15)}...` : value
              }
            />
            <Tooltip
              formatter={tooltipFormatter}
              labelFormatter={(label) => label}
              contentStyle={{ borderRadius: "8px" }}
            />
            <Bar dataKey="totalAmount" radius={[0, 4, 4, 0]}>
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.supplierName === t('other') ? "#9ca3af" : "#f59e0b"} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
