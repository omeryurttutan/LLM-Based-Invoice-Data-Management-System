import apiClient from './api-client';
import { API_ENDPOINTS } from './endpoints';
import { UserResponse, CreateUserRequest, UpdateUserRequest, ChangeRoleRequest } from '@/types/user';

export const userService = {
  // Get all users (paginated but simplified for frontend for now)
  getUsers: async (page = 0, size = 20): Promise<{ content: UserResponse[], totalElements: number }> => {
    const response = await apiClient.get(API_ENDPOINTS.USERS, { params: { page, size } });
    // Backend returns ApiResponse<Page<UserResponse>>: { success, data: { content, totalElements, ... } }
    const pageData = response.data?.data || response.data || {};
    return {
      content: pageData.content || [],
      totalElements: pageData.totalElements || 0,
    };
  },

  getUserById: async (id: string): Promise<UserResponse> => {
    const response = await apiClient.get(API_ENDPOINTS.USER_DETAIL(id));
    return response.data?.data;
  },

  createUser: async (data: CreateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.post(API_ENDPOINTS.USERS, data);
    return response.data?.data;
  },

  updateUser: async (id: string, data: UpdateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.put(API_ENDPOINTS.USER_DETAIL(id), data);
    return response.data?.data;
  },

  deleteUser: async (id: string): Promise<void> => {
    await apiClient.delete(API_ENDPOINTS.USER_DETAIL(id));
  },

  toggleUserActive: async (id: string): Promise<UserResponse> => {
    const response = await apiClient.patch(API_ENDPOINTS.USER_ACTIVATE(id));
    return response.data?.data;
  },

  changeUserRole: async (id: string, data: ChangeRoleRequest): Promise<UserResponse> => {
    const response = await apiClient.patch(API_ENDPOINTS.USER_ROLE(id), data);
    return response.data?.data;
  }
};
