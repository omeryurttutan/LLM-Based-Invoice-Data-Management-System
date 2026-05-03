"use client";

import React from "react";
import { DateRange } from "react-day-picker";
import { CalendarIcon } from "lucide-react";
import { format, startOfMonth, endOfMonth, subDays, startOfYear } from "date-fns";
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

  const [startMonth, setStartMonth] = React.useState<Date>(dateRange?.from || new Date());
  const [endMonth, setEndMonth] = React.useState<Date>(
    dateRange?.to ? dateRange.to : dateRange?.from ? new Date(dateRange.from.getFullYear(), dateRange.from.getMonth() + 1) : new Date(new Date().getFullYear(), new Date().getMonth() + 1)
  );

  React.useEffect(() => {
    if (dateRange?.from) {
      setStartMonth(dateRange.from);
    }
    if (dateRange?.to) {
      setEndMonth(dateRange.to);
    } else if (dateRange?.from) {
      setEndMonth(new Date(dateRange.from.getFullYear(), dateRange.from.getMonth() + 1));
    }
  }, [dateRange]);


  const handleSelect = (range: DateRange | undefined) => {
    // End date validation: React Day Picker handled this by resetting if before, 
    // but we ensure it's clean here.
    if (range?.from && range?.to && range.to < range.from) {
      setDateRange({ from: range.to, to: undefined });
      return;
    }
    setDateRange(range);
  };

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
            <PopoverContent className="w-auto p-0 flex flex-col md:flex-row" align="end">
              <div className="flex flex-col gap-2 p-3 border-r pr-4">
                <Button variant="ghost" className="justify-start text-sm font-medium hover:bg-transparent cursor-default">
                  {t('presets.title')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const today = new Date();
                  setDateRange({ from: today, to: today });
                }}>
                  {t('presets.today')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const yesterday = subDays(new Date(), 1);
                  setDateRange({ from: yesterday, to: yesterday });
                }}>
                  {t('presets.yesterday')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const today = new Date();
                  setDateRange({ from: subDays(today, 6), to: today });
                }}>
                  {t('presets.last7days')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const today = new Date();
                  setDateRange({ from: subDays(today, 29), to: today });
                }}>
                  {t('presets.last30days')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const today = new Date();
                  setDateRange({ from: startOfMonth(today), to: endOfMonth(today) });
                }}>
                  {t('presets.thisMonth')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const lastMonth = subDays(startOfMonth(new Date()), 1);
                  setDateRange({ from: startOfMonth(lastMonth), to: endOfMonth(lastMonth) });
                }}>
                  {t('presets.lastMonth')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const today = new Date();
                  setDateRange({ from: startOfMonth(subDays(startOfMonth(today), 60)), to: endOfMonth(today) });
                }}>
                  {t('presets.last3Months')}
                </Button>
                <Button variant="ghost" className="justify-start text-sm" onClick={() => {
                  const today = new Date();
                  setDateRange({ from: startOfYear(today), to: endOfMonth(today) });
                }}>
                  {t('presets.thisYear')}
                </Button>
              </div>
              <div className="p-3">
                <div className="flex flex-col sm:flex-row gap-4">
                  <div className="flex flex-col gap-2">
                    <span className="text-sm font-medium pl-3">{t('startDate') || 'Başlangıç Tarihi'}</span>
                    <Calendar
                      initialFocus
                      mode="range"
                      defaultMonth={startMonth}
                      month={startMonth}
                      onMonthChange={setStartMonth}
                      selected={dateRange}
                      onSelect={(range, selectedDay) => {
                        // Left Calendar ONLY sets the Start Date ("from").
                        // If selectedDay is AFTER the current "to", reset "to".
                        if (dateRange?.to && selectedDay > dateRange.to) {
                          setDateRange({ from: selectedDay, to: undefined });
                        } else {
                          setDateRange({ from: selectedDay, to: dateRange?.to });
                        }
                      }}
                      numberOfMonths={1}
                      locale={dateLocale}
                      hideWeekdays
                      captionLayout="dropdown"
                      fromYear={2000}
                      toYear={2050}
                    />
                  </div>
                  <div className="flex flex-col gap-2">
                    <span className="text-sm font-medium pl-3">{t('endDate') || 'Bitiş Tarihi'}</span>
                    <Calendar
                      mode="range"
                      defaultMonth={endMonth}
                      month={endMonth}
                      onMonthChange={setEndMonth}
                      selected={dateRange}
                      onSelect={(range, selectedDay) => {
                        // Right Calendar ONLY sets the End Date ("to").
                        // If selectedDay is BEFORE the current "from", do nothing or reset "from".
                        if (dateRange?.from && selectedDay < dateRange.from) {
                           setDateRange({ from: selectedDay, to: undefined });
                        } else {
                           setDateRange({ from: dateRange?.from, to: selectedDay });
                        }
                      }}
                      numberOfMonths={1}
                      locale={dateLocale}
                      hideWeekdays
                      captionLayout="dropdown"
                      fromYear={2000}
                      toYear={2050}
                    />
                  </div>
                </div>
              </div>
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
