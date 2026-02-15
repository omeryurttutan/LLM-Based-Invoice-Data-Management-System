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
            title: "Format listesi alınamadı",
            description: "Varsayılan formatlar kullanılacak.",
            variant: "destructive"
          });
          // Fallback formats
          setFormats([
            { format: 'XLSX', label: 'Excel (XLSX)', description: 'Excel formatı', category: 'GENERAL', fileExtension: 'xlsx', icon: 'file-spreadsheet' },
            { format: 'CSV', label: 'CSV', description: 'CSV formatı', category: 'GENERAL', fileExtension: 'csv', icon: 'file-text' }
          ]);
        } finally {
          setIsLoadingFormats(false);
        }
      };
      
      loadFormats();
    }
  }, [open, toast]);

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
        title: "Dışa aktarım başarılı",
        description: `Dosya indirildi: ${filename}`,
        variant: "default"
      });
      
      onOpenChange(false);
    } catch (error) {
      console.error('Export failed:', error);
      toast({
        title: "Dışa aktarım başarısız",
        description: "Bir hata oluştu. Lütfen tekrar deneyin.",
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
          <span className="font-medium text-center">{format.label}</span>
        </Label>
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[550px]">
        <DialogHeader>
          <DialogTitle>Fatura Verilerini Dışa Aktar</DialogTitle>
          <DialogDescription>
            Mevcut filtrelerinize uyan <strong>{totalCount}</strong> fatura dışa aktarılacaktır.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-6 py-4">
          {isEmptyDataset && (
             <div className="flex items-center gap-2 p-3 text-sm text-yellow-600 bg-yellow-50 rounded-md border border-yellow-200">
               <AlertTriangle className="h-4 w-4" />
               <span>Dışa aktarılacak fatura bulunamadı.</span>
             </div>
          )}

          {isLargeDataset && (
             <div className="flex items-center gap-2 p-3 text-sm text-blue-600 bg-blue-50 rounded-md border border-blue-200">
               <Loader2 className="h-4 w-4" />
               <span>Büyük veri seti ({totalCount} kayıt). İndirme işlemi birkaç dakika sürebilir.</span>
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
                  <Label>Genel Formatlar</Label>
                  <div className="grid grid-cols-2 gap-4">
                    {generalFormats.map(renderFormatOption)}
                  </div>
                </div>

                <Separator className="my-4" />

                {/* Accounting Formats */}
                <div className="space-y-3">
                  <Label className="flex items-center gap-2">
                    Muhasebe Yazılımları
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Info className="h-4 w-4 text-muted-foreground cursor-help" />
                        </TooltipTrigger>
                        <TooltipContent>
                          <p>Bu formatlar doğrudan muhasebe programlarına aktarım içindir.</p>
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
                <p className="mt-1">Dosya uzantısı: <strong>.{currentFormat?.fileExtension}</strong></p>
              </div>

              {isAccountingFormat && (
                <Alert variant="default" className="bg-blue-50 border-blue-200 text-blue-800">
                  <Info className="h-4 w-4" />
                  <AlertTitle>Önemli Bilgi</AlertTitle>
                  <AlertDescription>
                    Muhasebe formatları sadece <strong>Onaylanmış (Verified)</strong> faturaları içerir. 
                    Bekleyen veya reddedilen faturalar otomatik olarak hariç tutulacaktır.
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
                Fatura kalemlerini dahil et
              </Label>
              <p className="text-xs text-muted-foreground">
                {isAccountingFormat 
                  ? "Muhasebe formatları için bu seçenek zorunludur." 
                  : "Seçilirse, her fatura kalemi için ayrı bir satır oluşturulur."}
              </p>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>İptal</Button>
          <Button onClick={handleExport} disabled={isExporting || isEmptyDataset || isLoadingFormats}>
            {isExporting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Hazırlanıyor...
              </>
            ) : (
              <>
                <Download className="mr-2 h-4 w-4" />
                İndir
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
