/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MonthlyTrend } from "@/services/dashboard.service";
import { Skeleton } from "@/components/ui/skeleton";
import { useRouter } from "next/navigation";
import { format, parseISO, endOfMonth } from "date-fns";
import { useFormatter, useTranslations } from "next-intl";

interface MonthlyTrendChartProps {
  data?: MonthlyTrend[];
  loading: boolean;
  currency: string;
}

export function MonthlyTrendChart({
  data,
  loading,
  currency,
}: MonthlyTrendChartProps) {
  const router = useRouter();

  const t = useTranslations('dashboard.charts');
  const tCards = useTranslations('dashboard.cards');
  const tStatus = useTranslations('common.status');
  const formatNumber = useFormatter();

  if (loading) {
    return (
      <Card className="col-span-1">
        <CardHeader>
          <CardTitle>{t('monthlyTrend')}</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px] flex items-center justify-center">
          <Skeleton className="h-[250px] w-full" />
        </CardContent>
      </Card>
    );
  }

  const formatCurrency = (amount: number) => {
    return formatNumber.number(amount, {
      style: "currency",
      currency: currency,
      maximumFractionDigits: 0,
    });
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const tooltipFormatter = (value: any) => formatCurrency(value);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleDotClick = (data: any) => {
    if (data && data.activePayload && data.activePayload.length > 0) {
      const item = data.activePayload[0].payload as MonthlyTrend;
      const date = parseISO(`${item.month}-01`);
      const from = format(date, 'yyyy-MM-dd');
      const to = format(endOfMonth(date), 'yyyy-MM-dd');
      router.push(`/invoices?dateFrom=${from}&dateTo=${to}`);
    }
  };

  return (
    <Card className="col-span-1">
      <CardHeader>
        <CardTitle>{t('monthlyTrend')}</CardTitle>
      </CardHeader>
      <CardContent className="h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart
            data={data}
            margin={{
              top: 5,
              right: 30,
              left: 20,
              bottom: 5,
            }}
            onClick={handleDotClick}
            className="cursor-pointer"
          >
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="label" fontSize={12} tickLine={false} axisLine={false} />
            <YAxis
              tickFormatter={(value) =>
                formatNumber.number(value, {
                  notation: "compact",
                  compactDisplay: "short",
                })
              }
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <Tooltip
              formatter={tooltipFormatter}
              contentStyle={{ borderRadius: "8px" }}
            />
            <Legend />
            <Line
              type="monotone"
              dataKey="totalAmount"
              name={tCards('totalAmount')}
              stroke="#2563eb"
              activeDot={{ r: 8 }}
              strokeWidth={2}
            />
            <Line
              type="monotone"
              dataKey="verifiedAmount"
              name={tStatus('verified')}
              stroke="#16a34a"
              strokeDasharray="5 5"
              strokeWidth={2}
            />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
