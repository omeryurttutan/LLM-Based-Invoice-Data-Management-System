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

  // Multi-tenancy
  activeCompanyId: string | null;

  // Actions
  setAuth: (user: User, tokens: AuthTokens) => void;
  setTokens: (tokens: AuthTokens) => void;
  setUser: (user: User) => void;
  setLoading: (loading: boolean) => void;
  setInitialized: (initialized: boolean) => void;
  setActiveCompanyId: (companyId: string) => void;
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
      activeCompanyId: null,

      // Actions
      setAuth: (user, tokens) => set({
        user,
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        isAuthenticated: true,
        isLoading: false,
        activeCompanyId: user.companyId, // default active company
      }),

      setTokens: (tokens) => set({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      }),

      setUser: (user) => set({ user }),

      setLoading: (isLoading) => set({ isLoading }),

      setInitialized: (isInitialized) => set({ isInitialized }),

      setActiveCompanyId: (companyId) => set({ activeCompanyId: companyId }),

      logout: () => set({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
        activeCompanyId: null,
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
        activeCompanyId: state.activeCompanyId,
      }),
    }
  )
);
