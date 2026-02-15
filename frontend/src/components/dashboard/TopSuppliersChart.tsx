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

  if (loading) {
    return (
      <Card className="col-span-1">
        <CardHeader>
          <CardTitle>En Çok Çalışılan Tedarikçiler</CardTitle>
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
          supplierName: "Diğer",
          totalAmount: data.othersAmount,
          invoiceCount: data.othersCount,
          percentage: 0, // calculate if needed or show different tooltip
        },
      ].filter((item) => item.totalAmount > 0)
    : [];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: currency,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const handleBarClick = (entry: any) => {
      if (entry && entry.supplierName && entry.supplierName !== "Diğer") {
          router.push(`/invoices?supplierName=${entry.supplierName}`);
      }
  };

  return (
    <Card className="col-span-1">
      <CardHeader>
        <CardTitle>En Çok Çalışılan Tedarikçiler</CardTitle>
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
            onClick={(data) => {
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
              formatter={(value: number, name: string, props: any) => [
                formatCurrency(value),
                "Toplam Tutar",
              ]}
              labelFormatter={(label) => label}
              contentStyle={{ borderRadius: "8px" }}
            />
            <Bar dataKey="totalAmount" radius={[0, 4, 4, 0]}>
                {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.supplierName === "Diğer" ? "#9ca3af" : "#f59e0b"} />
                ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
