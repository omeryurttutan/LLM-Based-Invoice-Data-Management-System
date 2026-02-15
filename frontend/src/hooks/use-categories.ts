'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { categoryService } from '@/services/category-service';
import { CreateCategoryRequest } from '@/types/category';
import { toast } from 'sonner';

export const categoryKeys = {
  all: ['categories'] as const,
  list: () => [...categoryKeys.all, 'list'] as const,
};

export function useCategories() {
  return useQuery({
    queryKey: categoryKeys.list(),
    queryFn: () => categoryService.getCategories(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCategoryRequest) => categoryService.createCategory(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.list() });
      toast.success('Kategori oluşturuldu');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Kategori oluşturulamadı'),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateCategoryRequest }) =>
      categoryService.updateCategory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.list() });
      toast.success('Kategori güncellendi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Kategori güncellenemedi'),
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => categoryService.deleteCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.list() });
      toast.success('Kategori silindi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Kategori silinemedi'),
  });
}
