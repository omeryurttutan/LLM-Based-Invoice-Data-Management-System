/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  Legend,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CategoryDistribution } from "@/services/dashboard.service";
import { Skeleton } from "@/components/ui/skeleton";
import { useRouter } from "next/navigation";

interface CategoryDistributionChartProps {
  data?: CategoryDistribution[];
  loading: boolean;
  currency: string;
}

export function CategoryDistributionChart({
  data,
  loading,
  currency,
}: CategoryDistributionChartProps) {
  const router = useRouter();

  if (loading) {
    return (
      <Card className="col-span-1">
        <CardHeader>
          <CardTitle>Kategori Dağılımı</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px] flex items-center justify-center">
          <Skeleton className="h-[250px] w-[250px] rounded-full" />
        </CardContent>
      </Card>
    );
  }

  if (!data || data.length === 0) {
    return (
      <Card className="col-span-1">
        <CardHeader>
          <CardTitle>Kategori Dağılımı</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px] flex items-center justify-center text-muted-foreground">
          Henüz kategorize edilmiş fatura bulunmuyor
        </CardContent>
      </Card>
    );
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: currency,
    }).format(amount);
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const tooltipFormatter = (value: any, name: any, props: any) => {
    return [
      formatCurrency(value),
      `${name} (${props?.payload?.percentage ?? 0}%)`,
    ] as [string, string];
  };

  const handlePieClick = (entry: CategoryDistribution) => {
    if (entry && entry.categoryId) {
      router.push(`/invoices?categoryId=${entry.categoryId}`);
    } else {
      // Handle uncategorized or "other" if needed, usually just list all
      router.push(`/invoices`);
    }
  };

  return (
    <Card className="col-span-1">
      <CardHeader>
        <CardTitle>Kategori Dağılımı</CardTitle>
      </CardHeader>
      <CardContent className="h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={80}
              paddingAngle={2}
              dataKey="totalAmount"
              nameKey="categoryName"
              onClick={handlePieClick}
              cursor="pointer"
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.categoryColor} />
              ))}
            </Pie>
            <Tooltip
              formatter={tooltipFormatter}
            />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
