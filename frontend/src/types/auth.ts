export type Role = 'ADMIN' | 'MANAGER' | 'ACCOUNTANT' | 'INTERN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  companyId: string;
  companyName: string;
  avatarUrl?: string;
  isActive: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}
