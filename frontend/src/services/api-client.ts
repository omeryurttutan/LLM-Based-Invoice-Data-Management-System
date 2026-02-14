import axios from 'axios';
import { useAuthStore } from '@/stores/auth-store';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add auth token
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor - handle 401, refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // Phase 11: Implement token refresh logic here
    // If 401 and not retry:
    // try refresh -> retry original request
    // else logout
    if (error.response?.status === 401) {
        // useAuthStore.getState().logout(); // Optional auto-logout
    }
    return Promise.reject(error);
  }
);

export default apiClient;
