import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { invoiceService } from "@/services/invoice-service";
import { useUploadStore } from "@/store/use-upload-store";
import { toast } from "sonner";
import { useState, useEffect } from "react";
// import { BatchFileStatus } from "@/types/invoice"; // Unused currently

export function useUpload() {
  const queryClient = useQueryClient();
  const { 
    setUploading, 
    updateFileStatus, 
    updateFileProgress, 
    setBatchId, 
    batchId,
    updateBatchProgress,
    // uploadMode // Unused
  } = useUploadStore();

  const [pollingEnabled, setPollingEnabled] = useState(false);

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
      setPollingEnabled(true);
      toast.success("Dosyalar yüklendi, işleme başladı.");
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Toplu yükleme başlatılamadı.");
      setUploading(false);
    },
  });

  // Batch Status Polling
  const batchQuery = useQuery({
    queryKey: ["batch-status", batchId],
    queryFn: () => invoiceService.getBatchStatus(batchId!),
    enabled: !!batchId && pollingEnabled,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.status === "COMPLETED" || data?.status === "FAILED") {
        return false;
      }
      return 5000; // Poll every 5 seconds
    },
  });

  // Handle Poll Results
  useEffect(() => {
    if (batchQuery.data) {
      const { status, totalFiles, completedFiles, failedFiles } = batchQuery.data;

      // Update progress
      updateBatchProgress({
        total: totalFiles,
        processed: completedFiles + failedFiles,
        success: completedFiles,
        failed: failedFiles
      });

      // Update individual files
      // files.forEach((fileStatus: BatchFileStatus) => {
         // Placeholder for matching file IDs if we had them or matching by name
      // });

      if (status === "COMPLETED" || status === "FAILED") {
        setPollingEnabled(false);
        setUploading(false);
        if (status === "COMPLETED") {
          toast.success("Toplu işlem tamamlandı.");
          queryClient.invalidateQueries({ queryKey: ["invoices"] });
        } else {
          toast.error("Toplu işlem hatalarla tamamlandı.");
        }
      }
    }
  }, [batchQuery.data, updateBatchProgress, setUploading, queryClient]);

  return {
    uploadMutation,
    bulkUploadMutation,
    batchQuery
  };
}
