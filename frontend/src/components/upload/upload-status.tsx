"use client";

import { CheckCircle2, CircleDashed, AlertCircle, Loader2 } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export type UploadStatusType = "QUEUED" | "UPLOADING" | "PROCESSING" | "COMPLETED" | "FAILED";

interface UploadStatusProps {
  status: UploadStatusType;
  progress?: number;
  message?: string;
  className?: string;
}

export function UploadStatus({ status, progress = 0, message, className }: UploadStatusProps) {
  const getStatusIcon = () => {
    switch (status) {
      case "QUEUED":
        return <CircleDashed className="w-4 h-4 text-muted-foreground" />;
      case "UPLOADING":
        return <Loader2 className="w-4 h-4 text-blue-500 animate-spin" />;
      case "PROCESSING":
        return <Loader2 className="w-4 h-4 text-amber-500 animate-spin" />;
      case "COMPLETED":
        return <CheckCircle2 className="w-4 h-4 text-green-500" />;
      case "FAILED":
        return <AlertCircle className="w-4 h-4 text-red-500" />;
      default:
        return <CircleDashed className="w-4 h-4 text-muted-foreground" />;
    }
  };

  const getStatusText = () => {
    if (message) return message;
    switch (status) {
      case "QUEUED":
        return "Sırada";
      case "UPLOADING":
        return "Yükleniyor...";
      case "PROCESSING":
        return "İşleniyor...";
      case "COMPLETED":
        return "Tamamlandı";
      case "FAILED":
        return "Hata";
      default:
        return "";
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case "QUEUED":
        return "bg-secondary text-secondary-foreground hover:bg-secondary/80";
      case "UPLOADING":
        return "bg-blue-500/10 text-blue-600 hover:bg-blue-500/20 border-blue-200";
      case "PROCESSING":
        return "bg-amber-500/10 text-amber-600 hover:bg-amber-500/20 border-amber-200";
      case "COMPLETED":
        return "bg-green-500/10 text-green-600 hover:bg-green-500/20 border-green-200";
      case "FAILED":
        return "bg-red-500/10 text-red-600 hover:bg-red-500/20 border-red-200";
      default:
        return "bg-secondary text-secondary-foreground";
    }
  };

  return (
    <div className={cn("flex items-center gap-3 w-full", className)}>
      <div className="flex-1 space-y-1">
        <div className="flex items-center justify-between text-xs">
          <Badge variant="outline" className={cn("gap-1.5 font-normal", getStatusColor())}>
            {getStatusIcon()}
            <span>{getStatusText()}</span>
          </Badge>
          {status === "UPLOADING" && (
            <span className="text-muted-foreground">{Math.round(progress)}%</span>
          )}
        </div>
        
        {status === "UPLOADING" && (
          <Progress value={progress} className="h-1.5 w-full bg-blue-100 [&>div]:bg-blue-500" />
        )}
        
        {status === "PROCESSING" && (
          <Progress value={100} className="h-1.5 w-full bg-amber-100 animate-pulse [&>div]:bg-amber-500" />
        )}
      </div>
    </div>
  );
}
