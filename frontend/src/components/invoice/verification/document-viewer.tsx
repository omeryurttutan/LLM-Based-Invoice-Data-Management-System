import React, { useEffect, useState } from 'react';
import { invoiceService } from '@/services/invoice-service';
import { ImageViewer } from './image-viewer';
import { PdfViewer } from './pdf-viewer';
import { FileText, AlertCircle } from 'lucide-react';

interface DocumentViewerProps {
  invoiceId: string;
}

export const DocumentViewer: React.FC<DocumentViewerProps> = ({ invoiceId }) => {
  const [fileUrl, setFileUrl] = useState<string | null>(null);
  const [fileType, setFileType] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let objectUrl: string | null = null;

    const fetchFile = async () => {
      try {
        setLoading(true);
        setError(null);
        const blob = await invoiceService.getInvoiceFile(invoiceId);
        
        // Determine file type
        const type = blob.type;
        setFileType(type);

        // Create object URL
        objectUrl = URL.createObjectURL(blob);
        setFileUrl(objectUrl);
      } catch (err) {
        console.error('Error fetching document:', err);
        setError('Belge yüklenirken bir hata oluştu');
      } finally {
        setLoading(false);
      }
    };

    if (invoiceId) {
      fetchFile();
    }

    return () => {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [invoiceId]);

  if (loading) {
    return (
      <div className="h-full w-full flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="flex flex-col items-center gap-2">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          <span className="text-sm text-gray-500">Belge yükleniyor...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-full w-full flex items-center justify-center bg-gray-50 dark:bg-gray-900 border border-red-200">
        <div className="flex flex-col items-center gap-2 text-destructive">
          <AlertCircle className="h-8 w-8" />
          <span className="text-sm font-medium">{error}</span>
          <button 
            onClick={() => window.location.reload()} 
            className="text-xs underline hover:text-red-700"
          >
            Tekrar Dene
          </button>
        </div>
      </div>
    );
  }

  if (!fileUrl) {
    return (
      <div className="h-full w-full flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="flex flex-col items-center gap-2 text-gray-400">
          <FileText className="h-12 w-12" />
          <span className="text-sm">Görüntülenecek belge yok</span>
        </div>
      </div>
    );
  }

  // Render based on file type
  if (fileType?.includes('pdf')) {
    return <PdfViewer url={fileUrl} />;
  } else if (fileType?.includes('image')) {
    return <ImageViewer url={fileUrl} alt="Fatura Görseli" />;
  } else if (fileType?.includes('xml')) {
     // For now, just show a basic XML viewer or fallback
     return (
        <div className="h-full w-full p-4 overflow-auto bg-gray-50 dark:bg-gray-900 font-mono text-xs">
            <pre className="whitespace-pre-wrap break-all">
                {/* We can't easily render URL content here without fetching it as text. 
                    For now, showing a placeholder for XML. 
                    Ideally we would read the blob as text and display it. */}
                XML Önizleme şu an için desteklenmemektedir. Lütfen indirerek görüntüleyin.
            </pre>
        </div>
     )
  }

  // Fallback for unknown types
  return (
    <div className="h-full w-full flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="flex flex-col items-center gap-2 text-gray-500">
        <AlertCircle className="h-8 w-8" />
        <span className="text-sm">Desteklenmeyen dosya formatı: {fileType}</span>
      </div>
    </div>
  );
};
