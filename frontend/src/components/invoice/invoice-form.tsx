'use client';

import { useState, useEffect } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { CalendarIcon, Trash2, Plus } from 'lucide-react';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';

import { cn, formatCurrency } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Calendar } from '@/components/ui/calendar';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';

import { CreateInvoiceRequest, UnitType, Currency } from '@/types/invoice';
import { Category } from '@/types/category';

const invoiceSchema = z.object({
  invoiceNumber: z.string().min(1, 'Fatura numarası zorunludur'),
  invoiceDate: z.date({ required_error: 'Fatura tarihi zorunludur' }),
  dueDate: z.date().optional(),
  supplierName: z.string().min(1, 'Tedarikçi adı zorunludur'),
  supplierTaxNumber: z.string().regex(/^[0-9]{10,11}$/, 'Vergi numarası 10 veya 11 haneli olmalıdır').optional().or(z.literal('')),
  supplierTaxOffice: z.string().optional(),
  supplierAddress: z.string().optional(),
  supplierPhone: z.string().optional(),
  supplierEmail: z.string().email('Geçerli bir e-posta adresi giriniz').optional().or(z.literal('')),
  currency: z.enum(['TRY', 'USD', 'EUR', 'GBP'] as const),
  exchangeRate: z.coerce.number().min(0).optional(),
  categoryId: z.string().optional(),
  notes: z.string().optional(),
  items: z.array(z.object({
    description: z.string().min(1, 'Açıklama zorunludur'),
    quantity: z.coerce.number().min(0.0001, 'Miktar 0 dan büyük olmalıdır'),
    unit: z.enum(['ADET', 'KG', 'LT', 'M', 'M2', 'M3', 'PAKET', 'KUTU', 'SAAT', 'GUN'] as const),
    unitPrice: z.coerce.number().min(0, 'Birim fiyat 0 dan küçük olamaz'),
    taxRate: z.coerce.number().min(0).max(100),
    productCode: z.string().optional(),
    taxAmount: z.number().optional(), // Calculated, strictly for display/internal use
    total: z.number().optional(), // Calculated
  })).min(1, 'En az bir kalem eklemelisiniz'),
});

type InvoiceFormValues = z.infer<typeof invoiceSchema>;

interface InvoiceFormProps {
  initialData?: any;
  categories: Category[];
  onSubmit: (data: CreateInvoiceRequest) => void;
  isLoading: boolean;
}

export function InvoiceForm({ initialData, categories, onSubmit, isLoading }: InvoiceFormProps) {
  const defaultValues: Partial<InvoiceFormValues> = {
    invoiceDate: new Date(),
    currency: 'TRY',
    items: [{ description: '', quantity: 1, unit: 'ADET', unitPrice: 0, taxRate: 20 }],
    ...initialData,
    invoiceDate: initialData?.invoiceDate ? new Date(initialData.invoiceDate) : new Date(),
    dueDate: initialData?.dueDate ? new Date(initialData.dueDate) : undefined,
  };

  const form = useForm<InvoiceFormValues>({
    resolver: zodResolver(invoiceSchema),
    defaultValues,
    mode: 'onChange',
  });

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: 'items',
  });

  const { watch } = form;
  const watchedItems = watch('items');
  const watchedCurrency = watch('currency');

  const [totals, setTotals] = useState({ subtotal: 0, taxAmount: 0, totalAmount: 0 });

  useEffect(() => {
    const newTotals = watchedItems.reduce(
      (acc, item) => {
        const qty = Number(item.quantity) || 0;
        const price = Number(item.unitPrice) || 0;
        const taxRate = Number(item.taxRate) || 0;
        
        const subtotal = qty * price;
        const tax = subtotal * (taxRate / 100);
        
        return {
          subtotal: acc.subtotal + subtotal,
          taxAmount: acc.taxAmount + tax,
          totalAmount: acc.totalAmount + subtotal + tax,
        };
      },
      { subtotal: 0, taxAmount: 0, totalAmount: 0 }
    );
    setTotals(newTotals);
  }, [watchedItems]);

  const handleSubmit = (values: InvoiceFormValues) => {
    const formattedData: CreateInvoiceRequest = {
      ...values,
      invoiceDate: format(values.invoiceDate, 'yyyy-MM-dd'),
      dueDate: values.dueDate ? format(values.dueDate, 'yyyy-MM-dd') : undefined,
      categoryId: values.categoryId === 'none' ? undefined : values.categoryId,
      supplierTaxNumber: values.supplierTaxNumber || undefined,
      supplierEmail: values.supplierEmail || undefined,
    };
    onSubmit(formattedData);
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-8">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Invoice Info */}
          <Card>
            <CardHeader>
              <CardTitle>Fatura Bilgileri</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FormField
                control={form.control}
                name="invoiceNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Fatura No</FormLabel>
                    <FormControl>
                      <Input placeholder="FTR-2024-..." {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="invoiceDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel>Fatura Tarihi</FormLabel>
                      <Popover>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button
                              variant={"outline"}
                              className={cn(
                                "w-full pl-3 text-left font-normal",
                                !field.value && "text-muted-foreground"
                              )}
                            >
                              {field.value ? (
                                format(field.value, "d MMMM yyyy", { locale: tr })
                              ) : (
                                <span>Tarih seçin</span>
                              )}
                              <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value}
                            onSelect={field.onChange}
                            disabled={(date) =>
                              date > new Date() || date < new Date("1900-01-01")
                            }
                            initialFocus
                          />
                        </PopoverContent>
                      </Popover>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="dueDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel>Vade Tarihi</FormLabel>
                      <Popover>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button
                              variant={"outline"}
                              className={cn(
                                "w-full pl-3 text-left font-normal",
                                !field.value && "text-muted-foreground"
                              )}
                            >
                              {field.value ? (
                                format(field.value, "d MMMM yyyy", { locale: tr })
                              ) : (
                                <span>Tarih seçin</span>
                              )}
                              <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value}
                            onSelect={field.onChange}
                            disabled={(date) => date < new Date("1900-01-01")}
                            initialFocus
                          />
                        </PopoverContent>
                      </Popover>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

               <div className="grid grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="currency"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Para Birimi</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Seçiniz" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="TRY">TRY</SelectItem>
                            <SelectItem value="USD">USD</SelectItem>
                            <SelectItem value="EUR">EUR</SelectItem>
                            <SelectItem value="GBP">GBP</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  
                  <FormField
                    control={form.control}
                    name="categoryId"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Kategori</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Seçiniz" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="none">Kategorisiz</SelectItem>
                            {categories.map((category) => (
                              <SelectItem key={category.id} value={category.id}>
                                {category.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
               </div>
            </CardContent>
          </Card>

          {/* Supplier Info */}
          <Card>
            <CardHeader>
              <CardTitle>Tedarikçi Bilgileri</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FormField
                control={form.control}
                name="supplierName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Firma Adı</FormLabel>
                    <FormControl>
                      <Input placeholder="Firma Adı" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="supplierTaxNumber"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Vergi No / TCKN</FormLabel>
                      <FormControl>
                        <Input placeholder="Vergi No" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                 <FormField
                  control={form.control}
                  name="supplierTaxOffice"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Vergi Dairesi</FormLabel>
                      <FormControl>
                        <Input placeholder="Vergi Dairesi" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

               <div className="grid grid-cols-2 gap-4">
                 <FormField
                  control={form.control}
                  name="supplierPhone"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Telefon</FormLabel>
                      <FormControl>
                        <Input placeholder="Telefon" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                 <FormField
                  control={form.control}
                  name="supplierEmail"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>E-posta</FormLabel>
                      <FormControl>
                        <Input placeholder="E-posta" type="email" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
               </div>
               
               <FormField
                  control={form.control}
                  name="supplierAddress"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Adres</FormLabel>
                      <FormControl>
                        <Textarea placeholder="Adres" className="min-h-[60px]" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
            </CardContent>
          </Card>
        </div>

        {/* Invoice Items */}
        <Card>
           <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Fatura Kalemleri</CardTitle>
              <Button type="button" variant="secondary" size="sm" onClick={() => append({ 
                description: '', quantity: 1, unit: 'ADET', unitPrice: 0, taxRate: 20 
              })}>
                <Plus className="h-4 w-4 mr-2" /> Kalem Ekle
              </Button>
           </CardHeader>
           <CardContent>
             <div className="space-y-4">
               {fields.map((field, index) => (
                 <div key={field.id} className="grid grid-cols-12 gap-2 items-end border-b pb-4 last:border-0 last:pb-0">
                    <div className="col-span-4">
                      <FormField
                        control={form.control}
                        name={`items.${index}.description`}
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel className={cn(index !== 0 && "sr-only")}>Açıklama</FormLabel>
                            <FormControl>
                              <Input placeholder="Ürün/Hizmet" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    
                    <div className="col-span-1">
                      <FormField
                        control={form.control}
                        name={`items.${index}.quantity`}
                        render={({ field }) => (
                          <FormItem>
                             <FormLabel className={cn(index !== 0 && "sr-only")}>Miktar</FormLabel>
                            <FormControl>
                              <Input type="number" min="0" step="any" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    
                    <div className="col-span-2">
                       <FormField
                        control={form.control}
                        name={`items.${index}.unit`}
                        render={({ field }) => (
                          <FormItem>
                             <FormLabel className={cn(index !== 0 && "sr-only")}>Birim</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Birim" />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                {['ADET', 'KG', 'LT', 'M', 'M2', 'M3', 'PAKET', 'KUTU', 'SAAT', 'GUN'].map(u => (
                                  <SelectItem key={u} value={u}>{u}</SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                    
                    <div className="col-span-2">
                      <FormField
                        control={form.control}
                        name={`items.${index}.unitPrice`}
                        render={({ field }) => (
                          <FormItem>
                             <FormLabel className={cn(index !== 0 && "sr-only")}>Birim Fiyat</FormLabel>
                            <FormControl>
                              <Input type="number" min="0" step="any" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    <div className="col-span-2">
                       <FormField
                        control={form.control}
                        name={`items.${index}.taxRate`}
                        render={({ field }) => (
                          <FormItem>
                             <FormLabel className={cn(index !== 0 && "sr-only")}>KDV %</FormLabel>
                            <FormControl>
                              <Input type="number" min="0" max="100" step="1" {...field} />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>

                    <div className="col-span-1">
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="text-destructive"
                        onClick={() => remove(index)}
                        disabled={fields.length === 1}
                      >
                         <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                 </div>
               ))}
             </div>
             
             <Separator className="my-6" />
             
             <div className="flex flex-col gap-2 items-end text-sm">
                <div className="flex justify-between w-[250px]">
                  <span className="text-muted-foreground">Ara Toplam:</span>
                  <span className="font-medium">{formatCurrency(totals.subtotal, watchedCurrency)}</span>
                </div>
                <div className="flex justify-between w-[250px]">
                  <span className="text-muted-foreground">Toplam KDV:</span>
                  <span className="font-medium">{formatCurrency(totals.taxAmount, watchedCurrency)}</span>
                </div>
                <Separator className="w-[250px]" />
                <div className="flex justify-between w-[250px] text-lg font-bold">
                  <span>Genel Toplam:</span>
                  <span className="text-primary">{formatCurrency(totals.totalAmount, watchedCurrency)}</span>
                </div>
             </div>
           </CardContent>
        </Card>
        
         <Card>
            <CardContent className="pt-6">
               <FormField
                  control={form.control}
                  name="notes"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Notlar</FormLabel>
                      <FormControl>
                        <Textarea placeholder="Fatura ile ilgili notlar..." className="min-h-[80px]" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
            </CardContent>
         </Card>

        <div className="flex justify-end gap-4">
           <Button type="button" variant="outline" onClick={() => window.history.back()}>İptal</Button>
           <Button type="submit" disabled={isLoading}>
             {isLoading ? 'Kaydediliyor...' : 'Kaydet'}
           </Button>
        </div>
      </form>
    </Form>
  );
}
