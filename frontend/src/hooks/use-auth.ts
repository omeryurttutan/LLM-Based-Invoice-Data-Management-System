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
    } catch (error) {
        // Re-throw to let the UI handle the specific error message
        throw error;
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
    } catch (error) {
        throw error;
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
