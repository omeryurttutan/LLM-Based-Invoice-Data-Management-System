import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from 'sonner';
import { getCookie } from '@/lib/utils';
import trCommon from '@/messages/tr/common.json';
import enCommon from '@/messages/en/common.json';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8082/api/v1';

// Simplified translation helper for non-React context
const getTranslation = (key: string, params?: Record<string, string | number>) => {
  const locale = getCookie('NEXT_LOCALE') || 'tr';
  const messages = locale === 'en' ? enCommon : trCommon;

  // Use 'any' to bypass strict type checking for deep nested keys in this utility
  // This is a tradeoff for using it outside of next-intl context components
  const keys = key.split('.');
  let value: any = messages;

  for (const k of keys) {
    if (value && typeof value === 'object' && k in value) {
      value = value[k as keyof typeof value];
    } else {
      return key; // Fallback to key if not found
    }
  }

  if (typeof value === 'string' && params) {
    let result = value;
    Object.entries(params).forEach(([k, v]) => {
      result = result.replace(`{${k}}`, String(v));
    });
    return result;
  }

  return typeof value === 'string' ? value : key;
};

// Error code mapping
const ERROR_CODE_MAP: Record<string, string> = {
  'INVOICE_NOT_FOUND': 'messages.error.notFound',
  'UNAUTHORIZED': 'messages.error.unauthorized',
  'RATE_LIMIT_EXCEEDED': 'messages.error.rateLimited',
  'VALIDATION_ERROR': 'messages.error.validation',
  'DUPLICATE_INVOICE': 'messages.error.serverError', // Fallback or specific
};

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
    const { accessToken, activeCompanyId } = useAuthStore.getState();

    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    if (activeCompanyId && config.headers) {
      config.headers['X-Company-Id'] = activeCompanyId;
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
  (response) => {
    // If the response follows our standard ApiResponse wrapping, unwrap the data field
    // but check if it's actually an ApiResponse structure first to avoid breaking other responses (like Blobs)
    if (response.data &&
      typeof response.data === 'object' &&
      'success' in response.data &&
      'data' in response.data) {
      return {
        ...response,
        data: response.data.data
      };
    }
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // If generic error handling is needed (e.g. backend returns error code in response data)
    // We can map it here. For now, let's look at status codes.

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
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }

      try {
        // We use a separate instance to avoid circular dependency loop if refresh fails
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
        if (typeof window !== 'undefined') {
          window.location.href = '/login?session=expired';
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // If 429 Too Many Requests
    if (error.response?.status === 429) {
      const retryAfter = parseInt(error.response.headers['retry-after'] || '60', 10);

      // Show localized toast without awaiting an artificial sleep
      toast.warning(getTranslation('messages.error.rateLimited') + ` (${retryAfter}s)`, {
        id: 'rate-limit-toast'
      });

      // Reject the promise immediately so the UI doesn't freeze
      // Components can catch this and handle gracefully instead of being stuck
      (error as any).translatedMessage = getTranslation('messages.error.rateLimited');
      return Promise.reject(error);
    }

    // Handle other errors (optional translation mapping)
    const errorCode = (error.response?.data as any)?.code;
    if (errorCode && ERROR_CODE_MAP[errorCode]) {
      // If we wanted to toast automatically here, we could.
      // But usually components handle specific errors.
      // Let's attach the translated message to the error object for components to use
      (error as any).translatedMessage = getTranslation(ERROR_CODE_MAP[errorCode]);
    }

    return Promise.reject(error);
  }
);

export default apiClient;
