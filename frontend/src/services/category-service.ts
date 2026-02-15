import apiClient from '@/lib/api-client';
import { Category, CreateCategoryRequest } from '@/types/category';

export const categoryService = {
  async getCategories(): Promise<Category[]> {
    return (await apiClient.get('/categories')).data;
  },
  async createCategory(data: CreateCategoryRequest): Promise<Category> {
    return (await apiClient.post('/categories', data)).data;
  },
  async updateCategory(id: string, data: CreateCategoryRequest): Promise<Category> {
    return (await apiClient.put(`/categories/${id}`, data)).data;
  },
  async deleteCategory(id: string): Promise<void> {
    await apiClient.delete(`/categories/${id}`);
  },
};
