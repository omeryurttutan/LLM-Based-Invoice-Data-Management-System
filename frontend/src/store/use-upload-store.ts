import { create } from "zustand";

export type UploadStatus = "QUEUED" | "UPLOADING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface FileItem {
  id: string;
  file: File;
  status: UploadStatus;
  progress: number;
  message?: string;
  resultId?: string; // Invoice ID
  error?: string;
  uploadId?: string; // Backend ID if needed
}

interface UploadState {
  files: FileItem[];
  isUploading: boolean;
  uploadMode: "single" | "bulk" | null;
  batchId: string | null;
  batchProgress: {
    total: number;
    processed: number;
    success: number;
    failed: number;
  } | null;

  // Actions
  addFiles: (newFiles: File[]) => void;
  removeFile: (id: string) => void;
  updateFileStatus: (id: string, status: UploadStatus, updates?: Partial<FileItem>) => void;
  updateFileProgress: (id: string, progress: number) => void;
  setUploading: (isUploading: boolean) => void;
  setUploadMode: (mode: "single" | "bulk" | null) => void;
  setBatchId: (id: string | null) => void;
  updateBatchProgress: (progress: UploadState["batchProgress"]) => void;
  reset: () => void;
  retryFile: (id: string) => void;
}

export const useUploadStore = create<UploadState>((set) => ({
  files: [],
  isUploading: false,
  uploadMode: null,
  batchId: null,
  batchProgress: null,

  addFiles: (newFiles) => {
    const newItems: FileItem[] = newFiles.map((file) => ({
      id: Math.random().toString(36).substring(2, 9),
      file,
      status: "QUEUED",
      progress: 0,
    }));

    set((state) => {
      const updatedFiles = [...state.files, ...newItems];
      // Auto-detect mode
      const mode = updatedFiles.length > 1 || updatedFiles.some(f => f.file.name.endsWith('.zip')) 
        ? "bulk" 
        : "single";
      
      return {
        files: updatedFiles,
        uploadMode: mode,
      };
    });
  },

  removeFile: (id) => {
    set((state) => {
      const updatedFiles = state.files.filter((f) => f.id !== id);
      const mode = updatedFiles.length === 0 
        ? null 
        : (updatedFiles.length > 1 || updatedFiles.some(f => f.file.name.endsWith('.zip')) ? "bulk" : "single");
      
      return {
        files: updatedFiles,
        uploadMode: mode,
      };
    });
  },

  updateFileStatus: (id, status, updates) => {
    set((state) => ({
      files: state.files.map((f) =>
        f.id === id ? { ...f, status, ...updates } : f
      ),
    }));
  },

  updateFileProgress: (id, progress) => {
    set((state) => ({
      files: state.files.map((f) =>
        f.id === id ? { ...f, progress } : f
      ),
    }));
  },

  setUploading: (isUploading) => set({ isUploading }),
  
  setUploadMode: (uploadMode) => set({ uploadMode }),
  
  setBatchId: (batchId) => set({ batchId }),
  
  updateBatchProgress: (batchProgress) => set({ batchProgress }),

  reset: () =>
    set({
      files: [],
      isUploading: false,
      uploadMode: null,
      batchId: null,
      batchProgress: null,
    }),

  retryFile: (id) => {
    set((state) => ({
      files: state.files.map((f) =>
        f.id === id ? { ...f, status: "QUEUED", progress: 0, error: undefined, message: undefined } : f
      ),
    }));
  },
}));
