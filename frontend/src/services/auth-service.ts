import apiClient from '@/lib/api-client';
import { 
  LoginRequest, 
  RegisterRequest, 
  AuthResponse, 
  RefreshResponse,
  User 
} from '@/types/auth';

export const authService = {
  /**
   * Login with email and password
   */
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', data);
    return response.data;
  },
  
  /**
   * Register a new user
   */
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/register', data);
    return response.data;
  },
  
  /**
   * Refresh access token
   */
  async refresh(refreshToken: string): Promise<RefreshResponse> {
    const response = await apiClient.post<RefreshResponse>('/auth/refresh', {
      refreshToken,
    });
    return response.data;
  },
  
  /**
   * Logout and invalidate refresh token
   */
  async logout(refreshToken: string): Promise<void> {
    await apiClient.post('/auth/logout', { refreshToken });
  },
  
  /**
   * Get current user profile
   */
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/auth/me');
    return response.data;
  },
};

export default authService;
