import apiClient from './api-client';
import { API_ENDPOINTS } from './endpoints';
import { UserProfileResponse, UpdateProfileRequest, ChangePasswordRequest } from '@/types/profile';

export const profileService = {
  getProfile: async (): Promise<UserProfileResponse> => {
    const response = await apiClient.get(API_ENDPOINTS.PROFILE);
    // Backend wraps in ApiResponse { success, data, message }
    return response.data?.data || response.data;
  },

  updateProfile: async (data: UpdateProfileRequest): Promise<UserProfileResponse> => {
    const response = await apiClient.put(API_ENDPOINTS.PROFILE, data);
    return response.data?.data || response.data;
  },

  changePassword: async (data: ChangePasswordRequest): Promise<void> => {
    const response = await apiClient.patch(API_ENDPOINTS.PROFILE_PASSWORD, data);
    return response.data;
  }
};

