# PHASE 11: FRONTEND — AUTHENTICATION PAGES

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Backend**: Java 17 + Spring Boot 3.2 (Hexagonal Architecture) — **running on port 8080**
- **Frontend**: Next.js 14+ (App Router, TypeScript, Tailwind CSS, Shadcn/ui) — **running on port 3000**
- **Database**: PostgreSQL 15+

### Current State
**Phases 0-10 have been completed:**
- ✅ Phase 0: Docker Compose environment — Full project skeleton (backend, frontend, Python service)
- ✅ Phase 1: CI/CD Pipeline with GitHub Actions (includes frontend lint + build steps)
- ✅ Phase 2: Hexagonal Architecture (backend)
- ✅ Phase 3: Database schema (all tables created including users, companies, roles)
- ✅ Phase 4: JWT Authentication API:
  - `POST /api/v1/auth/register` — User registration (returns tokens)
  - `POST /api/v1/auth/login` — User login (returns access + refresh tokens)
  - `POST /api/v1/auth/refresh` — Refresh access token
  - `POST /api/v1/auth/logout` — Invalidate refresh token
- ✅ Phase 5: RBAC with 4 roles (ADMIN, MANAGER, ACCOUNTANT, INTERN)
- ✅ Phase 6: Company & User Management API
- ✅ Phase 7: Invoice CRUD API (with categories, status workflow, items)
- ✅ Phase 8: Audit Log Mechanism
- ✅ Phase 9: Duplication Control
- ✅ Phase 10: Frontend Layout and Routing:
  - Complete layout skeleton with sidebar and header
  - All routes created with placeholder pages
  - Shadcn/ui components installed and configured
  - Auth store (Zustand) skeleton ready
  - API client (Axios) with interceptors prepared
  - TypeScript types defined for auth, user, company
  - Dark/light theme working
  - Mobile responsive sidebar
  - Protected route middleware skeleton ready

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Implement fully functional authentication pages (Login, Register) with complete backend integration, JWT token management via Zustand store, automatic token refresh, protected route enforcement, and logout functionality. By the end of this phase, users should be able to register, log in, have their sessions persist across page refreshes, be automatically redirected when unauthorized, and log out cleanly.

---

## DETAILED REQUIREMENTS

### 1. Backend API Reference

**Base URL**: `http://localhost:8080/api/v1`

#### 1.1 Authentication Endpoints

**POST /auth/register**
```json
// Request
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "fullName": "Ahmet Yılmaz",
  "companyId": "uuid-of-company"  // Optional for first user
}

// Response 201 Created
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,  // 15 minutes in seconds
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "fullName": "Ahmet Yılmaz",
    "role": "ADMIN",  // First user gets ADMIN
    "companyId": "uuid",
    "companyName": "Şirket Adı",
    "isActive": true,
    "createdAt": "2025-02-01T10:00:00Z"
  }
}

// Error 400 Bad Request
{
  "error": "VALIDATION_ERROR",
  "message": "Geçersiz giriş verileri",
  "details": {
    "email": "Geçerli bir e-posta adresi giriniz",
    "password": "Şifre en az 8 karakter olmalıdır"
  }
}

// Error 409 Conflict
{
  "error": "EMAIL_ALREADY_EXISTS",
  "message": "Bu e-posta adresi zaten kullanımda"
}
```

**POST /auth/login**
```json
// Request
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}

// Response 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "fullName": "Ahmet Yılmaz",
    "role": "ADMIN",
    "companyId": "uuid",
    "companyName": "Şirket Adı",
    "isActive": true,
    "createdAt": "2025-02-01T10:00:00Z"
  }
}

// Error 401 Unauthorized
{
  "error": "INVALID_CREDENTIALS",
  "message": "E-posta veya şifre hatalı"
}

// Error 403 Forbidden
{
  "error": "ACCOUNT_DISABLED",
  "message": "Hesabınız devre dışı bırakılmış"
}
```

**POST /auth/refresh**
```json
// Request
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

// Response 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",  // New refresh token (rotation)
  "tokenType": "Bearer",
  "expiresIn": 900
}

// Error 401 Unauthorized
{
  "error": "INVALID_REFRESH_TOKEN",
  "message": "Oturum süresi dolmuş, lütfen tekrar giriş yapın"
}
```

**POST /auth/logout**
```json
// Request
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

// Response 200 OK
{
  "message": "Başarıyla çıkış yapıldı"
}
```

**GET /auth/me** (Get current user - useful for session validation)
```json
// Headers: Authorization: Bearer <accessToken>

// Response 200 OK
{
  "id": "uuid",
  "email": "user@example.com",
  "fullName": "Ahmet Yılmaz",
  "role": "ADMIN",
  "companyId": "uuid",
  "companyName": "Şirket Adı",
  "isActive": true,
  "createdAt": "2025-02-01T10:00:00Z"
}

// Error 401 Unauthorized
{
  "error": "UNAUTHORIZED",
  "message": "Geçersiz veya süresi dolmuş token"
}
```

---

### 2. Zustand Auth Store Implementation

**File: `frontend/src/stores/auth-store.ts`**

The auth store skeleton was created in Phase 10. Now implement it fully:

```typescript
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { User, AuthTokens } from '@/types/auth';

interface AuthState {
  // State
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isInitialized: boolean;  // True after checking stored tokens on app load
  
  // Actions
  setAuth: (user: User, tokens: AuthTokens) => void;
  setTokens: (tokens: AuthTokens) => void;
  setUser: (user: User) => void;
  setLoading: (loading: boolean) => void;
  setInitialized: (initialized: boolean) => void;
  logout: () => void;
  
  // Computed helpers
  hasRole: (roles: string[]) => boolean;
  isAdmin: () => boolean;
  isManager: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      // Initial state
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      isInitialized: false,
      
      // Actions
      setAuth: (user, tokens) => set({
        user,
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        isAuthenticated: true,
        isLoading: false,
      }),
      
      setTokens: (tokens) => set({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      }),
      
      setUser: (user) => set({ user }),
      
      setLoading: (isLoading) => set({ isLoading }),
      
      setInitialized: (isInitialized) => set({ isInitialized }),
      
      logout: () => set({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
      }),
      
      // Computed helpers
      hasRole: (roles) => {
        const { user } = get();
        return user ? roles.includes(user.role) : false;
      },
      
      isAdmin: () => get().hasRole(['ADMIN']),
      
      isManager: () => get().hasRole(['ADMIN', 'MANAGER']),
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
```

---

### 3. API Client with Token Management

**File: `frontend/src/lib/api-client.ts`**

Extend the Axios client created in Phase 10 with full token management:

```typescript
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/auth-store';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

// Create axios instance
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const { accessToken } = useAuthStore.getState();
    
    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle token refresh
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: AxiosError | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    
    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Don't retry for auth endpoints
      if (originalRequest.url?.includes('/auth/')) {
        return Promise.reject(error);
      }
      
      if (isRefreshing) {
        // Queue the request while refreshing
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return apiClient(originalRequest);
        });
      }
      
      originalRequest._retry = true;
      isRefreshing = true;
      
      const { refreshToken, logout, setTokens } = useAuthStore.getState();
      
      if (!refreshToken) {
        logout();
        window.location.href = '/login';
        return Promise.reject(error);
      }
      
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken,
        });
        
        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;
        
        setTokens({
          accessToken: newAccessToken,
          refreshToken: newRefreshToken,
          tokenType: 'Bearer',
          expiresIn: 900,
        });
        
        processQueue(null, newAccessToken);
        
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as AxiosError, null);
        logout();
        window.location.href = '/login?session=expired';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

### 4. Auth Service Layer

**File: `frontend/src/services/auth-service.ts`**

```typescript
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
```

---

### 5. TypeScript Types

**File: `frontend/src/types/auth.ts`**

Update/extend the types created in Phase 10:

```typescript
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
```

---

### 6. Login Page Implementation

**File: `frontend/src/app/(auth)/login/page.tsx`**

```typescript
'use client';

import { useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2, Mail, Lock, AlertCircle } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';

import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';
import { ApiError } from '@/types/auth';

// Validation schema
const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'E-posta adresi gereklidir')
    .email('Geçerli bir e-posta adresi giriniz'),
  password: z
    .string()
    .min(1, 'Şifre gereklidir')
    .min(8, 'Şifre en az 8 karakter olmalıdır'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const { setAuth } = useAuthStore();
  
  // Check for session expired message
  const sessionExpired = searchParams.get('session') === 'expired';
  const redirectTo = searchParams.get('redirect') || '/';
  
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });
  
  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await authService.login(data);
      
      setAuth(response.user, {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
      });
      
      // Redirect to intended page or dashboard
      router.push(redirectTo);
      router.refresh();
    } catch (err) {
      const apiError = err as { response?: { data?: ApiError } };
      
      if (apiError.response?.data?.message) {
        setError(apiError.response.data.message);
      } else {
        setError('Giriş yapılırken bir hata oluştu. Lütfen tekrar deneyin.');
      }
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold">Giriş Yap</CardTitle>
          <CardDescription>
            Hesabınıza giriş yaparak devam edin
          </CardDescription>
        </CardHeader>
        
        <CardContent>
          {sessionExpired && (
            <Alert variant="destructive" className="mb-4">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                Oturumunuzun süresi dolmuş. Lütfen tekrar giriş yapın.
              </AlertDescription>
            </Alert>
          )}
          
          {error && (
            <Alert variant="destructive" className="mb-4">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">E-posta</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder="ornek@sirket.com"
                  className="pl-10"
                  disabled={isLoading}
                  {...register('email')}
                />
              </div>
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email.message}</p>
              )}
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="password">Şifre</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  className="pl-10"
                  disabled={isLoading}
                  {...register('password')}
                />
              </div>
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password.message}</p>
              )}
            </div>
            
            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Giriş yapılıyor...
                </>
              ) : (
                'Giriş Yap'
              )}
            </Button>
          </form>
        </CardContent>
        
        <CardFooter className="flex flex-col space-y-2">
          <div className="text-sm text-muted-foreground text-center">
            Hesabınız yok mu?{' '}
            <Link href="/register" className="text-primary hover:underline font-medium">
              Kayıt Ol
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}
```

---

### 7. Register Page Implementation

**File: `frontend/src/app/(auth)/register/page.tsx`**

```typescript
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2, Mail, Lock, User, AlertCircle, CheckCircle2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';

import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';
import { ApiError } from '@/types/auth';

// Password requirements
const passwordRequirements = [
  { regex: /.{8,}/, text: 'En az 8 karakter' },
  { regex: /[A-Z]/, text: 'En az bir büyük harf' },
  { regex: /[a-z]/, text: 'En az bir küçük harf' },
  { regex: /[0-9]/, text: 'En az bir rakam' },
  { regex: /[!@#$%^&*(),.?":{}|<>]/, text: 'En az bir özel karakter' },
];

// Validation schema
const registerSchema = z.object({
  fullName: z
    .string()
    .min(1, 'Ad soyad gereklidir')
    .min(3, 'Ad soyad en az 3 karakter olmalıdır')
    .max(100, 'Ad soyad en fazla 100 karakter olabilir'),
  email: z
    .string()
    .min(1, 'E-posta adresi gereklidir')
    .email('Geçerli bir e-posta adresi giriniz'),
  password: z
    .string()
    .min(1, 'Şifre gereklidir')
    .min(8, 'Şifre en az 8 karakter olmalıdır')
    .regex(/[A-Z]/, 'Şifre en az bir büyük harf içermelidir')
    .regex(/[a-z]/, 'Şifre en az bir küçük harf içermelidir')
    .regex(/[0-9]/, 'Şifre en az bir rakam içermelidir')
    .regex(/[!@#$%^&*(),.?":{}|<>]/, 'Şifre en az bir özel karakter içermelidir'),
  confirmPassword: z
    .string()
    .min(1, 'Şifre tekrarı gereklidir'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Şifreler eşleşmiyor',
  path: ['confirmPassword'],
});

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const { setAuth } = useAuthStore();
  
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  });
  
  const password = watch('password');
  
  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await authService.register({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
      });
      
      setAuth(response.user, {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
      });
      
      // Redirect to dashboard
      router.push('/');
      router.refresh();
    } catch (err) {
      const apiError = err as { response?: { data?: ApiError } };
      
      if (apiError.response?.data?.message) {
        setError(apiError.response.data.message);
      } else if (apiError.response?.data?.details) {
        // Show first validation error
        const firstError = Object.values(apiError.response.data.details)[0];
        setError(firstError);
      } else {
        setError('Kayıt olunurken bir hata oluştu. Lütfen tekrar deneyin.');
      }
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold">Kayıt Ol</CardTitle>
          <CardDescription>
            Yeni bir hesap oluşturun
          </CardDescription>
        </CardHeader>
        
        <CardContent>
          {error && (
            <Alert variant="destructive" className="mb-4">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="fullName">Ad Soyad</Label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="fullName"
                  type="text"
                  placeholder="Ahmet Yılmaz"
                  className="pl-10"
                  disabled={isLoading}
                  {...register('fullName')}
                />
              </div>
              {errors.fullName && (
                <p className="text-sm text-destructive">{errors.fullName.message}</p>
              )}
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="email">E-posta</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder="ornek@sirket.com"
                  className="pl-10"
                  disabled={isLoading}
                  {...register('email')}
                />
              </div>
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email.message}</p>
              )}
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="password">Şifre</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                  className="pl-10"
                  disabled={isLoading}
                  {...register('password')}
                />
              </div>
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password.message}</p>
              )}
              
              {/* Password requirements checklist */}
              {password && (
                <div className="mt-2 space-y-1">
                  {passwordRequirements.map((req, index) => (
                    <div
                      key={index}
                      className={`flex items-center text-xs ${
                        req.regex.test(password)
                          ? 'text-green-600 dark:text-green-400'
                          : 'text-muted-foreground'
                      }`}
                    >
                      <CheckCircle2 className={`h-3 w-3 mr-1 ${
                        req.regex.test(password) ? 'opacity-100' : 'opacity-30'
                      }`} />
                      {req.text}
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="confirmPassword">Şifre Tekrar</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="confirmPassword"
                  type="password"
                  placeholder="••••••••"
                  className="pl-10"
                  disabled={isLoading}
                  {...register('confirmPassword')}
                />
              </div>
              {errors.confirmPassword && (
                <p className="text-sm text-destructive">{errors.confirmPassword.message}</p>
              )}
            </div>
            
            <Button type="submit" className="w-full" disabled={isLoading}>
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Kayıt yapılıyor...
                </>
              ) : (
                'Kayıt Ol'
              )}
            </Button>
          </form>
        </CardContent>
        
        <CardFooter className="flex flex-col space-y-2">
          <div className="text-sm text-muted-foreground text-center">
            Zaten hesabınız var mı?{' '}
            <Link href="/login" className="text-primary hover:underline font-medium">
              Giriş Yap
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}
```

---

### 8. Auth Layout

**File: `frontend/src/app/(auth)/layout.tsx`**

```typescript
import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Kimlik Doğrulama | Fatura OCR',
  description: 'Fatura OCR sistemine giriş yapın veya kayıt olun',
};

interface AuthLayoutProps {
  children: React.ReactNode;
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-background to-muted">
      {/* Optional: Add logo or branding */}
      <div className="absolute top-6 left-6">
        <h1 className="text-xl font-bold text-primary">Fatura OCR</h1>
      </div>
      
      {children}
      
      {/* Optional: Footer */}
      <div className="absolute bottom-4 left-0 right-0 text-center text-sm text-muted-foreground">
        © 2025 Fatura OCR ve Veri Yönetim Sistemi
      </div>
    </div>
  );
}
```

---

### 9. Protected Route Middleware

**File: `frontend/src/middleware.ts`**

```typescript
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Routes that don't require authentication
const publicRoutes = ['/login', '/register'];

// Routes that require authentication
const protectedRoutes = [
  '/',
  '/invoices',
  '/upload',
  '/categories',
  '/users',
  '/company',
  '/audit-logs',
  '/profile',
  '/settings',
];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Check if route is public
  const isPublicRoute = publicRoutes.some(route => pathname.startsWith(route));
  
  // Get auth token from cookie or localStorage (via cookie)
  // Note: In middleware, we can only access cookies, not localStorage
  // The actual auth check with localStorage happens client-side via AuthProvider
  const authCookie = request.cookies.get('auth-storage');
  
  let isAuthenticated = false;
  
  if (authCookie) {
    try {
      const authData = JSON.parse(authCookie.value);
      isAuthenticated = authData.state?.isAuthenticated === true;
    } catch {
      isAuthenticated = false;
    }
  }
  
  // Redirect authenticated users away from auth pages
  if (isPublicRoute && isAuthenticated) {
    return NextResponse.redirect(new URL('/', request.url));
  }
  
  // Redirect unauthenticated users to login
  if (!isPublicRoute && !isAuthenticated) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('redirect', pathname);
    return NextResponse.redirect(loginUrl);
  }
  
  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public folder
     * - api routes
     */
    '/((?!_next/static|_next/image|favicon.ico|public|api).*)',
  ],
};
```

---

### 10. Auth Provider Component

**File: `frontend/src/components/providers/auth-provider.tsx`**

This component handles initialization and persistent auth state:

```typescript
'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';
import { LoadingSkeleton } from '@/components/common/loading-skeleton';

interface AuthProviderProps {
  children: React.ReactNode;
}

const publicRoutes = ['/login', '/register'];

export function AuthProvider({ children }: AuthProviderProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [isChecking, setIsChecking] = useState(true);
  
  const {
    isAuthenticated,
    isInitialized,
    accessToken,
    refreshToken,
    setAuth,
    setInitialized,
    setTokens,
    logout,
  } = useAuthStore();
  
  useEffect(() => {
    const initializeAuth = async () => {
      // If already initialized, skip
      if (isInitialized) {
        setIsChecking(false);
        return;
      }
      
      // If we have tokens, validate them
      if (accessToken && refreshToken) {
        try {
          // Try to get current user to validate token
          const user = await authService.getCurrentUser();
          setAuth(user, { accessToken, refreshToken, tokenType: 'Bearer', expiresIn: 900 });
        } catch (error) {
          // Token invalid, try to refresh
          try {
            const refreshResponse = await authService.refresh(refreshToken);
            setTokens({
              accessToken: refreshResponse.accessToken,
              refreshToken: refreshResponse.refreshToken,
              tokenType: refreshResponse.tokenType,
              expiresIn: refreshResponse.expiresIn,
            });
            
            // Get user with new token
            const user = await authService.getCurrentUser();
            setAuth(user, refreshResponse);
          } catch {
            // Refresh failed, logout
            logout();
          }
        }
      }
      
      setInitialized(true);
      setIsChecking(false);
    };
    
    initializeAuth();
  }, []);
  
  // Handle routing based on auth state
  useEffect(() => {
    if (!isInitialized || isChecking) return;
    
    const isPublicRoute = publicRoutes.includes(pathname);
    
    if (!isAuthenticated && !isPublicRoute) {
      router.push(`/login?redirect=${encodeURIComponent(pathname)}`);
    } else if (isAuthenticated && isPublicRoute) {
      router.push('/');
    }
  }, [isAuthenticated, isInitialized, isChecking, pathname, router]);
  
  // Show loading while checking auth
  if (isChecking) {
    return <LoadingSkeleton />;
  }
  
  return <>{children}</>;
}
```

---

### 11. Update Root Layout with AuthProvider

**File: `frontend/src/app/layout.tsx`** (Update)

```typescript
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';

import { ThemeProvider } from '@/components/providers/theme-provider';
import { QueryProvider } from '@/components/providers/query-provider';
import { AuthProvider } from '@/components/providers/auth-provider';
import { Toaster } from '@/components/ui/sonner';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'Fatura OCR ve Veri Yönetim Sistemi',
  description: 'LLM tabanlı fatura veri çıkarım ve yönetim sistemi',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="tr" suppressHydrationWarning>
      <body className={inter.className}>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          <QueryProvider>
            <AuthProvider>
              {children}
            </AuthProvider>
            <Toaster position="top-right" />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
```

---

### 12. Update User Menu with Logout

**File: `frontend/src/components/layout/user-menu.tsx`** (Update)

```typescript
'use client';

import { useRouter } from 'next/navigation';
import { User, Settings, LogOut, Moon, Sun } from 'lucide-react';
import { useTheme } from 'next-themes';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';

import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';
import { toast } from 'sonner';

export function UserMenu() {
  const router = useRouter();
  const { theme, setTheme } = useTheme();
  const { user, refreshToken, logout } = useAuthStore();
  
  const handleLogout = async () => {
    try {
      if (refreshToken) {
        await authService.logout(refreshToken);
      }
    } catch (error) {
      // Ignore logout API errors, proceed with local logout
      console.error('Logout API error:', error);
    } finally {
      logout();
      toast.success('Başarıyla çıkış yapıldı');
      router.push('/login');
      router.refresh();
    }
  };
  
  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };
  
  const getRoleBadge = (role: string) => {
    const roleLabels: Record<string, string> = {
      ADMIN: 'Yönetici',
      MANAGER: 'Müdür',
      ACCOUNTANT: 'Muhasebeci',
      INTERN: 'Stajyer',
    };
    return roleLabels[role] || role;
  };
  
  if (!user) return null;
  
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="relative h-10 w-10 rounded-full">
          <Avatar className="h-10 w-10">
            <AvatarFallback className="bg-primary text-primary-foreground">
              {getInitials(user.fullName)}
            </AvatarFallback>
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      
      <DropdownMenuContent className="w-56" align="end" forceMount>
        <DropdownMenuLabel className="font-normal">
          <div className="flex flex-col space-y-1">
            <p className="text-sm font-medium leading-none">{user.fullName}</p>
            <p className="text-xs leading-none text-muted-foreground">{user.email}</p>
            <p className="text-xs leading-none text-muted-foreground mt-1">
              {getRoleBadge(user.role)} • {user.companyName}
            </p>
          </div>
        </DropdownMenuLabel>
        
        <DropdownMenuSeparator />
        
        <DropdownMenuItem onClick={() => router.push('/profile')}>
          <User className="mr-2 h-4 w-4" />
          Profilim
        </DropdownMenuItem>
        
        <DropdownMenuItem onClick={() => router.push('/settings')}>
          <Settings className="mr-2 h-4 w-4" />
          Ayarlar
        </DropdownMenuItem>
        
        <DropdownMenuItem onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
          {theme === 'dark' ? (
            <Sun className="mr-2 h-4 w-4" />
          ) : (
            <Moon className="mr-2 h-4 w-4" />
          )}
          {theme === 'dark' ? 'Açık Tema' : 'Koyu Tema'}
        </DropdownMenuItem>
        
        <DropdownMenuSeparator />
        
        <DropdownMenuItem
          onClick={handleLogout}
          className="text-destructive focus:text-destructive"
        >
          <LogOut className="mr-2 h-4 w-4" />
          Çıkış Yap
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
```

---

### 13. Custom Hooks for Auth

**File: `frontend/src/hooks/use-auth.ts`**

```typescript
'use client';

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';
import { LoginRequest, RegisterRequest } from '@/types/auth';
import { toast } from 'sonner';

export function useAuth() {
  const router = useRouter();
  const {
    user,
    isAuthenticated,
    isLoading,
    setAuth,
    setLoading,
    logout: storeLogout,
    refreshToken,
    hasRole,
    isAdmin,
    isManager,
  } = useAuthStore();
  
  const login = useCallback(async (data: LoginRequest) => {
    setLoading(true);
    try {
      const response = await authService.login(data);
      setAuth(response.user, {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
      });
      toast.success(`Hoş geldiniz, ${response.user.fullName}!`);
      return response;
    } finally {
      setLoading(false);
    }
  }, [setAuth, setLoading]);
  
  const register = useCallback(async (data: RegisterRequest) => {
    setLoading(true);
    try {
      const response = await authService.register(data);
      setAuth(response.user, {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
      });
      toast.success('Hesabınız başarıyla oluşturuldu!');
      return response;
    } finally {
      setLoading(false);
    }
  }, [setAuth, setLoading]);
  
  const logout = useCallback(async () => {
    try {
      if (refreshToken) {
        await authService.logout(refreshToken);
      }
    } catch (error) {
      console.error('Logout API error:', error);
    } finally {
      storeLogout();
      toast.success('Başarıyla çıkış yapıldı');
      router.push('/login');
      router.refresh();
    }
  }, [refreshToken, storeLogout, router]);
  
  return {
    user,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout,
    hasRole,
    isAdmin,
    isManager,
  };
}
```

---

### 14. Add Required Shadcn/ui Components

If not already installed in Phase 10, add these components:

```bash
cd frontend

# Required for auth pages
npx shadcn-ui@latest add card
npx shadcn-ui@latest add alert
npx shadcn-ui@latest add form    # Optional: for better form handling
```

---

### 15. Environment Variables

**File: `frontend/.env.local`** (Update)

```env
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1

# App Configuration
NEXT_PUBLIC_APP_NAME=Fatura OCR
NEXT_PUBLIC_APP_VERSION=1.0.0
```

---

## TESTING REQUIREMENTS

### Manual Testing Steps

1. **Registration Flow**:
   - Navigate to `/register`
   - Enter valid data (fullName, email, password with all requirements, confirmPassword)
   - Submit → Should redirect to dashboard
   - Check localStorage for `auth-storage` key with user data
   - Verify user menu shows correct name and role

2. **Registration Validation**:
   - Submit empty form → All validation errors shown in Turkish
   - Enter invalid email → "Geçerli bir e-posta adresi giriniz"
   - Enter weak password → Password requirements checklist shows unchecked items
   - Enter mismatched passwords → "Şifreler eşleşmiyor"
   - Try existing email → "Bu e-posta adresi zaten kullanımda"

3. **Login Flow**:
   - Navigate to `/login`
   - Enter valid credentials
   - Submit → Should redirect to dashboard (or redirect param if present)
   - Verify tokens stored in localStorage

4. **Login Validation**:
   - Submit empty form → Validation errors
   - Enter wrong password → "E-posta veya şifre hatalı"
   - Enter non-existent email → "E-posta veya şifre hatalı" (same error for security)

5. **Session Persistence**:
   - Log in successfully
   - Refresh page → Should stay logged in
   - Close browser, reopen → Should stay logged in
   - User menu shows correct user info

6. **Token Refresh**:
   - Log in and wait 15+ minutes (or manually expire token in DevTools)
   - Make API request → Should automatically refresh token
   - No visible interruption to user

7. **Protected Routes**:
   - Log out
   - Navigate to `/invoices` directly → Should redirect to `/login?redirect=/invoices`
   - Log in → Should redirect back to `/invoices`

8. **Auth Pages Redirect**:
   - Log in
   - Navigate to `/login` → Should redirect to dashboard
   - Navigate to `/register` → Should redirect to dashboard

9. **Logout Flow**:
   - Click user avatar → Open dropdown
   - Click "Çıkış Yap"
   - Should redirect to login
   - localStorage should be cleared
   - Refresh token should be invalidated on backend

10. **Session Expired**:
    - Manually invalidate refresh token (via backend or clear localStorage)
    - Try to make API request → Should redirect to `/login?session=expired`
    - Alert should show "Oturumunuzun süresi dolmuş"

11. **Mobile Responsive**:
    - Test login/register pages on mobile viewport
    - Forms should be centered and usable
    - Keyboard should not break layout

### Build Verification

```bash
cd frontend
npm run build    # Should succeed with no errors
npm run lint     # Should pass with no errors
npm run dev      # Manual testing
```

### API Testing with curl

```bash
# Test registration
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!@#","fullName":"Test User"}'

# Test login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!@#"}'

# Test token refresh (replace with actual token)
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<your-refresh-token>"}'

# Test get current user (replace with actual token)
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <your-access-token>"
```

---

## VERIFICATION CHECKLIST

### Auth Store
- [ ] Zustand store with persist middleware implemented
- [ ] User, tokens, isAuthenticated state managed
- [ ] setAuth, setTokens, logout actions working
- [ ] hasRole, isAdmin, isManager helpers working
- [ ] State persisted to localStorage

### API Client
- [ ] Axios instance configured with base URL
- [ ] Request interceptor adds Authorization header
- [ ] Response interceptor handles 401 errors
- [ ] Token refresh mechanism working
- [ ] Request queue during refresh working
- [ ] Logout on refresh failure working

### Auth Service
- [ ] login() method working
- [ ] register() method working
- [ ] refresh() method working
- [ ] logout() method working
- [ ] getCurrentUser() method working

### Login Page
- [ ] Form with email and password fields
- [ ] Zod validation with Turkish messages
- [ ] Loading state during submission
- [ ] Error messages displayed correctly
- [ ] Success redirects to dashboard
- [ ] Session expired alert shown when applicable
- [ ] Link to register page

### Register Page
- [ ] Form with fullName, email, password, confirmPassword
- [ ] Password requirements checklist
- [ ] Zod validation with all rules
- [ ] Loading state during submission
- [ ] Error messages from API displayed
- [ ] Success redirects to dashboard
- [ ] Link to login page

### Auth Provider
- [ ] Initializes auth state on app load
- [ ] Validates stored tokens
- [ ] Handles token refresh on init
- [ ] Shows loading state during check
- [ ] Redirects unauthenticated users

### Middleware
- [ ] Protects dashboard routes
- [ ] Redirects to login with redirect param
- [ ] Redirects authenticated users from auth pages

### User Menu
- [ ] Shows user initials
- [ ] Shows user name and email
- [ ] Shows role and company
- [ ] Logout button working
- [ ] Theme toggle working

### Build & Lint
- [ ] `npm run build` passes
- [ ] `npm run lint` passes
- [ ] No TypeScript errors
- [ ] No console errors in browser

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_11_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time vs estimated (2-3 days)

### 2. Completed Tasks
List each task with checkbox:
- [ ] Auth store implementation
- [ ] API client with token management
- [ ] Auth service layer
- [ ] Login page with validation
- [ ] Register page with validation
- [ ] Auth provider component
- [ ] Protected route middleware
- [ ] User menu with logout
- [ ] Custom useAuth hook

### 3. Files Created/Modified
```
frontend/src/
├── stores/
│   └── auth-store.ts          [MODIFIED]
├── lib/
│   └── api-client.ts          [MODIFIED]
├── services/
│   └── auth-service.ts        [CREATED]
├── types/
│   └── auth.ts                [MODIFIED]
├── app/
│   ├── (auth)/
│   │   ├── layout.tsx         [MODIFIED]
│   │   ├── login/
│   │   │   └── page.tsx       [MODIFIED]
│   │   └── register/
│   │       └── page.tsx       [MODIFIED]
│   └── layout.tsx             [MODIFIED]
├── components/
│   ├── providers/
│   │   └── auth-provider.tsx  [CREATED]
│   └── layout/
│       └── user-menu.tsx      [MODIFIED]
├── hooks/
│   └── use-auth.ts            [CREATED]
└── middleware.ts              [CREATED]
```

### 4. Test Results

| Test Case | Status | Notes |
|-----------|--------|-------|
| Registration with valid data | ✅/❌ | |
| Registration validation errors | ✅/❌ | |
| Login with valid credentials | ✅/❌ | |
| Login validation errors | ✅/❌ | |
| Session persistence | ✅/❌ | |
| Token refresh | ✅/❌ | |
| Protected route redirect | ✅/❌ | |
| Logout flow | ✅/❌ | |
| Mobile responsive | ✅/❌ | |

### 5. API Integration Status
- Backend connection: Working / Not Working
- Auth endpoints tested: Yes / No
- Token management: Working / Issues

### 6. Screenshots
Include screenshots of:
- Login page (empty)
- Login page (with validation errors)
- Register page (with password requirements)
- User menu (logged in)
- Session expired alert

### 7. Build Output
```bash
$ npm run build
# Include actual output
```

### 8. Issues Encountered
Document any problems and solutions:
- Issue: ...
- Solution: ...

### 9. Database Changes
If any migration needed:
- [ ] No database changes required
- [ ] Migration file created: `V{X}__description.sql`

### 10. Next Steps
What needs to be done in Phase 12 (Invoice CRUD UI):
- Auth system ready for use
- API client configured for authenticated requests
- User context available via useAuth hook
- Ready to implement invoice list and forms

### 11. Time Spent
- Estimated: 2-3 days
- Actual: X days

---

## DEPENDENCIES

### Requires
- **Phase 4**: JWT Authentication API (backend endpoints must be working)
- **Phase 10**: Frontend Layout (layout, routing, store skeletons, types)

### Required By
- **Phase 12**: Invoice CRUD UI (needs auth context, API client)
- **Phase 20**: Invoice Upload UI (needs auth for file uploads)
- **Phase 26**: Dashboard (needs user context for role-based widgets)
- All subsequent frontend phases (authentication is foundational)

---

## SUCCESS CRITERIA

1. ✅ Users can register with validation
2. ✅ Users can log in with validation
3. ✅ JWT tokens stored securely in localStorage
4. ✅ Tokens automatically included in API requests
5. ✅ Token refresh works automatically on 401
6. ✅ Session persists across page refreshes
7. ✅ Protected routes redirect unauthenticated users
8. ✅ Auth pages redirect authenticated users
9. ✅ Logout clears local state and invalidates server token
10. ✅ User menu shows user info and role
11. ✅ All UI text in Turkish
12. ✅ Mobile responsive auth pages
13. ✅ Build and lint pass
14. ✅ Result file created at `docs/OMER/step_results/faz_11_result.md`

---

## IMPORTANT NOTES

1. **Backend Must Be Running**: This phase requires the backend auth endpoints from Phase 4 to be working. Test with curl first.

2. **Token Storage Security**: Tokens are stored in localStorage for simplicity. For production, consider httpOnly cookies for refresh tokens.

3. **Error Messages**: All error messages must be in Turkish and user-friendly. Don't expose technical details.

4. **Password Requirements**: Must match backend validation (min 8 chars, uppercase, lowercase, number, special char).

5. **First User**: The first registered user becomes ADMIN automatically. This is handled by backend.

6. **Company ID**: For initial registration, companyId is optional. The backend creates a new company.

7. **Zustand Persist**: Uses localStorage. The middleware reads from cookies (limited), so full auth check happens client-side.

8. **Race Conditions**: The token refresh mechanism queues requests to prevent multiple simultaneous refresh calls.

9. **Testing**: Always test both with running backend and with network errors to ensure graceful degradation.

---

**Phase 11 Completion Target**: Fully functional authentication system where users can register, log in, have persistent sessions, automatic token refresh, protected routes, and clean logout.
