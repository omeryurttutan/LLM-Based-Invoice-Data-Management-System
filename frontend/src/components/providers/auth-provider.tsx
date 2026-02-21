'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';

interface AuthProviderProps {
  children: React.ReactNode;
}

const publicRoutes = ['/login', '/register', '/test-route'];

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
          // Timeout promise
          const timeoutPromise = new Promise((_, reject) =>
            setTimeout(() => reject(new Error('Auth check timeout')), 3000)
          );

          // Try to get current user to validate token with timeout
          const user = await Promise.race([
            authService.getCurrentUser(),
            timeoutPromise
          ]) as any;

          setAuth(user, { accessToken, refreshToken, tokenType: 'Bearer', expiresIn: 900 });
        } catch (error) {
          console.error('Auth initialization error:', error);

          // Only attempt refresh if it wasn't a timeout
          if (error instanceof Error && error.message !== 'Auth check timeout') {
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
          } else {
            // On timeout or complete failure, ensure we logout to clear broken persisted state
            console.warn('Auth check timed out or failed, proceeding to logout');
            logout();
          }
        }
      } else {
        // No tokens but potentially persisted isAuthenticated=true (invalid state)
        if (isAuthenticated) {
          logout();
        }
      }

      setInitialized(true);
      setIsChecking(false);
    };

    initializeAuth();
  }, [accessToken, refreshToken, isInitialized, setAuth, setInitialized, setTokens, logout]);

  // Handle routing based on auth state
  useEffect(() => {
    if (!isInitialized || isChecking) return;

    const isPublicRoute = publicRoutes.some(route => pathname.startsWith(route));

    if (!isAuthenticated && !isPublicRoute) {
      router.replace(`/login?redirect=${encodeURIComponent(pathname)}`);
    } else if (isAuthenticated && isPublicRoute) {
      // Honor the 'redirect' query parameter if it exists
      const searchParams = new URLSearchParams(window.location.search);
      const redirectPath = searchParams.get('redirect') || '/';
      router.push(redirectPath);
    }
  }, [isAuthenticated, isInitialized, isChecking, pathname, router]);

  // Show loading while checking auth
  if (isChecking) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="flex flex-col items-center gap-4">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          <p className="text-sm text-muted-foreground">Yükleniyor...</p>
        </div>
      </div>
    );
  }

  // Prevent flash of protected content before redirect
  const isPublicRoute = publicRoutes.some(route => pathname.startsWith(route));
  if (!isChecking && !isAuthenticated && !isPublicRoute) {
    return null;
  }

  return <>{children}</>;
}
