'use client';

import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { CalendarIcon, Check } from 'lucide-react';
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem } from '@/components/ui/command';
import { cn } from '@/lib/utils';
import { format } from 'date-fns';
import { tr, enUS } from 'date-fns/locale'; // Import EN locale too if needed, or dynamic import
import { FilterOptionsResponse, InvoiceStatus, Currency, SourceType, LlmProvider } from '@/types/invoice';
import { Badge } from '@/components/ui/badge';
import { useLocale, useTranslations } from 'next-intl';

// Imports for icons needed above not imported yet
import { ChevronsUpDown, X } from 'lucide-react';
import { useState, useEffect, useMemo } from 'react';
import { Slider } from '@/components/ui/slider';

// Supplier Autocomplete using Command
import { useSupplierAutocomplete } from '@/hooks/use-supplier-autocomplete';

interface FilterProps {
  value?: any;
  onChange: (value: any) => void;
  options?: any[];
  label?: string;
  isLoading?: boolean;
}

export function StatusFilter({ value, onChange, options }: FilterProps) {
  const t = useTranslations('invoices.filter');
  // Simple multi-select implementation using badges or checkboxes
  // For simplicity MVP we can use a multi-select dropdown or just a group of toggle buttons
  // Let's use a nice multi-select popover
  const selectedValues = (value ? String(value).split(',') : []) as InvoiceStatus[];

  const toggleValue = (val: InvoiceStatus) => {
    if (selectedValues.includes(val)) {
      const newValues = selectedValues.filter((v) => v !== val);
      onChange(newValues.length > 0 ? newValues.join(',') : undefined);
    } else {
      const newValues = [...selectedValues, val];
      onChange(newValues.join(','));
    }
  };

  return (
    <div className='space-y-1'>
      <Label>{t('status')}</Label>
      <div className='flex flex-wrap gap-2'>
        {options?.map((opt) => {
          const isSelected = selectedValues.includes(opt.value);
          return (
            <Badge
              key={opt.value}
              variant={isSelected ? 'default' : 'outline'}
              className='cursor-pointer'
              onClick={() => toggleValue(opt.value)}
            >
              {opt.label}
              {/* Label might come from options which might be translated already in parent or need translation here 
                   If options are passed from parent, parent should translate. 
                   If options are hardcoded enum values, we should translate them.
                   Assuming options passed here have 'label' which is already what we want to display.
                   If 'label' is raw enum, we might need t(`status.${opt.value}`)
               */}
            </Badge>
          );
        })}
      </div>
    </div>
  );
}

export function DateRangeFilter({ value, onChange }: { value: { from?: string, to?: string }, onChange: (val: { from?: string, to?: string }) => void }) {
  const t = useTranslations('invoices.filter');
  const locale = useLocale();
  const dateLocale = locale === 'tr' ? tr : enUS;

  const fromDate = useMemo(() => value.from ? new Date(value.from) : undefined, [value.from]);
  const toDate = useMemo(() => value.to ? new Date(value.to) : undefined, [value.to]);
  const dateRange = { from: fromDate, to: toDate };

  const [startMonth, setStartMonth] = useState<Date>(fromDate || new Date());
  const [endMonth, setEndMonth] = useState<Date>(
    toDate ? toDate : fromDate ? new Date(fromDate.getFullYear(), fromDate.getMonth() + 1) : new Date(new Date().getFullYear(), new Date().getMonth() + 1)
  );

  useEffect(() => {
    if (fromDate) setStartMonth(fromDate);
    if (toDate) setEndMonth(toDate);
    else if (fromDate) setEndMonth(new Date(fromDate.getFullYear(), fromDate.getMonth() + 1));
  }, [fromDate, toDate]);

  return (
    <div className='space-y-2'>
      <Label>{t('dateRange')}</Label>
      <div className='flex gap-2'>
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant={"outline"}
              className={cn(
                "w-[260px] justify-start text-left font-normal",
                !value.from && "text-muted-foreground"
              )}
            >
              <CalendarIcon className="mr-2 h-4 w-4" />
              {value.from ? (
                value.to ? (
                  <>
                    {format(fromDate!, "dd MMM yyyy", { locale: dateLocale })} -{" "}
                    {format(toDate!, "dd MMM yyyy", { locale: dateLocale })}
                  </>
                ) : (
                  format(fromDate!, "dd MMM yyyy", { locale: dateLocale })
                )
              ) : (
                <span>{t('dateRange') || 'Tarih Aralığı Seçin'}</span>
              )}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0 flex flex-col md:flex-row" align="start">
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
                      if (toDate && selectedDay > toDate) {
                        onChange({ from: format(selectedDay, 'yyyy-MM-dd'), to: undefined });
                      } else {
                        onChange({ from: format(selectedDay, 'yyyy-MM-dd'), to: value.to });
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
                      if (fromDate && selectedDay < fromDate) {
                        onChange({ from: format(selectedDay, 'yyyy-MM-dd'), to: undefined });
                      } else {
                        onChange({ from: value.from, to: format(selectedDay, 'yyyy-MM-dd') });
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
    </div>
  );
}

export function AmountRangeFilter({ value, onChange }: { value: { min?: number, max?: number }, onChange: (val: { min?: number, max?: number }) => void }) {
  const t = useTranslations('invoices.filter');

  const handleBlur = () => {
    if (value.min !== undefined && value.max !== undefined && value.min > value.max) {
      onChange({ min: value.max, max: value.min });
    }
  };

  return (
    <div className='space-y-2'>
      <Label>{t('amountRange')}</Label>
      <div className='flex gap-2 items-center'>
        <Input
          type="number"
          min={0}
          placeholder={t('min')}
          value={value.min ?? ''}
          onChange={(e) => {
             const val = e.target.value ? Math.max(0, Number(e.target.value)) : undefined;
             onChange({ ...value, min: val });
          }}
          onBlur={handleBlur}
          className='w-24'
        />
        <span>-</span>
        <Input
          type="number"
          min={0}
          placeholder={t('max')}
          value={value.max ?? ''}
          onChange={(e) => {
             const val = e.target.value ? Math.max(0, Number(e.target.value)) : undefined;
             onChange({ ...value, max: val });
          }}
          onBlur={handleBlur}
          className='w-24'
        />
      </div>
    </div>
  );
}

export function CategoryFilter({ value, onChange, options, isLoading }: FilterProps) {
  const t = useTranslations('invoices.filter');
  // value is current categoryId (CSV)
  const selectedValues = (value ? String(value).split(',') : []);

  return (
    <div className='space-y-2'>
      <Label>{t('category')}</Label>
      <Select
        value={selectedValues[0]}
        onValueChange={(val) => {
          // For now single select wrapper to test, can be improved to multi-select
          onChange(val);
        }}
      >
        <SelectTrigger>
          <SelectValue placeholder={t('selectCategory')} />
        </SelectTrigger>
        <SelectContent>
          {isLoading ? (
            <SelectItem disabled value="loading">{t('loading') || 'Yükleniyor...'}</SelectItem>
          ) : (!options || options.length === 0) ? (
            <SelectItem disabled value="empty">{t('noCategory') || 'Kategori Bulunmuyor'}</SelectItem>
          ) : (
            options.map((opt: any) => (
              <SelectItem key={opt.id} value={opt.id}>{opt.name}</SelectItem>
            ))
          )}
        </SelectContent>
      </Select>
    </div>
  )
}

export function SupplierFilter({ value, onChange }: FilterProps) {
  const t = useTranslations('invoices.filter');
  const { searchTerm, setSearchTerm, suppliers, isLoading } = useSupplierAutocomplete();
  const [open, setOpen] = useState(false);
  const selectedValues = (value ? String(value).split(',') : []);

  // Use local state for command input to debounce
  // We need to sync CommandInput with `setSearchTerm`.

  const toggleSupplier = (supplier: string) => {
    if (selectedValues.includes(supplier)) {
      const newValues = selectedValues.filter(v => v !== supplier);
      onChange(newValues.length > 0 ? newValues.join(',') : undefined);
    } else {
      const newValues = [...selectedValues, supplier];
      onChange(newValues.join(','));
    }
  };

  return (
    <div className='space-y-2'>
      <Label>{t('supplier')}</Label>
      <div>
        {selectedValues.length > 0 && (
          <div className='flex flex-wrap gap-1 mb-2'>
            {selectedValues.map(v => (
              <Badge key={v} variant="secondary" onClick={() => toggleSupplier(v)} className='cursor-pointer hover:bg-destructive hover:text-destructive-foreground'>
                {v} <X className='w-3 h-3 ml-1' />
              </Badge>
            ))}
          </div>
        )}
        <Popover open={open} onOpenChange={setOpen}>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              role="combobox"
              aria-expanded={open}
              className="w-full justify-between"
            >
              {t('searchSupplier')}...
              <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-full p-0">
            <Command shouldFilter={false}>
              <CommandInput placeholder={t('supplierName') + "..."} onValueChange={setSearchTerm} value={searchTerm} />
              <CommandEmpty>{isLoading ? t('searching') : t('noSupplierFound')}</CommandEmpty>
              <CommandGroup>
                {suppliers.map((supplier) => (
                  <CommandItem
                    key={supplier}
                    value={supplier}
                    onSelect={(currentValue) => {
                      toggleSupplier(supplier);
                      // setOpen(false); // Keep open for multi select
                    }}
                  >
                    <Check
                      className={cn(
                        "mr-2 h-4 w-4",
                        selectedValues.includes(supplier) ? "opacity-100" : "opacity-0"
                      )}
                    />
                    {supplier}
                  </CommandItem>
                ))}
              </CommandGroup>
            </Command>
          </PopoverContent>
        </Popover>
      </div>
    </div>
  );
}

export function CurrencyFilter({ value, onChange, options }: FilterProps) {
  const t = useTranslations('invoices.filter');
  // options is array of currencies
  const selectedValues = (value ? String(value).split(',') : []);

  const toggleValue = (val: string) => {
    if (selectedValues.includes(val)) {
      const newValues = selectedValues.filter(v => v !== val);
      onChange(newValues.length > 0 ? newValues.join(',') : undefined);
    } else {
      const newValues = [...selectedValues, val];
      onChange(newValues.join(','));
    }
  };

  return (
    <div className='space-y-1'>
      <Label>{t('currency')}</Label>
      <div className='flex flex-wrap gap-2'>
        {options?.map((opt: any) => {
          const isSelected = selectedValues.includes(opt);
          return (
            <Badge
              key={opt}
              variant={isSelected ? 'default' : 'outline'}
              className='cursor-pointer'
              onClick={() => toggleValue(opt)}
            >
              {opt}
            </Badge>
          );
        })}
      </div>
    </div>
  );
}

export function SourceTypeFilter({ value, onChange, options }: FilterProps) {
  const t = useTranslations('invoices.filter');
  const selectedValues = (value ? String(value).split(',') : []);

  const toggleValue = (val: string) => {
    if (selectedValues.includes(val)) {
      const newValues = selectedValues.filter(v => v !== val);
      onChange(newValues.length > 0 ? newValues.join(',') : undefined);
    } else {
      const newValues = [...selectedValues, val];
      onChange(newValues.join(','));
    }
  };

  return (
    <div className='space-y-1'>
      <Label>{t('sourceType')}</Label>
      <div className='flex flex-wrap gap-2'>
        {options?.map((opt: any) => {
          const isSelected = selectedValues.includes(opt);
          return (
            <Badge
              key={opt}
              variant={isSelected ? 'default' : 'outline'}
              className='cursor-pointer'
              onClick={() => toggleValue(opt)}
            >
              {opt}
            </Badge>
          );
        })}
      </div>
    </div>
  );
}

