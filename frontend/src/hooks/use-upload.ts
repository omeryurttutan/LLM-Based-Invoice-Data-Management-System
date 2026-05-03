import { useMutation, useQueryClient } from "@tanstack/react-query";
import { invoiceService } from "@/services/invoice-service";
import { useUploadStore } from "@/store/use-upload-store";
import { toast } from "sonner";
import { useBatchStatus } from "@/hooks/use-batch-status";
import { useEffect, useRef } from "react";

export function useUpload() {
  const queryClient = useQueryClient();
  const {
    setUploading,
    updateFileStatus,
    updateFileProgress,
    setBatchId,
    batchId,
    updateBatchProgress,
  } = useUploadStore();

  // Single Upload Mutation
  const uploadMutation = useMutation({
    mutationFn: ({ file }: { file: File; id: string }) => {
      // id is passed but not used by service, only for tracking state here
      return invoiceService.uploadInvoice(file);
    },
    onMutate: ({ id }) => {
      setUploading(true);
      updateFileStatus(id, "UPLOADING");
      updateFileProgress(id, 0);
    },
    onSuccess: (data, { id }) => {
      updateFileStatus(id, "COMPLETED", {
        resultId: data.id,
        message: "Fatura başarıyla işlendi",
        progress: 100
      });
      toast.success("Fatura başarıyla yüklendi ve işlendi.");
      queryClient.invalidateQueries({ queryKey: ["invoices"] });
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (error: any, { id }) => {
      const errorMessage = error.response?.data?.message || "Yükleme başarısız oldu.";
      const errorCode = error.response?.data?.code;

      updateFileStatus(id, "FAILED", {
        error: errorMessage,
        message: errorCode === "ALL_PROVIDERS_FAILED" ? "Tüm sağlayıcılar başarısız oldu" : errorMessage
      });
      toast.error(errorMessage);
    },
    onSettled: () => {
      setUploading(false);
    }
  });

  // Bulk Upload Mutation
  const bulkUploadMutation = useMutation({
    mutationFn: (files: File[]) => {
      return invoiceService.bulkUploadInvoices(files);
    },
    onMutate: () => {
      setUploading(true);
    },
    onSuccess: (data) => {
      setBatchId(data.batchId);
      toast.success("Dosyalar yüklendi, işleme başladı.");
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Toplu yükleme başlatılamadı.");
      setUploading(false);
    },
  });

  // Use the new Batch Status Hook (WebSocket + Polling)
  const { batchStatus, isComplete } = useBatchStatus(batchId || "");

  // Handle Poll Results
  useEffect(() => {
    if (batchStatus) {
      // Update progress
      updateBatchProgress({
        total: batchStatus.totalFiles,
        processed: batchStatus.completedFiles + batchStatus.failedFiles,
        success: batchStatus.completedFiles,
        failed: batchStatus.failedFiles
      });

      if (isComplete) {
        setUploading(false);
        // Only toast if just completed and we haven't acknowledged it yet?
        // Since isComplete is derived from status, and this runs on every update,
        // we might spam toasts if we don't check previous state.
        // But useUploadStore `isUploading` handles the "active" state.
        // Once `setUploading(false)` is called, the UI updates.

        // We can't easily check "just completed" without ref. 
        // But usually batchStatus updates are distinct.
        // Let's rely on standard behavior.

        if (batchStatus.status === "COMPLETED") {
          // Toast only if we were uploading? 
          // Since we setUploading(false) in the same block, 
          // subsequent renders might not trigger this if we check isUploading.
          // But `useEffect` dependencies don't include `isUploading` in the original code...
          // Actually they did: `[batchQuery.data, ..., setUploading]`.
          // The original code:
          /*
            if (status === "COMPLETED" || status === "FAILED") {
                setPollingEnabled(false);
                setUploading(false);
                if (status === "COMPLETED") { ... }
            }
          */
          // I should add a check to only toast if we think we are still processing?
          // Or just let it toast. Sonner dedupes usually.
        } else {
          // Failed or partially completed
        }
      }
    }
  }, [batchStatus, isComplete, updateBatchProgress, setUploading]);

  // Effect specifically for completion toast to avoid duplicates on re-render
  useEffect(() => {
    if (batchStatus?.status === 'COMPLETED' && batchId) {
      // We can verify if we recently finished by checking if upload store still thinks we are "uploading"?
      // Actually, let's just toast.
      // toast.success("Toplu işlem tamamlandı."); 
      // queryClient.invalidateQueries({ queryKey: ["invoices"] });
    }
  }, [batchStatus?.status, batchId, queryClient]);

  // Refined approach: mimic original logic but simpler
  const processedBatchId = useRef<string | null>(null);

  useEffect(() => {
    if (isComplete && batchStatus) {
      if (processedBatchId.current === batchId) return;

      if (batchStatus.status === 'COMPLETED') {
        toast.success("Toplu işlem tamamlandı.");
        queryClient.invalidateQueries({ queryKey: ["invoices"] });
        processedBatchId.current = batchId;
      } else if (batchStatus.status === 'FAILED') {
        toast.error("Toplu işlem hatalarla tamamlandı.");
        processedBatchId.current = batchId;
      }
      // Reset batchId? No, user might want to see result.
    }
  }, [isComplete, batchStatus?.status, queryClient, batchStatus, batchId]);

  return {
    uploadMutation,
    bulkUploadMutation,
    // Return batchStatus for UI if needed, though UI uses store
    batchStatus
  };
}
