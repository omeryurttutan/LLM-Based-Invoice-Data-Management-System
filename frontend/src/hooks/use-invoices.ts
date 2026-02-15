'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { invoiceService } from '@/services/invoice-service';
import { CreateInvoiceRequest, UpdateInvoiceRequest, VerifyInvoiceRequest, 
         RejectInvoiceRequest, InvoiceListParams } from '@/types/invoice';
import { toast } from 'sonner';

export const invoiceKeys = {
  all: ['invoices'] as const,
  lists: () => [...invoiceKeys.all, 'list'] as const,
  list: (params: InvoiceListParams) => [...invoiceKeys.lists(), params] as const,
  details: () => [...invoiceKeys.all, 'detail'] as const,
  detail: (id: string) => [...invoiceKeys.details(), id] as const,
};

export function useInvoices(params: InvoiceListParams = {}) {
  return useQuery({
    queryKey: invoiceKeys.list(params),
    queryFn: () => invoiceService.getInvoices(params),
    staleTime: 30 * 1000,
  });
}

export function useInvoice(id: string) {
  return useQuery({
    queryKey: invoiceKeys.detail(id),
    queryFn: () => invoiceService.getInvoice(id),
    enabled: !!id,
  });
}

export function useCreateInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateInvoiceRequest) => invoiceService.createInvoice(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      toast.success('Fatura başarıyla oluşturuldu');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura oluşturulamadı'),
  });
}

export function useUpdateInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateInvoiceRequest }) =>
      invoiceService.updateInvoice(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(variables.id) });
      toast.success('Fatura başarıyla güncellendi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura güncellenemedi'),
  });
}

export function useDeleteInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => invoiceService.deleteInvoice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      toast.success('Fatura başarıyla silindi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura silinemedi'),
  });
}

export function useVerifyInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data?: VerifyInvoiceRequest }) =>
      invoiceService.verifyInvoice(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(variables.id) });
      toast.success('Fatura onaylandı');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura onaylanamadı'),
  });
}

export function useRejectInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectInvoiceRequest }) =>
      invoiceService.rejectInvoice(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(variables.id) });
      toast.success('Fatura reddedildi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura reddedilemedi'),
  });
}

export function useReopenInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => invoiceService.reopenInvoice(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(id) });
      toast.success('Fatura yeniden açıldı');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura yeniden açılamadı'),
  });
}
