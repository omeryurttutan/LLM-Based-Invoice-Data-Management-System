import React, { useState, useEffect, useCallback } from 'react';
import { useForm, FormProvider } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useRouter } from 'next/navigation';
import { verificationSchema, VerificationFormData } from './verification-schema';
import { InvoiceDetail, ValidationIssue, ExtractionCorrection } from '@/types/invoice';
import { invoiceService } from '@/services/invoice-service';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2, Check, X, Save, RotateCw, AlertTriangle } from 'lucide-react';
import { toast } from 'sonner';
import { FormSection } from './form-section';
import { VerificationItems } from './verification-items';
import { ValidatedField } from './validated-field';
import { ConfidenceBadge } from './confidence-badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from '@/components/ui/label';
import { UnitType, Currency } from '@/types/invoice';

interface VerificationFormProps {
  invoice: InvoiceDetail;
}

export const VerificationForm: React.FC<VerificationFormProps> = ({ invoice }) => {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [confidenceScore, setConfidenceScore] = useState<number>(invoice.confidenceScore || 0);
  const [validationIssues, setValidationIssues] = useState<ValidationIssue[]>(invoice.validationIssues || []);
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectDialog, setShowRejectDialog] = useState(false);
  const [showVerifyDialog, setShowVerifyDialog] = useState(false);

  // Map initial values
  const defaultValues: Partial<VerificationFormData> = {
    invoiceNumber: invoice.invoiceNumber,
    invoiceDate: invoice.invoiceDate,
    dueDate: invoice.dueDate || '',
    supplierName: invoice.supplierName,
    supplierTaxNumber: invoice.supplierTaxNumber || '',
    supplierAddress: invoice.supplierAddress || '',
    buyerName: invoice.buyerName || '',
    buyerTaxNumber: invoice.buyerTaxNumber || '',
    items: invoice.items.map(item => ({
      ...item,
      subtotal: item.subtotal, 
      unit: item.unit || 'ADET',
    })),
    subtotal: invoice.subtotal,
    taxAmount: invoice.taxAmount,
    totalAmount: invoice.totalAmount,
    currency: invoice.currency,
    notes: invoice.notes || '',
  };

  const methods = useForm<VerificationFormData>({
    resolver: zodResolver(verificationSchema),
    defaultValues,
    mode: 'onChange',
  });

  const { handleSubmit, watch, formState: { errors, dirtyFields }, reset } = methods;

  // Helper to collect corrections
  const getCorrections = useCallback((data: VerificationFormData): ExtractionCorrection[] => {
    // Only track fields that are dirty
    const corrections: ExtractionCorrection[] = [];
    
        // We iterate over dirtyFields
    Object.keys(dirtyFields).forEach((key) => {
        const fieldKey = key as keyof VerificationFormData;
        if (fieldKey === 'items') return; // Complex handling for items optional

        const originalValue = (invoice as unknown as Record<string, any>)[fieldKey];
        const newValue = data[fieldKey];

        if (originalValue != newValue) {
             corrections.push({
                field: fieldKey,
                originalValue: originalValue,
                correctedValue: newValue
             });
        }
   });
   
   return corrections;
  }, [dirtyFields, invoice]);

  const handleSaveDraft = useCallback(async () => {
    const data = methods.getValues();
    setIsSubmitting(true);
    try {
        const corrections = getCorrections(data);
        await invoiceService.updateInvoice(invoice.id, {
            ...data,
            items: data.items.map((item, index) => ({ 
                ...item, 
                unit: item.unit as UnitType,
                lineNumber: index + 1,
                totalAmount: (item.subtotal || 0) + (item.taxAmount || 0)
            })),
            extractionCorrections: corrections,
        });
        
        // Reset dirty state with new values
        reset(data);

        toast.success("Taslak Kaydedildi", { description: "Değişiklikler taslak olarak kaydedildi." });
    } catch (error) {
        console.error("Taslak kaydedilemedi", error);
        toast.error("Hata", { description: "Taslak kaydedilirken bir hata oluştu." });
    } finally {
        setIsSubmitting(false);
    }
  }, [invoice.id, methods, reset, getCorrections]);

  // Amount consistency check
  const watchedSubtotal = watch('subtotal');
  const watchedTaxAmount = watch('taxAmount');
  const watchedTotalAmount = watch('totalAmount');
  const isAmountConsistent = Math.abs((watchedSubtotal || 0) + (watchedTaxAmount || 0) - (watchedTotalAmount || 0)) < 0.05;

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        setShowVerifyDialog(true);
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        handleSaveDraft();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleSaveDraft]);

  // Unsaved changes warning (browser refresh/close)
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (methods.formState.isDirty) {
        e.preventDefault();
        e.returnValue = '';
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [methods.formState.isDirty]);

  // Helper to find issue for a field
  const getIssue = (fieldName: string) => {
    return validationIssues.find(i => i.field === fieldName);
  };

  const handleRevalidate = async () => {
    const data = methods.getValues();
    setIsValidating(true);
    try {
        const result = await invoiceService.validateExtraction({
            ...data,
            items: data.items.map((item, index) => ({ 
                ...item, 
                unit: item.unit as UnitType,
                lineNumber: index + 1,
                totalAmount: (item.subtotal || 0) + (item.taxAmount || 0)
            })),
        });
        
        setConfidenceScore(result.confidenceScore);
        setValidationIssues(result.validationIssues);
        toast.success("Yeniden Doğrulama Tamamlandı", { description: `Güven Skoru: ${result.confidenceScore}/100` });
    } catch (error) {
        toast.error("Hata", { description: "Yeniden doğrulama başarısız oldu." });
    } finally {
        setIsValidating(false);
    }
  };

  const onVerify = async (data: VerificationFormData) => {
    setIsSubmitting(true);
    try {
      // 1. Update Invoice Data
      const corrections = getCorrections(data);
      await invoiceService.updateInvoice(invoice.id, {
        ...data,
        items: data.items.map((item, index) => ({ 
            ...item, 
            unit: item.unit as UnitType, 
            lineNumber: index + 1,
            totalAmount: (item.subtotal || 0) + (item.taxAmount || 0)
        })), // Cast unit
        extractionCorrections: corrections,
      });

      // 2. Verify Invoice
      await invoiceService.verifyInvoice(invoice.id);

      toast.success("Fatura Onaylandı", { description: "Fatura başarıyla doğrulandı ve onaylandı." });

      router.push('/invoices'); // Or next invoice
    } catch (error) {
       toast.error("Hata", { description: "Fatura onaylanırken bir hata oluştu." });
    } finally {
      setIsSubmitting(false);
      setShowVerifyDialog(false);
    }
  };

  const handleReject = async () => {
    if (!rejectReason) {
         toast.error("Hata", { description: "Lütfen bir ret sebebi belirtin." });
        return;
    }

    setIsSubmitting(true);
    try {
      await invoiceService.rejectInvoice(invoice.id, { rejectionReason: rejectReason });
      toast.success("Fatura Reddedildi", { description: "Fatura reddedildi." });
      router.push('/invoices');
    } catch (error) {
       toast.error("Hata", { description: "Fatura reddedilirken bir hata oluştu." });
    } finally {
      setIsSubmitting(false);
      setShowRejectDialog(false);
    }
  };

  const isReadOnly = invoice.status === 'VERIFIED' || invoice.status === 'REJECTED';

  return (
    <FormProvider {...methods}>
      <div className="flex flex-col h-full bg-gray-50 dark:bg-gray-900 border-l border-gray-200 dark:border-gray-800 overflow-hidden">
        
        {/* Header */}
        <div className="flex items-center justify-between p-4 bg-white dark:bg-gray-950 border-b border-gray-200 dark:border-gray-800">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold">{invoice.invoiceNumber}</h2>
            <ConfidenceBadge score={confidenceScore} />
            {invoice.sourceType === 'LLM' && (
                <span className="text-xs px-2 py-1 bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 rounded-full font-medium">
                    {invoice.llmProvider || 'AI'}
                </span>
            )}
            {invoice.status === 'VERIFIED' && (
                <span className="text-xs px-2 py-1 bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300 rounded-full font-medium">
                    ONAYLANDI
                </span>
            )}
            {invoice.status === 'REJECTED' && (
                <span className="text-xs px-2 py-1 bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300 rounded-full font-medium">
                    REDDEDİLDİ
                </span>
            )}
          </div>
          
          {!isReadOnly && (
             <div className="flex gap-2">
                 <Button 
                    variant="outline" 
                    size="sm" 
                    onClick={handleRevalidate} 
                    disabled={isValidating || isSubmitting}
                 >
                    {isValidating ? <Loader2 className="h-4 w-4 animate-spin mr-2"/> : <RotateCw className="h-4 w-4 mr-2"/>}
                    Yeniden Doğrula
                 </Button>
                 <Button 
                    variant="outline" 
                    size="sm" 
                    onClick={handleSaveDraft}
                    disabled={isSubmitting}
                 >
                    <Save className="h-4 w-4 mr-2" />
                    Taslak Kaydet
                 </Button>
             </div>
          )}
        </div>

        {/* Form Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-6">
            <form onSubmit={handleSubmit(onVerify)} className="space-y-6">
                
                {/* Section A: General */}
                <FormSection title="Genel Bilgiler">
                    <ValidatedField issue={getIssue('invoiceNumber')}>
                        <Label>Fatura Numarası</Label>
                        <Input {...methods.register('invoiceNumber')} disabled={isReadOnly} />
                        {errors.invoiceNumber && <span className="text-destructive text-xs">{errors.invoiceNumber.message}</span>}
                    </ValidatedField>
                    
                    <ValidatedField issue={getIssue('invoiceDate')}>
                        <Label>Fatura Tarihi</Label>
                        <Input type="date" {...methods.register('invoiceDate')} disabled={isReadOnly} />
                        {errors.invoiceDate && <span className="text-destructive text-xs">{errors.invoiceDate.message}</span>}
                    </ValidatedField>
                    
                    <ValidatedField issue={getIssue('dueDate')}>
                        <Label>Vade Tarihi</Label>
                        <Input type="date" {...methods.register('dueDate')} disabled={isReadOnly} />
                    </ValidatedField>

                    <ValidatedField issue={getIssue('currency')}>
                        <Label>Para Birimi</Label>
                        <Select 
                            onValueChange={(val: Currency) => methods.setValue('currency', val)} 
                            defaultValue={invoice.currency}
                            disabled={isReadOnly}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="Seçiniz" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="TRY">TRY - Türk Lirası</SelectItem>
                                <SelectItem value="USD">USD - Amerikan Doları</SelectItem>
                                <SelectItem value="EUR">EUR - Euro</SelectItem>
                                <SelectItem value="GBP">GBP - İngiliz Sterlini</SelectItem>
                            </SelectContent>
                        </Select>
                    </ValidatedField>
                </FormSection>

                {/* Section B: Supplier */}
                <FormSection title="Tedarikçi Bilgileri">
                    <ValidatedField issue={getIssue('supplierName')}>
                        <Label>Tedarikçi Adı</Label>
                        <Input {...methods.register('supplierName')} disabled={isReadOnly} />
                         {errors.supplierName && <span className="text-destructive text-xs">{errors.supplierName.message}</span>}
                    </ValidatedField>

                    <ValidatedField issue={getIssue('supplierTaxNumber')}>
                        <Label>Vergi Numarası</Label>
                        <Input {...methods.register('supplierTaxNumber')} disabled={isReadOnly} maxLength={11} />
                    </ValidatedField>

                    <ValidatedField issue={getIssue('supplierAddress')} className="col-span-1 md:col-span-2">
                        <Label>Adres</Label>
                        <Textarea {...methods.register('supplierAddress')} disabled={isReadOnly} rows={2} />
                    </ValidatedField>
                </FormSection>

                 {/* Section C: Buyer */}
                <FormSection title="Alıcı Bilgileri">
                     <ValidatedField issue={getIssue('buyerName')}>
                        <Label>Alıcı Adı</Label>
                        <Input {...methods.register('buyerName')} disabled={isReadOnly} />
                    </ValidatedField>
                    
                     <ValidatedField issue={getIssue('buyerTaxNumber')}>
                        <Label>Vergi Numarası</Label>
                        <Input {...methods.register('buyerTaxNumber')} disabled={isReadOnly} maxLength={11} />
                    </ValidatedField>
                </FormSection>

                {/* Section D: Items */}
                <ValidatedField issue={getIssue('items')}>
                     <VerificationItems readOnly={isReadOnly} />
                </ValidatedField>

                {/* Section E: Amounts */}
                <FormSection title="Tutarlar">
                    <ValidatedField issue={getIssue('subtotal')}>
                        <Label>Ara Toplam</Label>
                        <Input type="number" step="0.01" {...methods.register('subtotal', { valueAsNumber: true })} disabled={isReadOnly} />
                    </ValidatedField>

                    <ValidatedField issue={getIssue('taxAmount')}>
                        <Label>KDV Tutarı</Label>
                        <Input type="number" step="0.01" {...methods.register('taxAmount', { valueAsNumber: true })} disabled={isReadOnly} />
                    </ValidatedField>

                    <ValidatedField issue={getIssue('totalAmount')}>
                        <Label>Genel Toplam</Label>
                        <Input type="number" step="0.01" {...methods.register('totalAmount', { valueAsNumber: true })} disabled={isReadOnly} />
                    </ValidatedField>

                    {!isAmountConsistent && (
                        <div className="col-span-1 md:col-span-2 flex items-center text-yellow-600 bg-yellow-50 dark:bg-yellow-900/20 p-2 rounded text-sm">
                            <AlertTriangle className="h-4 w-4 mr-2" />
                            <span>Ara Toplam + KDV Toplamı, Genel Toplama eşit değil! Lütfen kontrol ediniz.</span>
                        </div>
                    )}
                </FormSection>

                {/* Section F: Notes */}
                <FormSection title="Notlar">
                    <div className="col-span-1 md:col-span-2">
                        <Textarea {...methods.register('notes')} disabled={isReadOnly} placeholder="Fatura ile ilgili notlar..." />
                    </div>
                </FormSection>

            </form>
        </div>

        {/* Footer Actions */}
        {!isReadOnly && (
            <div className="p-4 bg-white dark:bg-gray-950 border-t border-gray-200 dark:border-gray-800 flex justify-between items-center gap-4">
                <Button 
                    variant="ghost" 
                    onClick={() => router.back()}
                    disabled={isSubmitting}
                >
                    İptal
                </Button>
                
                <div className="flex gap-2">
                     <Dialog open={showRejectDialog} onOpenChange={setShowRejectDialog}>
                        <DialogTrigger asChild>
                             <Button variant="destructive" disabled={isSubmitting}>
                                <X className="h-4 w-4 mr-2" />
                                Reddet
                            </Button>
                        </DialogTrigger>
                        <DialogContent>
                            <DialogHeader>
                                <DialogTitle>Faturayı Reddet</DialogTitle>
                                <DialogDescription>
                                    Faturayı reddetmek istediğinize emin misiniz? Lütfen bir sebep belirtin.
                                </DialogDescription>
                            </DialogHeader>
                            <Textarea 
                                value={rejectReason} 
                                onChange={(e) => setRejectReason(e.target.value)} 
                                placeholder="Red sebebi..."
                                rows={3}
                            />
                            <DialogFooter>
                                <Button variant="ghost" onClick={() => setShowRejectDialog(false)}>İptal</Button>
                                <Button variant="destructive" onClick={handleReject} disabled={isSubmitting}>Reddet</Button>
                            </DialogFooter>
                        </DialogContent>
                    </Dialog>

                    <Dialog open={showVerifyDialog} onOpenChange={setShowVerifyDialog}>
                        <DialogTrigger asChild>
                            <Button className="bg-green-600 hover:bg-green-700 text-white" disabled={isSubmitting}>
                                <Check className="h-4 w-4 mr-2" />
                                Onayla ve Kaydet
                            </Button>
                        </DialogTrigger>
                        <DialogContent>
                             <DialogHeader>
                                <DialogTitle>Onaylama İşlemi</DialogTitle>
                                <DialogDescription>
                                    Bu faturayı onaylamak istediğinize emin misiniz? Onaylanan faturalar düzenleme için kilitlenir.
                                </DialogDescription>
                            </DialogHeader>
                            <DialogFooter>
                                <Button variant="ghost" onClick={() => setShowVerifyDialog(false)}>İptal</Button>
                                <Button className="bg-green-600 hover:bg-green-700 text-white" onClick={handleSubmit(onVerify)} disabled={isSubmitting}>
                                    {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin mr-2"/> : null}
                                    Onayla
                                </Button>
                            </DialogFooter>
                        </DialogContent>
                    </Dialog>
                </div>
            </div>
        )}
        
        <div className="px-4 py-1 bg-gray-100 dark:bg-gray-800 text-[10px] text-gray-500 flex justify-center gap-4 border-t border-gray-200 dark:border-gray-700">
            <span>Kısayollar: <strong>Tab</strong> (Geçiş)</span>
            <span><strong>Ctrl+Enter</strong> (Onayla)</span>
            <span><strong>Ctrl+S</strong> (Taslak Kaydet)</span>
        </div>

      </div>
    </FormProvider>
  );
};
