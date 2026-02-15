"use client";

import { useState } from "react";
import { UploadZone } from "@/components/upload/upload-zone";
import { FileList } from "@/components/upload/file-list";
import { DuplicateDialog } from "@/components/upload/duplicate-dialog";
import { useUploadStore } from "@/store/use-upload-store";
import { useUpload } from "@/hooks/use-upload";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, Loader2 } from "lucide-react";
import Link from "next/link";

export default function UploadPage() {
  const { 
    files, 
    addFiles, 
    removeFile, 
    reset, 
    isUploading, 
    uploadMode, 
    batchProgress,
    retryFile
  } = useUploadStore();
  
  const { uploadMutation, bulkUploadMutation } = useUpload();
  const [duplicateDialogOpen, setDuplicateDialogOpen] = useState(false);
  // const [duplicateFile, setDuplicateFile] = useState<{name: string, id: string} | null>(null); // Unused for now

  const handleFilesSelected = (newFiles: File[]) => {
    addFiles(newFiles);
  };

  const handleRemoveFile = (id: string) => {
    removeFile(id);
  };

  const handleRetryFile = (id: string) => {
    retryFile(id);
    const fileItem = files.find(f => f.id === id);
    if (fileItem) {
        if (uploadMode === 'single') {
            uploadMutation.mutate({ file: fileItem.file, id });
        } else {
            // For bulk, retry might be complex if batch is closed, maybe just treat as single upload retry
            // Or add to a new batch. For now simplicity: treat as single
            uploadMutation.mutate({ file: fileItem.file, id });
        }
    }
  };

  const handleUpload = async () => {
    if (files.length === 0) return;

    if (uploadMode === "single") {
      const fileItem = files[0];
      uploadMutation.mutate({ file: fileItem.file, id: fileItem.id });
    } else {
      // Bulk upload
      const validFiles = files.map(f => f.file);
      bulkUploadMutation.mutate(validFiles);
    }
  };

  return (
    <div className="container mx-auto py-8 max-w-4xl space-y-8 animate-in fade-in duration-500">
      
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link href="/invoices">
            <ArrowLeft className="w-5 h-5" />
          </Link>
        </Button>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Fatura Yükle</h1>
          <p className="text-muted-foreground">
            Sisteme yeni fatura eklemek için dosyalarınızı yükleyin.
          </p>
        </div>
      </div>

      <Separator />

      <div className="grid gap-8 md:grid-cols-[2fr_1fr]">
        
        {/* Left Column: Upload Zone & Queue */}
        <div className="space-y-6">
          <Card>
            <CardContent className="p-6">
              <UploadZone 
                onFilesSelected={handleFilesSelected}
                isUploading={isUploading || uploadMutation.isPending || bulkUploadMutation.isPending}
              />
            </CardContent>
          </Card>
          
          <div className="flex justify-end gap-3"> 
             {files.length > 0 && !isUploading && (
                <Button variant="outline" onClick={reset}>
                    Temizle
                </Button>
             )}
             
             {files.length > 0 && (
                <Button 
                    onClick={handleUpload} 
                    disabled={isUploading || files.every(f => f.status === 'COMPLETED')}
                    className="min-w-[150px]"
                >
                    {isUploading ? (
                        <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            Yükleniyor...
                        </>
                    ) : (
                        files.some(f => f.status === 'COMPLETED') ? "Yeni Yükleme Yap" : "Yüklemeyi Başlat"
                    )}
                </Button>
             )}
          </div>

          <FileList 
            files={files}
            onRemove={handleRemoveFile}
            onRetry={handleRetryFile}
            isUploading={isUploading}
          />
        </div>

        {/* Right Column: Info / Status */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Bilgilendirme</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-sm text-muted-foreground">
              <p>
                <strong>Tekli Yükleme:</strong> Sonuçlar hemen görüntülenir (30-90 sn sürebilir).
              </p>
              <p>
                <strong>Toplu Yükleme:</strong> Dosyalar arka planda işlenir. İlerleme durumunu buradan takip edebilirsiniz.
              </p>
              <Separator />
              <div className="space-y-1">
                <p>Desteklenen formatlar:</p>
                <div className="flex flex-wrap gap-2">
                    <span className="bg-muted px-2 py-1 rounded text-xs text-foreground">JPG/PNG</span>
                    <span className="bg-muted px-2 py-1 rounded text-xs text-foreground">PDF</span>
                    <span className="bg-muted px-2 py-1 rounded text-xs text-foreground">XML</span>
                    <span className="bg-muted px-2 py-1 rounded text-xs text-foreground">ZIP</span>
                </div>
              </div>
            </CardContent>
          </Card>
          
          {batchProgress && (
             <Card className="bg-muted/30">
                <CardHeader className="pb-2">
                    <CardTitle className="text-sm font-medium">Toplu İşlem Durumu</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-2">
                        <div className="flex justify-between text-xs">
                            <span>İşlenen</span>
                            <span>{batchProgress.processed} / {batchProgress.total}</span>
                        </div>
                        <div className="w-full bg-secondary h-2 rounded-full overflow-hidden">
                            <div 
                                className="bg-primary h-full transition-all duration-500"
                                style={{ width: `${(batchProgress.processed / batchProgress.total) * 100}%` }}
                            />
                        </div>
                        <div className="flex justify-between text-xs text-muted-foreground pt-1">
                            <span className="text-green-600">{batchProgress.success} Başarılı</span>
                            <span className="text-red-600">{batchProgress.failed} Hatalı</span>
                        </div>
                    </div>
                </CardContent>
             </Card>
          )}
        </div>
      </div>

      <DuplicateDialog 
         isOpen={duplicateDialogOpen}
         onOpenChange={setDuplicateDialogOpen}
         onConfirm={() => {
             // Handle duplicate logic if 409
         }}
         fileName="" // duplicateFile ? duplicateFile.name : ""
      />
    </div>
  );
}
