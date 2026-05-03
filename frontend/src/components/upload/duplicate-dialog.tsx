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
import { useTranslations } from "next-intl";

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
  const t = useTranslations('invoices');

  return (
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t('upload.duplicate.title')}</AlertDialogTitle>
          <AlertDialogDescription>
            {t.rich('upload.duplicate.description', {
              fileName: fileName,
              details: (date || invoiceNumber) ? (
                (date ? t('upload.duplicate.detailsDate', { date }) : '') +
                (invoiceNumber ? t('upload.duplicate.detailsInvoice', { invoiceNumber }) : '')
              ) : '',
              strong: (chunks) => <strong>{chunks}</strong>
            })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={() => onOpenChange(false)}>{t('upload.duplicate.cancel')}</AlertDialogCancel>
          <AlertDialogAction onClick={() => {
            onConfirm();
            onOpenChange(false);
          }}>{t('upload.duplicate.confirm')}</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
