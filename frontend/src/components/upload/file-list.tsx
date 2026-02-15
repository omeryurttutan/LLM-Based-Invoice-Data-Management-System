import { FileIcon, Trash2, RefreshCw, Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { UploadStatus, UploadStatusType } from "./upload-status";
import Link from "next/link";

export interface FileItem {
  id: string;
  file: File;
  status: UploadStatusType;
  progress: number;
  message?: string;
  resultId?: string; // Invoice ID if successful
  error?: string;
}

interface FileListProps {
  files: FileItem[];
  onRemove: (id: string) => void;
  onRetry: (id: string) => void;
  isUploading: boolean;
}

export function FileList({ files, onRemove, onRetry, isUploading }: FileListProps) {
  if (files.length === 0) return null;

  return (
    <div className="w-full space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Yükleme Listesi</h3>
        <span className="text-sm text-muted-foreground">{files.length} dosya</span>
      </div>

      <ScrollArea className="h-[300px] w-full rounded-md border p-4">
        <div className="space-y-4">
          {files.map((item) => (
            <div
              key={item.id}
              className="flex items-start gap-4 p-3 rounded-lg border bg-card hover:bg-accent/5 transition-colors"
            >
              <div className="p-2 rounded bg-muted">
                <FileIcon className="w-8 h-8 text-muted-foreground" />
              </div>

              <div className="flex-1 space-y-1 min-w-0">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-sm font-medium truncate" title={item.file.name}>
                    {item.file.name}
                  </p>
                  <span className="text-xs text-muted-foreground whitespace-nowrap">
                    {(item.file.size / 1024 / 1024).toFixed(2)} MB
                  </span>
                </div>

                <UploadStatus 
                  status={item.status} 
                  progress={item.progress} 
                  message={item.message || item.error} 
                />
              </div>

              <div className="flex flex-col gap-1">
                {item.status === "COMPLETED" && item.resultId && (
                  <Button variant="ghost" size="icon" asChild title="Görüntüle">
                    <Link href={`/invoices/${item.resultId}`}>
                      <Eye className="w-4 h-4 text-blue-500" />
                    </Link>
                  </Button>
                )}

                {item.status === "FAILED" && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onRetry(item.id)}
                    title="Tekrar Dene"
                  >
                    <RefreshCw className="w-4 h-4 text-orange-500" />
                  </Button>
                )}

                {!isUploading && item.status !== "UPLOADING" && item.status !== "PROCESSING" && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onRemove(item.id)}
                    title="Kaldır"
                  >
                    <Trash2 className="w-4 h-4 text-red-500" />
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      </ScrollArea>
    </div>
  );
}
