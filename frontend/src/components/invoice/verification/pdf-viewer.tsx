import React, { useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';

// Configure worker
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

interface PdfViewerProps {
  url: string;
}

export const PdfViewer: React.FC<PdfViewerProps> = ({ url }) => {
  const [numPages, setNumPages] = useState<number>(0);
  const [pageNumber, setPageNumber] = useState<number>(1);
  const [scale, setScale] = useState<number>(1.0);

  function onDocumentLoadSuccess({ numPages }: { numPages: number }) {
    setNumPages(numPages);
    setPageNumber(1);
  }

  const changePage = (offset: number) => {
    setPageNumber(prevPageNumber => {
      const newPageNumber = prevPageNumber + offset;
      return Math.min(Math.max(newPageNumber, 1), numPages);
    });
  };

  const changeScale = (delta: number) => {
    setScale(prevScale => Math.max(0.5, Math.min(3.0, prevScale + delta)));
  };

  return (
    <div className="h-full w-full flex flex-col bg-gray-100 dark:bg-gray-900 overflow-hidden relative">
      {/* Toolbar */}
      <div className="absolute top-4 left-1/2 -translate-x-1/2 z-10 flex gap-2 bg-white/90 dark:bg-black/90 p-1.5 rounded-full shadow-lg border border-gray-200 dark:border-gray-800 items-center">
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-full"
          disabled={pageNumber <= 1}
          onClick={() => changePage(-1)}
          title="Önceki Sayfa"
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <span className="text-xs font-medium px-2 min-w-[3rem] text-center">
          {pageNumber} / {numPages || '--'}
        </span>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-full"
          disabled={pageNumber >= numPages}
          onClick={() => changePage(1)}
          title="Sonraki Sayfa"
        >
          <ChevronRight className="h-4 w-4" />
        </Button>

        <div className="w-px h-4 bg-gray-200 dark:bg-gray-700 mx-1" />

        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-full"
          onClick={() => changeScale(0.1)}
          title="Yakınlaştır"
        >
          <ZoomIn className="h-4 w-4" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-full"
          onClick={() => changeScale(-0.1)}
          title="Uzaklaştır"
        >
          <ZoomOut className="h-4 w-4" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-full"
          onClick={() => setScale(1.0)}
          title="Sıfırla"
        >
          <RotateCcw className="h-4 w-4" />
        </Button>
      </div>

      {/* Document Container */}
      <div className="flex-1 w-full h-full overflow-auto flex justify-center p-4">
        <Document
          file={url}
          onLoadSuccess={onDocumentLoadSuccess}
          loading={
            <div className="flex items-center justify-center h-full">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          }
          error={
            <div className="flex items-center justify-center h-full text-destructive">
              PDF yüklenemedi. Lütfen tekrar deneyin.
            </div>
          }
          className="shadow-lg"
        >
          <Page 
            pageNumber={pageNumber} 
            scale={scale} 
            renderTextLayer={false}
            renderAnnotationLayer={false}
            className="shadow-lg"
          />
        </Document>
      </div>
    </div>
  );
};
