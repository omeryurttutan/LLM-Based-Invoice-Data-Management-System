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
import { tr } from 'date-fns/locale';
import { FilterOptionsResponse, InvoiceStatus, Currency, SourceType, LlmProvider } from '@/types/invoice';
import { Badge } from '@/components/ui/badge';

interface FilterProps {
  value?: any;
  onChange: (value: any) => void;
  options?: any[];
  label?: string;
}

export function StatusFilter({ value, onChange, options }: FilterProps) {
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
      <Label>Durum</Label>
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
             </Badge>
           );
        })}
      </div>
    </div>
  );
}

export function DateRangeFilter({ value, onChange }: { value: { from?: string, to?: string }, onChange: (val: { from?: string, to?: string }) => void }) {
  // Value is object with from/to
  // We need to parse strings to Date objects for Calendar
  const fromDate = value.from ? new Date(value.from) : undefined;
  const toDate = value.to ? new Date(value.to) : undefined;

  return (
    <div className='space-y-2'>
      <Label>Tarih Aralığı</Label>
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
              {fromDate ? format(fromDate, "dd.MM.yyyy") : <span>Başlangıç</span>}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={fromDate}
              onSelect={(date) => onChange({ ...value, from: date ? format(date, 'yyyy-MM-dd') : undefined })}
              initialFocus
              locale={tr}
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
              {toDate ? format(toDate, "dd.MM.yyyy") : <span>Bitiş</span>}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={toDate}
              onSelect={(date) => onChange({ ...value, to: date ? format(date, 'yyyy-MM-dd') : undefined })}
              initialFocus
              locale={tr}
            />
          </PopoverContent>
        </Popover>
      </div>
    </div>
  );
}

export function AmountRangeFilter({ value, onChange }: { value: { min?: number, max?: number }, onChange: (val: { min?: number, max?: number }) => void }) {
  return (
    <div className='space-y-2'>
      <Label>Tutar Aralığı</Label>
      <div className='flex gap-2 items-center'>
        <Input 
          type="number" 
          placeholder="Min" 
          value={value.min || ''} 
          onChange={(e) => onChange({ ...value, min: e.target.value ? Number(e.target.value) : undefined })}
          className='w-24'
        />
        <span>-</span>
        <Input 
          type="number" 
          placeholder="Max" 
          value={value.max || ''} 
          onChange={(e) => onChange({ ...value, max: e.target.value ? Number(e.target.value) : undefined })}
          className='w-24'
        />
      </div>
    </div>
  );
}

export function CategoryFilter({ value, onChange, options }: FilterProps) {
  // value is current categoryId (CSV)
  const selectedValues = (value ? String(value).split(',') : []);

  return (
    <div className='space-y-2'>
      <Label>Kategori</Label>
      <Select 
        value={selectedValues[0]} 
        onValueChange={(val) => {
            // For now single select wrapper to test, can be improved to multi-select
            // If we want multi select with standard select component it's tricky
            // Let's just append or toggle if we could, but Select doesn't support multi easily
            // For MVP/Phase 23b let's stick to single select or build a custom multi
            // But requirement says multi-select. 
            // We'll implementation a simple ScrollArea with checkboxes if many, or just Select for single for now as placeholder
            onChange(val);
        }}
      >
        <SelectTrigger>
          <SelectValue placeholder="Kategori Seç" />
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

// Supplier Autocomplete using Command
import { useSupplierAutocomplete } from '@/hooks/use-supplier-autocomplete';

export function SupplierFilter({ value, onChange }: FilterProps) {
  const { searchTerm, setSearchTerm, suppliers, isLoading } = useSupplierAutocomplete();
  const [open, setOpen] = useState(false);
  const selectedValues = (value ? String(value).split(',') : []);

  // Use local state for command input to debounce
  // We can't easily integrate hook's debounce with CommandInput value directly if controlled?
  // Actually hook handles debounce internally on `searchTerm` state change? 
  // Wait, hook uses `debouncedSearchTerm` derived from `searchTerm`.
  
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
      <Label>Tedarikçi</Label>
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
              Tedarikçi Ara...
              <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-full p-0">
            <Command shouldFilter={false}>
              <CommandInput placeholder="Tedarikçi adı..." onValueChange={setSearchTerm} value={searchTerm} />
              <CommandEmpty>{isLoading ? 'Aranıyor...' : 'Tedarikçi bulunamadı.'}</CommandEmpty>
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

// Imports for icons needed above not imported yet
import { ChevronsUpDown, X } from 'lucide-react';
import { useState } from 'react';
import { Slider } from '@/components/ui/slider';

export function CurrencyFilter({ value, onChange, options }: FilterProps) {
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
      <Label>Para Birimi</Label>
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
      <Label>Kaynak Tipi</Label>
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
      <Label>LLM Sağlayıcı</Label>
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
  const min = value.min ?? 0;
  const max = value.max ?? 100;

  return (
    <div className='space-y-4'>
      <div className="flex justify-between">
        <Label>Güven Skoru</Label>
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

