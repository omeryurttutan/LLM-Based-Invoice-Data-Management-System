import { useState, useRef } from "react";
import { Upload } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

interface UploadZoneProps {
  onFilesSelected: (files: File[]) => void;
  isUploading: boolean;
  maxFiles?: number;
  maxSizeMB?: number; // per file
  maxTotalSizeMB?: number; // total batch
  acceptedExtensions?: string[];
}

export function UploadZone({
  onFilesSelected,
  isUploading,
  maxFiles = 50,
  maxSizeMB = 20,
  maxTotalSizeMB = 200,
  acceptedExtensions = [".jpg", ".jpeg", ".png", ".pdf", ".xml", ".zip"],
}: UploadZoneProps) {
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const t = useTranslations('invoices');

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    if (!isUploading) {
      setIsDragOver(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);

    if (isUploading) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const files = Array.from(e.dataTransfer.files);
      validateAndSelect(files);
    }
  };

  const handleClick = () => {
    if (!isUploading) {
      fileInputRef.current?.click();
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const files = Array.from(e.target.files);
      validateAndSelect(files);
      // Reset input so same files can be selected again if needed
      e.target.value = "";
    }
  };

  const validateAndSelect = (files: File[]) => {
    // 1. File Count Check
    if (files.length > maxFiles) {
      toast.error(t('upload.zone.maxFiles', { maxFiles }));
      return;
    }

    // 2. Total Size Check
    const totalSize = files.reduce((acc, file) => acc + file.size, 0);
    if (totalSize > maxTotalSizeMB * 1024 * 1024) {
      toast.error(t('upload.zone.maxTotalSize', { maxTotalSize: maxTotalSizeMB }));
      return;
    }

    const validFiles: File[] = [];

    files.forEach((file) => {
      // 3. Extension Check
      const extension = "." + file.name.split(".").pop()?.toLowerCase();
      if (!acceptedExtensions.includes(extension)) {
        toast.error(t('upload.zone.invalidExtension', { fileName: file.name }));
        return;
      }

      // 4. Per File Size Check
      // Special logic for ZIP and XML if needed, or general logic
      let limit = maxSizeMB;
      if (extension === ".zip") limit = 100;
      else if (extension === ".xml") limit = 50;

      if (file.size > limit * 1024 * 1024) {
        toast.error(t('upload.zone.sizeLimit', { fileName: file.name, limit }));
        return;
      }

      // 5. Empty Check
      if (file.size === 0) {
        toast.error(t('upload.zone.emptyFile', { fileName: file.name }));
        return;
      }

      validFiles.push(file);
    });

    if (validFiles.length > 0) {
      onFilesSelected(validFiles);
    }
  };

  return (
    <div
      onClick={handleClick}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      className={cn(
        "relative flex flex-col items-center justify-center w-full min-h-[300px] border-2 border-dashed rounded-xl transition-all duration-200 cursor-pointer bg-muted/20",
        isDragOver
          ? "border-primary bg-primary/5 scale-[1.01]"
          : "border-muted-foreground/25 hover:border-primary/50 hover:bg-muted/40",
        isUploading && "cursor-not-allowed opacity-50 pointer-events-none"
      )}
    >
      <input
        type="file"
        ref={fileInputRef}
        className="hidden"
        multiple
        accept={acceptedExtensions.join(",")}
        onChange={handleFileInput}
        disabled={isUploading}
      />

      <div className="flex flex-col items-center gap-4 p-8 text-center animate-in fade-in zoom-in-95 duration-300">
        <div className={cn(
          "p-4 rounded-full bg-background shadow-sm ring-1 ring-muted transition-transform duration-300",
          isDragOver ? "scale-110 ring-primary" : ""
        )}>
          {isDragOver ? (
            <Upload className="w-8 h-8 text-primary animate-bounce" />
          ) : (
            <Upload className="w-8 h-8 text-muted-foreground" />
          )}
        </div>

        <div className="space-y-2">
          <h3 className="text-lg font-semibold tracking-tight">
            {t('upload.zone.dropTitle')}
          </h3>
          <p className="text-sm text-muted-foreground">
            {t('upload.zone.dropSubtitle')}
          </p>
        </div>

        <div className="flex items-center gap-2 text-xs text-muted-foreground bg-background/50 px-3 py-1 rounded-full border">
          <span className="font-medium">{t('upload.zone.supported')}</span>
          {acceptedExtensions.map(ext => ext.replace(".", "").toUpperCase()).join(", ")}
        </div>
      </div>
    </div>
  );
}
