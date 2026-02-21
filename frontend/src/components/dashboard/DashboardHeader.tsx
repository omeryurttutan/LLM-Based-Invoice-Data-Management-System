"use client";

import React from "react";
import { DateRange } from "react-day-picker";
import { CalendarIcon } from "lucide-react";
import { format } from "date-fns";
import { tr, enUS } from "date-fns/locale";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { useLocale, useTranslations } from "next-intl";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface DashboardHeaderProps {
  dateRange: DateRange | undefined;
  setDateRange: (range: DateRange | undefined) => void;
  currency: string;
  setCurrency: (currency: string) => void;
}

export function DashboardHeader({
  dateRange,
  setDateRange,
  currency,
  setCurrency,
}: DashboardHeaderProps) {
  const t = useTranslations('dashboard');
  const locale = useLocale();
  const dateLocale = locale === 'tr' ? tr : enUS;

  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <h1 className="text-3xl font-bold tracking-tight">{t('title') || 'Dashboard'}</h1>
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="grid gap-2">
          <Popover>
            <PopoverTrigger asChild>
              <Button
                id="date"
                variant={"outline"}
                className={cn(
                  "w-[260px] justify-start text-left font-normal",
                  !dateRange && "text-muted-foreground"
                )}
              >
                <CalendarIcon className="mr-2 h-4 w-4" />
                {dateRange?.from ? (
                  dateRange.to ? (
                    <>
                      {format(dateRange.from, "dd MMM yyyy", { locale: dateLocale })} -{" "}
                      {format(dateRange.to, "dd MMM yyyy", { locale: dateLocale })}
                    </>
                  ) : (
                    format(dateRange.from, "dd MMM yyyy", { locale: dateLocale })
                  )
                ) : (
                  <span>{t('dateSelect') || 'Tarih Aralığı Seçin'}</span>
                )}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0" align="end">
              <Calendar
                initialFocus
                mode="range"
                defaultMonth={dateRange?.from}
                selected={dateRange}
                onSelect={setDateRange}
                numberOfMonths={2}
                locale={dateLocale}
              />
            </PopoverContent>
          </Popover>
        </div>
        <Select value={currency} onValueChange={setCurrency}>
          <SelectTrigger className="w-[120px]">
            <SelectValue placeholder={t('currencyLabel') || "Para Birimi"} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="TRY">TRY (₺)</SelectItem>
            <SelectItem value="USD">USD ($)</SelectItem>
            <SelectItem value="EUR">EUR (€)</SelectItem>
            <SelectItem value="GBP">GBP (£)</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
