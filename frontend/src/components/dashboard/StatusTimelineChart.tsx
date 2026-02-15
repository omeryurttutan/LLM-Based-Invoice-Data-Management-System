/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatusTimeline } from "@/services/dashboard.service";
import { Skeleton } from "@/components/ui/skeleton";
import { format, parseISO } from "date-fns";
import { tr } from "date-fns/locale";
import { useRouter } from "next/navigation";

interface StatusTimelineChartProps {
  data?: StatusTimeline[];
  loading: boolean;
}

export function StatusTimelineChart({ data, loading }: StatusTimelineChartProps) {
  const router = useRouter();

  if (loading) {
    return (
      <Card className="col-span-full">
        <CardHeader>
          <CardTitle>Durum Zaman Çizelgesi</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px] flex items-center justify-center">
          <Skeleton className="h-[250px] w-full" />
        </CardContent>
      </Card>
    );
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleAreaClick = (data: any) => {
    if (data && data.activePayload) {
      const dateStr = data.activePayload[0].payload.date;
      router.push(`/invoices?dateFrom=${dateStr}&dateTo=${dateStr}`);
    }
  };

  return (
    <Card className="col-span-full">
      <CardHeader>
        <CardTitle>Durum Zaman Çizelgesi (Son 30 Gün)</CardTitle>
      </CardHeader>
      <CardContent className="h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart
            data={data}
            margin={{
              top: 10,
              right: 30,
              left: 0,
              bottom: 0,
            }}
            onClick={handleAreaClick}
            className="cursor-pointer"
          >
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis
              dataKey="date"
              tickFormatter={(value) => format(parseISO(value), 'dd MMM', { locale: tr })}
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis fontSize={12} tickLine={false} axisLine={false} />
            <Tooltip
              labelFormatter={(value) => format(parseISO(value), 'dd MMMM yyyy', { locale: tr })}
              contentStyle={{ borderRadius: "8px" }}
            />
            <Legend />
            <Area
              type="monotone"
              dataKey="created"
              name="Oluşturulan"
              stackId="1"
              stroke="#3b82f6"
              fill="#3b82f6"
              fillOpacity={0.6}
            />
            <Area
              type="monotone"
              dataKey="verified"
              name="Onaylanan"
              stackId="1"
              stroke="#22c55e"
              fill="#22c55e"
              fillOpacity={0.6}
            />
            <Area
              type="monotone"
              dataKey="rejected"
              name="Reddedilen"
              stackId="1"
              stroke="#ef4444"
              fill="#ef4444"
              fillOpacity={0.6}
            />
          </AreaChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
