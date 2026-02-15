"use client";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

interface DuplicateDialogProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  fileName: string;
  invoiceNumber?: string;
  date?: string;
}

export function DuplicateDialog({
  isOpen,
  onOpenChange,
  onConfirm,
  fileName,
  invoiceNumber,
  date,
}: DuplicateDialogProps) {
  return (
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Dosya Daha Önce Yüklenmiş</AlertDialogTitle>
          <AlertDialogDescription>
            <strong>{fileName}</strong> dosyası
            {date && ` ${date} tarihinde`}
            {invoiceNumber && ` ${invoiceNumber} fatura numarasıyla`} sisteme daha önce yüklenmiş.
            <br />
            <br />
            Yine de bu dosyayı tekrar yüklemek ve yeni bir analiz başlatmak istiyor musunuz?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={() => onOpenChange(false)}>İptal</AlertDialogCancel>
          <AlertDialogAction onClick={() => {
            onConfirm();
            onOpenChange(false);
          }}>Devam Et</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
