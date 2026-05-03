"use client";

import { useState, useEffect } from "react";
import { UploadZone } from "@/components/upload/upload-zone";
import { FileList } from "@/components/upload/file-list";
import { DuplicateDialog } from "@/components/upload/duplicate-dialog";
import { useUploadStore } from "@/store/use-upload-store";
import { useUpload } from "@/hooks/use-upload";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, Loader2, BarChart3, AlertTriangle } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { apiClient } from "@/lib/api-client";
import type { QuotaInfo } from "@/types/auth";

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
  const t = useTranslations('invoices');
  const [quotaInfo, setQuotaInfo] = useState<QuotaInfo | null>(null);

  useEffect(() => {
    apiClient.get('/api/v1/quota')
      .then(res => setQuotaInfo(res.data?.data))
      .catch(() => {/* quota info is optional display */});
  }, []);
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
          <h1 className="text-3xl font-bold tracking-tight">{t('upload.title')}</h1>
          <p className="text-muted-foreground">
            {t('upload.subtitle')}
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
                {t('upload.clear')}
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
                    {t('upload.uploading')}
                  </>
                ) : (
                  files.some(f => f.status === 'COMPLETED') ? t('upload.uploadNew') : t('upload.startUpload')
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
          {/* Quota Info Card */}
          {quotaInfo && (
            <Card className={quotaInfo.subscriptionStatus === 'TRIAL' ? 'border-amber-500/50 bg-amber-500/5' : ''}>
              <CardHeader className="pb-2">
                <CardTitle className="text-lg flex items-center gap-2">
                  <BarChart3 className="w-5 h-5" />
                  Kota Bilgisi
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {quotaInfo.subscriptionStatus === 'TRIAL' && (
                  <div className="flex items-center gap-2 text-amber-600 text-xs font-medium">
                    <AlertTriangle className="w-3.5 h-3.5" />
                    Deneme Sürümü
                    {quotaInfo.trialEndsAt && (
                      <span className="ml-auto text-muted-foreground">
                        {new Date(quotaInfo.trialEndsAt).toLocaleDateString('tr-TR')} tarihine kadar
                      </span>
                    )}
                  </div>
                )}

                {/* Daily Quota */}
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground">Günlük Kullanım</span>
                    <span className="font-medium">{quotaInfo.dailyUsedInvoices} / {quotaInfo.dailyMaxInvoices}</span>
                  </div>
                  <div className="w-full bg-secondary h-2 rounded-full overflow-hidden">
                    <div
                      className={`h-full transition-all duration-500 rounded-full ${
                        quotaInfo.dailyRemainingInvoices <= 5 ? 'bg-red-500' : 'bg-primary'
                      }`}
                      style={{ width: `${Math.min(100, (quotaInfo.dailyUsedInvoices / quotaInfo.dailyMaxInvoices) * 100)}%` }}
                    />
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">{quotaInfo.dailyRemainingInvoices} fatura hakkı kaldı</p>
                </div>

                {/* Total Quota */}
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground">Toplam Kullanım</span>
                    <span className="font-medium">{quotaInfo.usedInvoices} / {quotaInfo.maxInvoices}</span>
                  </div>
                  <div className="w-full bg-secondary h-2 rounded-full overflow-hidden">
                    <div
                      className={`h-full transition-all duration-500 rounded-full ${
                        quotaInfo.remainingInvoices <= 20 ? 'bg-red-500' : 'bg-emerald-500'
                      }`}
                      style={{ width: `${Math.min(100, (quotaInfo.usedInvoices / quotaInfo.maxInvoices) * 100)}%` }}
                    />
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">{quotaInfo.remainingInvoices} toplam fatura hakkı</p>
                </div>

                <Separator />
                <div className="flex justify-between text-xs">
                  <span className="text-muted-foreground">Kullanıcı</span>
                  <span className="font-medium">{quotaInfo.usedUsers} / {quotaInfo.maxUsers}</span>
                </div>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{t('upload.info.title')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-sm text-muted-foreground">
              <p>
                <strong>{t.rich('upload.info.single', { strong: (chunks) => <strong>{chunks}</strong> })}</strong>
              </p>
              <p>
                <strong>{t.rich('upload.info.bulk', { strong: (chunks) => <strong>{chunks}</strong> })}</strong>
              </p>
              <Separator />
              <div className="space-y-1">
                <p>{t('upload.info.formats')}</p>
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
                <CardTitle className="text-sm font-medium">{t('upload.batch.title')}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <div className="flex justify-between text-xs">
                    <span>{t('upload.batch.processed')}</span>
                    <span>{batchProgress.processed} / {batchProgress.total}</span>
                  </div>
                  <div className="w-full bg-secondary h-2 rounded-full overflow-hidden">
                    <div
                      className="bg-primary h-full transition-all duration-500"
                      style={{ width: `${(batchProgress.processed / batchProgress.total) * 100}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-xs text-muted-foreground pt-1">
                    <span className="text-green-600">{batchProgress.success} {t('upload.batch.success')}</span>
                    <span className="text-red-600">{batchProgress.failed} {t('upload.batch.failed')}</span>
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
