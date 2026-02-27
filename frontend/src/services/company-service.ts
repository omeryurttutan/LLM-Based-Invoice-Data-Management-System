import apiClient from './api-client';
import { API_ENDPOINTS } from './endpoints';

export interface CompanyResponse {
  id: string;
  name: string;
  taxNumber: string;
  taxOffice: string;
  address: string;
  city: string;
  district: string;
  postalCode: string;
  phone: string;
  email: string;
  website: string;
  defaultCurrency: string;
  invoicePrefix: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export const companyService = {
  getMyCompany: async (): Promise<CompanyResponse | null> => {
    try {
      const response = await apiClient.get(API_ENDPOINTS.MY_COMPANY);
      return response.data?.data || response.data;
    } catch {
      return null;
    }
  },
  updateCompany: async (id: string, data: Partial<CompanyResponse>): Promise<CompanyResponse> => {
    const response = await apiClient.put(`${API_ENDPOINTS.COMPANIES}/${id}`, data);
    return response.data?.data || response.data;
  },
};
