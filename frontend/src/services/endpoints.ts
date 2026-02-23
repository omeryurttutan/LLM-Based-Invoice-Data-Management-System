export const API_ENDPOINTS = {
  // Auth
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  REFRESH: '/auth/refresh',
  LOGOUT: '/auth/logout',
  
  // Invoices
  INVOICES: '/invoices',
  INVOICE_DETAIL: (id: string) => `/invoices/${id}`,
  INVOICE_VERIFY: (id: string) => `/invoices/${id}/verify`,
  INVOICE_REJECT: (id: string) => `/invoices/${id}/reject`,
  INVOICE_CHECK_DUPLICATE: '/invoices/check-duplicate',
  
  // Categories
  CATEGORIES: '/categories',
  
  // Users
  USERS: '/users',
  USER_DETAIL: (id: string) => `/users/${id}`,
  USER_ACTIVATE: (id: string) => `/users/${id}/toggle-active`,
  USER_ROLE: (id: string) => `/users/${id}/role`,

  PROFILE: '/profile',
  PROFILE_PASSWORD: '/profile/password',
  
  // Companies
  COMPANIES: '/companies',
  MY_COMPANY: '/companies/me',
  
  // Audit
  AUDIT_LOGS: '/audit-logs',

  // Settings / Notifications
  NOTIFICATION_PREFERENCES: '/notifications/preferences',
};
