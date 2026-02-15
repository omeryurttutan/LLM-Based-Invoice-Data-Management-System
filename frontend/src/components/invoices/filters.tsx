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
import { useState } from 'react';
import { Slider } from '@/components/ui/slider';

// Supplier Autocomplete using Command
import { useSupplierAutocomplete } from '@/hooks/use-supplier-autocomplete';

interface FilterProps {
  value?: any;
  onChange: (value: any) => void;
  options?: any[];
  label?: string;
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

  // Value is object with from/to
  // We need to parse strings to Date objects for Calendar
  const fromDate = value.from ? new Date(value.from) : undefined;
  const toDate = value.to ? new Date(value.to) : undefined;

  return (
    <div className='space-y-2'>
      <Label>{t('dateRange')}</Label>
      <div className='flex gap-2'>
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant={"outline"}
              className={cn(
                "w-[140px] justify-start text-left font-normal",
                !fromDate && "text-muted-foreground"
              )}
            >
              <CalendarIcon className="mr-2 h-4 w-4" />
              {fromDate ? format(fromDate, "dd.MM.yyyy") : <span>{t('startDate')}</span>}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={fromDate}
              onSelect={(date) => onChange({ ...value, from: date ? format(date, 'yyyy-MM-dd') : undefined })}
              initialFocus
              locale={dateLocale}
            />
          </PopoverContent>
        </Popover>

        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant={"outline"}
              className={cn(
                "w-[140px] justify-start text-left font-normal",
                !toDate && "text-muted-foreground"
              )}
            >
              <CalendarIcon className="mr-2 h-4 w-4" />
              {toDate ? format(toDate, "dd.MM.yyyy") : <span>{t('endDate')}</span>}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={toDate}
              onSelect={(date) => onChange({ ...value, to: date ? format(date, 'yyyy-MM-dd') : undefined })}
              initialFocus
              locale={dateLocale}
            />
          </PopoverContent>
        </Popover>
      </div>
    </div>
  );
}

export function AmountRangeFilter({ value, onChange }: { value: { min?: number, max?: number }, onChange: (val: { min?: number, max?: number }) => void }) {
  const t = useTranslations('invoices.filter');
  return (
    <div className='space-y-2'>
      <Label>{t('amountRange')}</Label>
      <div className='flex gap-2 items-center'>
        <Input
          type="number"
          placeholder={t('min')}
          value={value.min || ''}
          onChange={(e) => onChange({ ...value, min: e.target.value ? Number(e.target.value) : undefined })}
          className='w-24'
        />
        <span>-</span>
        <Input
          type="number"
          placeholder={t('max')}
          value={value.max || ''}
          onChange={(e) => onChange({ ...value, max: e.target.value ? Number(e.target.value) : undefined })}
          className='w-24'
        />
      </div>
    </div>
  );
}

export function CategoryFilter({ value, onChange, options }: FilterProps) {
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
          {options?.map((opt: any) => (
            <SelectItem key={opt.id} value={opt.id}>{opt.name}</SelectItem>
          ))}
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

export function LlmProviderFilter({ value, onChange, options, sourceType }: FilterProps & { sourceType?: string }) {
  const t = useTranslations('invoices.filter');
  // Only show if SourceType includes 'LLM'
  const isLlmSelected = sourceType?.includes('LLM');

  if (!isLlmSelected) return null;

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
      <Label>{t('llmProvider')}</Label>
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

export function ConfidenceFilter({ value, onChange }: { value: { min?: number, max?: number }, onChange: (val: { min?: number, max?: number }) => void }) {
  const t = useTranslations('invoices.filter');
  const min = value.min ?? 0;
  const max = value.max ?? 100;

  return (
    <div className='space-y-4'>
      <div className="flex justify-between">
        <Label>{t('confidenceScore')}</Label>
        <span className="text-xs text-muted-foreground">{min} - {max}</span>
      </div>
      <Slider
        value={[min, max]}
        min={0}
        max={100}
        step={1}
        minStepsBetweenThumbs={1}
        onValueChange={(vals) => onChange({ min: vals[0], max: vals[1] })}
        className="w-full"
      />
    </div>
  );
}

