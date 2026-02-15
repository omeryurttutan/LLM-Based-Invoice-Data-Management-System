import { useState, useEffect } from 'react';
import { Download, FileSpreadsheet, FileText, Loader2, AlertTriangle, Building, Cloud, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { useToast } from '@/hooks/use-toast';
import { useInvoiceFilters } from '@/hooks/use-invoice-filters';
import apiClient from '@/lib/api-client';
import { format } from 'date-fns';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Separator } from '@/components/ui/separator';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { useTranslations } from 'next-intl';

interface ExportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  totalCount: number;
}

interface ExportFormatMetadata {
  format: string;
  label: string;
  description: string;
  category: 'GENERAL' | 'ACCOUNTING';
  fileExtension: string;
  icon: string;
}

const iconMap: Record<string, React.ElementType> = {
  'file-spreadsheet': FileSpreadsheet,
  'file-text': FileText,
  'building': Building,
  'cloud': Cloud,
};

export function ExportDialog({ open, onOpenChange, totalCount }: ExportDialogProps) {
  const t = useTranslations('export');
  const tCommon = useTranslations('common');
  const [formats, setFormats] = useState<ExportFormatMetadata[]>([]);
  const [selectedFormat, setSelectedFormat] = useState<string>('XLSX');
  const [includeItems, setIncludeItems] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [isLoadingFormats, setIsLoadingFormats] = useState(false);
  const { filters } = useInvoiceFilters();
  const { toast } = useToast();

  useEffect(() => {
    if (open) {
      const loadFormats = async () => {
        try {
          setIsLoadingFormats(true);
          const response = await apiClient.get<ExportFormatMetadata[]>('/invoices/export/formats');
          setFormats(response.data);
        } catch (error) {
          console.error('Failed to load export formats:', error);
          toast({
            title: t('formatsError.title'),
            description: t('formatsError.description'),
            variant: "destructive"
          });
          // Fallback formats
          setFormats([
            { format: 'XLSX', label: 'Excel (XLSX)', description: t('formats.xlsx.description'), category: 'GENERAL', fileExtension: 'xlsx', icon: 'file-spreadsheet' },
            { format: 'CSV', label: 'CSV', description: t('formats.csv.description'), category: 'GENERAL', fileExtension: 'csv', icon: 'file-text' }
          ]);
        } finally {
          setIsLoadingFormats(false);
        }
      };

      loadFormats();
    }
  }, [open, toast, t]);

  const currentFormat = formats.find(f => f.format === selectedFormat);
  const isAccountingFormat = currentFormat?.category === 'ACCOUNTING';

  // Accounting formats always require line items
  useEffect(() => {
    if (isAccountingFormat) {
      setIncludeItems(true);
    }
  }, [isAccountingFormat]);

  const handleExport = async () => {
    try {
      setIsExporting(true);

      const response = await apiClient.get('/invoices/export', {
        params: {
          ...filters,
          format: selectedFormat,
          includeItems: isAccountingFormat ? true : includeItems
        },
        responseType: 'blob'
      });

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;

      // Generate filename
      const dateStr = format(new Date(), 'yyyy-MM-dd_HHmmss');
      const ext = currentFormat?.fileExtension || 'dat';
      const prefix = isAccountingFormat ? selectedFormat : 'faturalar';
      const filename = `${prefix}_${dateStr}.${ext}`;

      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      toast({
        title: t('success.title'),
        description: t('success.description', { filename }),
        variant: "default"
      });

      onOpenChange(false);
    } catch (error) {
      console.error('Export failed:', error);
      toast({
        title: t('error.title'),
        description: t('error.description'),
        variant: "destructive"
      });
    } finally {
      setIsExporting(false);
    }
  };

  const isLargeDataset = totalCount > 10000;
  const isEmptyDataset = totalCount === 0;

  const generalFormats = formats.filter(f => f.category === 'GENERAL');
  const accountingFormats = formats.filter(f => f.category === 'ACCOUNTING');

  const renderFormatOption = (format: ExportFormatMetadata) => {
    const Icon = iconMap[format.icon] || FileText;

    return (
      <div key={format.format}>
        <RadioGroupItem value={format.format} id={format.format} className="peer sr-only" />
        <Label
          htmlFor={format.format}
          className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary [&:has([data-state=checked])]:border-primary cursor-pointer h-full"
        >
          <Icon className="mb-2 h-6 w-6" />
          <span className="font-medium text-center">{// Use translated label if available, fallback to format label
            // NOTE: Backend returns labels. If we want frontend translation, we need to map format.format to key.
            // For now assume backend returns translatable key or we just stick with fallback if custom.
            // Or better, just display format.label since it might be dynamic from backend plugins.
            format.label
          }</span>
        </Label>
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[550px]">
        <DialogHeader>
          <DialogTitle>{t('title')}</DialogTitle>
          <DialogDescription>
            {t.rich('description', {
              count: totalCount,
              strong: (chunks) => <strong>{chunks}</strong>
            })}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-6 py-4">
          {isEmptyDataset && (
            <div className="flex items-center gap-2 p-3 text-sm text-yellow-600 bg-yellow-50 rounded-md border border-yellow-200">
              <AlertTriangle className="h-4 w-4" />
              <span>{t('noData')}</span>
            </div>
          )}

          {isLargeDataset && (
            <div className="flex items-center gap-2 p-3 text-sm text-blue-600 bg-blue-50 rounded-md border border-blue-200">
              <Loader2 className="h-4 w-4" />
              <span>{t('largeDataset', { count: totalCount })}</span>
            </div>
          )}

          {isLoadingFormats ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <div className="space-y-6">
              <RadioGroup value={selectedFormat} onValueChange={setSelectedFormat}>
                {/* General Formats */}
                <div className="space-y-3">
                  <Label>{t('sections.general')}</Label>
                  <div className="grid grid-cols-2 gap-4">
                    {generalFormats.map(renderFormatOption)}
                  </div>
                </div>

                <Separator className="my-4" />

                {/* Accounting Formats */}
                <div className="space-y-3">
                  <Label className="flex items-center gap-2">
                    {t('sections.accounting')}
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Info className="h-4 w-4 text-muted-foreground cursor-help" />
                        </TooltipTrigger>
                        <TooltipContent>
                          <p>{t('accountingTooltip')}</p>
                        </TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                  </Label>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                    {accountingFormats.map(renderFormatOption)}
                  </div>
                </div>
              </RadioGroup>

              <div className="bg-muted/50 p-3 rounded-md text-xs text-muted-foreground">
                <p>{currentFormat?.description}</p>
                <p className="mt-1">{t('fileExtension')}: <strong>.{currentFormat?.fileExtension}</strong></p>
              </div>

              {isAccountingFormat && (
                <Alert variant="default" className="bg-blue-50 border-blue-200 text-blue-800">
                  <Info className="h-4 w-4" />
                  <AlertTitle>{t('importantInfo')}</AlertTitle>
                  <AlertDescription>
                    {t.rich('accountingWarning', {
                      strong: (chunks) => <strong>{chunks}</strong>
                    })}
                  </AlertDescription>
                </Alert>
              )}
            </div>
          )}

          <div className="flex items-center space-x-2">
            <Checkbox
              id="includeItems"
              checked={includeItems}
              onCheckedChange={(c: boolean | 'indeterminate') => setIncludeItems(!!c)}
              disabled={isAccountingFormat}
            />
            <div className="grid gap-1.5 leading-none">
              <Label htmlFor="includeItems" className="cursor-pointer">
                {t('options.includeItems')}
              </Label>
              <p className="text-xs text-muted-foreground">
                {isAccountingFormat
                  ? t('options.includeItemsMandatory')
                  : t('options.includeItemsDesc')}
              </p>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>{tCommon('cancel')}</Button>
          <Button onClick={handleExport} disabled={isExporting || isEmptyDataset || isLoadingFormats}>
            {isExporting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t('exporting')}...
              </>
            ) : (
              <>
                <Download className="mr-2 h-4 w-4" />
                {t('download')}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
