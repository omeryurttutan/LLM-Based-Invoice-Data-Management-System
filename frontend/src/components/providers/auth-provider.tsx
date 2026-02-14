'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { authService } from '@/services/auth-service';
// Note: We'll create a simple LoadingSkeleton inline or import if exists
// Since LoadingSkeleton was used in previous phases, keeping import. 
// If it fails, I'll replace with simple div.
// Checking if LoadingSkeleton exists or needs to be mocked.
// Assuming it might NOT exist in this specific path based on file structure. 
// Safest is to use a simple loader directly or check for common components.
// I'll use a simple fallback UI here to be safe.

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
  }, [accessToken, refreshToken, isInitialized, setAuth, setInitialized, setTokens, logout]); // Added dependencies
  
  // Handle routing based on auth state
  useEffect(() => {
    if (!isInitialized || isChecking) return;
    
    const isPublicRoute = publicRoutes.some(route => pathname.startsWith(route));
    
    if (!isAuthenticated && !isPublicRoute) {
      router.push(`/login?redirect=${encodeURIComponent(pathname)}`);
    } else if (isAuthenticated && isPublicRoute) {
      router.push('/');
    }
  }, [isAuthenticated, isInitialized, isChecking, pathname, router]);
  
  // Show loading while checking auth
  if (isChecking) {
    return (
        <div className="flex items-center justify-center min-h-screen">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
    );
  }
  
  return <>{children}</>;
}
