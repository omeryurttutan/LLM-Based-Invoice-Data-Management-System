import React, { useEffect } from 'react';
import { useFieldArray, useFormContext } from 'react-hook-form';
import { useTranslations } from 'next-intl';
import { Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { FormSection } from './form-section';

interface VerificationItemsProps {
  readOnly?: boolean;
}

export const VerificationItems: React.FC<VerificationItemsProps> = ({ readOnly = false }) => {
  const t = useTranslations('invoices.verification');
  const tForm = useTranslations('invoices.form');
  const { register, control, watch, setValue } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name: "items",
  });

  // Watch for changes to calculate totals
  const items = watch("items");

  useEffect(() => {
    if (!readOnly) {
      items.forEach((item: any, index: number) => {
        const quantity = parseFloat(item.quantity) || 0;
        const unitPrice = parseFloat(item.unitPrice) || 0;
        const taxRate = parseFloat(item.taxRate) || 0;

        const calculatedLineTotal = quantity * unitPrice;
        const calculatedTaxAmount = calculatedLineTotal * (taxRate / 100);

        // Only update if significantly different to avoid loops, 
        // but hook form setValue should handle this if we use shouldDirty: true/false appropriately.
        // Actually, better to just update if the value in form is different.

        // We use toFixed(2) for display/storage consistency
        const currentLineTotal = parseFloat(item.subtotal) || 0;
        const currentTaxAmount = parseFloat(item.taxAmount) || 0;

        if (Math.abs(calculatedLineTotal - currentLineTotal) > 0.01) {
          setValue(`items.${index}.subtotal`, parseFloat(calculatedLineTotal.toFixed(2)));
        }
        if (Math.abs(calculatedTaxAmount - currentTaxAmount) > 0.01) {
          setValue(`items.${index}.taxAmount`, parseFloat(calculatedTaxAmount.toFixed(2)));
        }
      });
    }
  }, [items, setValue, readOnly]);

  return (
    <FormSection
      title="Fatura Kalemleri"
      rightElement={
        !readOnly && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => append({
              description: '',
              quantity: 1,
              unit: 'ADET',
              unitPrice: 0,
              taxRate: 20,
              taxAmount: 0,
              subtotal: 0
            })}
          >
            <Plus className="h-4 w-4 mr-2" />
            Kalem Ekle
          </Button>
        )
      }
    >
      <div className="col-span-1 md:col-span-2 overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[30%]">Açıklama</TableHead>
              <TableHead className="w-[10%]">Miktar</TableHead>
              <TableHead className="w-[10%]">Birim</TableHead>
              <TableHead className="w-[15%]">Birim Fiyat</TableHead>
              <TableHead className="w-[10%]">KDV %</TableHead>
              <TableHead className="w-[10%]">KDV Tutarı</TableHead>
              <TableHead className="w-[15%]">Toplam</TableHead>
              {!readOnly && <TableHead className="w-[50px]"></TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {fields.map((field, index) => (
              <TableRow key={field.id}>
                <TableCell>
                  <Input
                    {...register(`items.${index}.description`)}
                    disabled={readOnly}
                    className="min-w-[150px]"
                  />
                </TableCell>
                <TableCell>
                  <Input
                    type="number"
                    step="0.01"
                    {...register(`items.${index}.quantity`, { valueAsNumber: true })}
                    disabled={readOnly}
                  />
                </TableCell>
                <TableCell>
                  <Input
                    {...register(`items.${index}.unit`)}
                    disabled={readOnly}
                  />
                </TableCell>
                <TableCell>
                  <Input
                    type="number"
                    step="0.01"
                    {...register(`items.${index}.unitPrice`, { valueAsNumber: true })}
                    disabled={readOnly}
                  />
                </TableCell>
                <TableCell>
                  <Input
                    type="number"
                    step="1"
                    {...register(`items.${index}.taxRate`, { valueAsNumber: true })}
                    disabled={readOnly}
                  />
                </TableCell>
                <TableCell>
                  <Input
                    type="number"
                    step="0.01"
                    {...register(`items.${index}.taxAmount`, { valueAsNumber: true })}
                    disabled={readOnly}
                  // calculated but editable
                  />
                </TableCell>
                <TableCell>
                  <Input
                    type="number"
                    step="0.01"
                    {...register(`items.${index}.subtotal`, { valueAsNumber: true })}
                    readOnly
                    className="bg-gray-50 dark:bg-gray-900"
                  />
                </TableCell>
                {!readOnly && (
                  <TableCell>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => remove(index)}
                      className="text-red-500 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/50"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </TableCell>
                )}
              </TableRow>
            ))}
            {fields.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center py-8 text-gray-500">
                  Henüz kalem eklenmemiş. &quot;Kalem Ekle&quot; butonunu kullanarak ekleyebilirsiniz.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </FormSection>
  );
};
