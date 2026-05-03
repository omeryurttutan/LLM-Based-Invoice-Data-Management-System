/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { ExtractionPerformance } from "@/services/dashboard.service";
import { Skeleton } from "@/components/ui/skeleton";
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from "recharts";
import { useFormatter, useTranslations } from "next-intl";

interface ExtractionPerformanceCardProps {
  data?: ExtractionPerformance;
  loading: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function ExtractionPerformanceCard({ data, loading }: ExtractionPerformanceCardProps) {
  const t = useTranslations('dashboard.performance');
  const format = useFormatter();

  if (loading) {
    return (
      <Card className="col-span-full">
        <CardHeader>
          <CardTitle>{t('title')}</CardTitle>
          <CardDescription>{t('description')}</CardDescription>
        </CardHeader>
        <CardContent className="h-[200px] flex items-center justify-center">
          <Skeleton className="h-[150px] w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!data || data.totalExtractions === 0) {
    return null;
  }

  // Define CustomTooltip inside to use closure for 't' and 'format'
  const CustomPerformanceTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const d = payload[0].payload;
      return (
        <div className="rounded-lg border bg-background p-2 shadow-sm">
          <div className="grid grid-cols-2 gap-2">
            <div className="flex flex-col">
              <span className="text-[0.70rem] uppercase text-muted-foreground">
                {t('model')}
              </span>
              <span className="font-bold text-muted-foreground">
                {d.provider}
              </span>
            </div>
            <div className="flex flex-col">
              <span className="text-[0.70rem] uppercase text-muted-foreground">
                {t('success')}
              </span>
              <span className="font-bold text-green-500">
                {d.successCount}
              </span>
            </div>
            <div className="flex flex-col">
              <span className="text-[0.70rem] uppercase text-muted-foreground">
                {t('error')}
              </span>
              <span className="font-bold text-red-500">
                {d.errorCount}
              </span>
            </div>
            <div className="flex flex-col">
              <span className="text-[0.70rem] uppercase text-muted-foreground">
                {t('cost')}
              </span>
              <span className="font-bold">
                {format.number(d.cost, { style: 'currency', currency: 'USD', minimumFractionDigits: 4 })}
              </span>
            </div>
          </div>
        </div>
      );
    }
    return null;
  };

  const successData = [
    { name: t('successful'), value: data.successRate, color: "#22c55e" },
    { name: t('failed'), value: 100 - data.successRate, color: "#ef4444" },
  ];

  const providerData = data.byProvider.map(p => ({
    ...p,
    successRate: p.attempts > 0 ? (p.successCount / p.attempts) * 100 : 0
  }));

  const getBarColor = (rate: number) => {
    if (rate >= 90) return "#22c55e"; // Green
    if (rate >= 70) return "#eab308"; // Yellow
    return "#ef4444"; // Red
  };

  return (
    <Card className="col-span-full">
      <CardHeader>
        <CardTitle>{t('title')}</CardTitle>
        <CardDescription>{t('description')}</CardDescription>
      </CardHeader>
      <CardContent className="grid grid-cols-1 md:grid-cols-3 gap-6">

        {/* Metric 1: Donut Chart */}
        <div className="flex flex-col items-center justify-center">
          <div className="h-[100px] w-[100px] relative">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={successData}
                  cx="50%"
                  cy="50%"
                  innerRadius={30}
                  outerRadius={45}
                  dataKey="value"
                  startAngle={90}
                  endAngle={-270}
                >
                  {successData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
              </PieChart>
            </ResponsiveContainer>
            <div className="absolute inset-0 flex items-center justify-center text-sm font-bold">
              {data.successRate}%
            </div>
          </div>
          <span className="text-sm font-medium mt-2">{t('successRate')}</span>
        </div>

        {/* Metric 2: Stats Grid */}
        <div className="grid grid-cols-1 gap-4 text-center">
          <div className="flex flex-col p-4 bg-muted rounded-lg">
            <span className="text-2xl font-bold">{data.totalExtractions}</span>
            <span className="text-xs text-muted-foreground w-full">{t('totalExtractions')}</span>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col p-4 bg-muted rounded-lg">
              <span className="text-lg font-bold">%{data.averageConfidence}</span>
              <span className="text-xs text-muted-foreground">{t('avgConfidence')}</span>
            </div>
            <div className="flex flex-col p-4 bg-muted rounded-lg">
              <span className="text-lg font-bold">{data.averageDuration}sn</span>
              <span className="text-xs text-muted-foreground">{t('avgDuration')}</span>
            </div>
          </div>
        </div>

        {/* Metric 3: Provider Performance */}
        <div className="h-[150px]">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={providerData} layout="vertical" margin={{ left: 40 }}>
              <XAxis type="number" hide />
              <YAxis dataKey="provider" type="category" width={60} tick={{ fontSize: 12 }} />
              <Tooltip
                content={<CustomPerformanceTooltip />}
                cursor={{ fill: 'transparent' }}
              />
              <Bar dataKey="attempts" radius={[0, 4, 4, 0]}>
                {providerData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={getBarColor(entry.successRate)} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          <p className="text-center text-xs text-muted-foreground mt-2">{t('providerAttempts')}</p>
        </div>
      </CardContent>
    </Card>
  );
}
