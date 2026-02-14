

// User role enum matching backend
export type UserRole = 'ADMIN' | 'MANAGER' | 'ACCOUNTANT' | 'INTERN';

// User entity
export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  companyId: string;
  companyName: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

// Auth tokens
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

// Login request
export interface LoginRequest {
  email: string;
  password: string;
}

// Register request
export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  companyId?: string;  // Optional for first user
}

// Auth response (login/register)
export interface AuthResponse extends AuthTokens {
  user: User;
}

// Refresh response
export interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

// API Error response
export interface ApiError {
  error: string;
  message: string;
  details?: Record<string, string>;
}

// Form validation schemas (for react-hook-form + zod)
export interface LoginFormData {
  email: string;
  password: string;
}

export interface RegisterFormData {
  email: string;
  password: string;
  confirmPassword: string;
  fullName: string;
}
