/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { ExtractionPerformance } from "@/services/dashboard.service";
import { Skeleton } from "@/components/ui/skeleton";
import { PieChart, Pie, Cell, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip } from "recharts";

interface ExtractionPerformanceCardProps {
  data?: ExtractionPerformance;
  loading: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    const d = payload[0].payload;
    return (
      <div className="rounded-lg border bg-background p-2 shadow-sm">
        <div className="grid grid-cols-2 gap-2">
          <div className="flex flex-col">
            <span className="text-[0.70rem] uppercase text-muted-foreground">
              Model
            </span>
            <span className="font-bold text-muted-foreground">
              {d.provider}
            </span>
          </div>
          <div className="flex flex-col">
            <span className="text-[0.70rem] uppercase text-muted-foreground">
              Başarı
            </span>
            <span className="font-bold text-green-500">
              {d.successCount}
            </span>
          </div>
          <div className="flex flex-col">
            <span className="text-[0.70rem] uppercase text-muted-foreground">
              Hata
            </span>
            <span className="font-bold text-red-500">
              {d.errorCount}
            </span>
          </div>
          <div className="flex flex-col">
            <span className="text-[0.70rem] uppercase text-muted-foreground">
              Maliyet
            </span>
            <span className="font-bold">
              ${d.cost.toFixed(4)}
            </span>
          </div>
        </div>
      </div>
    );
  }
  return null;
};

export function ExtractionPerformanceCard({ data, loading }: ExtractionPerformanceCardProps) {
  if (loading) {
    return (
      <Card className="col-span-full">
        <CardHeader>
          <CardTitle>LLM Çıkarım Performansı</CardTitle>
          <CardDescription>Yapay zeka modellerinin başarı oranları</CardDescription>
        </CardHeader>
        <CardContent className="h-[200px] flex items-center justify-center">
          <Skeleton className="h-[150px] w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!data || data.totalExtractions === 0) {
    return null;
    // Or show empty state if desired, but requirements say "If no LLM extractions exist, show: 'Henüz LLM ile çıkarım yapılmadı.'"
    // However requirement 233 says "Conditional Rendering: Only render... if ADMIN or MANAGER". 
    // If data is empty for admin, we should probably show it.
  }

  const successData = [
    { name: "Başarılı", value: data.successRate, color: "#22c55e" },
    { name: "Başarısız", value: 100 - data.successRate, color: "#ef4444" },
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
        <CardTitle>LLM Çıkarım Performansı</CardTitle>
        <CardDescription>Yapay zeka modellerinin başarı oranları</CardDescription>
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
          <span className="text-sm font-medium mt-2">Başarı Oranı</span>
        </div>

        {/* Metric 2: Stats Grid */}
        <div className="grid grid-cols-1 gap-4 text-center">
          <div className="flex flex-col p-4 bg-muted rounded-lg">
            <span className="text-2xl font-bold">{data.totalExtractions}</span>
            <span className="text-xs text-muted-foreground w-full">Toplam Çıkarım</span>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col p-4 bg-muted rounded-lg">
              <span className="text-lg font-bold">%{data.averageConfidence}</span>
              <span className="text-xs text-muted-foreground">Ort. Güven</span>
            </div>
            <div className="flex flex-col p-4 bg-muted rounded-lg">
              <span className="text-lg font-bold">{data.averageDuration}sn</span>
              <span className="text-xs text-muted-foreground">Ort. Süre</span>
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
                content={<CustomTooltip />}
                cursor={{ fill: 'transparent' }}
              />
              <Bar dataKey="attempts" radius={[0, 4, 4, 0]}>
                {providerData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={getBarColor(entry.successRate)} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          <p className="text-center text-xs text-muted-foreground mt-2">Sağlayıcı Bazlı Denemeler</p>
        </div>
      </CardContent>
    </Card>
  );
}
