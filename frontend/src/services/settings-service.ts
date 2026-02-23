import apiClient from './api-client';
import { API_ENDPOINTS } from './endpoints';

// Backend returns: { "EXTRACTION_COMPLETED": { "in_app": true, "email": false, "push": false }, ... }
// UI needs: { "EMAIL_NOTIFICATIONS": boolean, "SYSTEM_NOTIFICATIONS": boolean }

const DEFAULT_PREFERENCES: Record<string, boolean> = {
  EMAIL_NOTIFICATIONS: false,
  SYSTEM_NOTIFICATIONS: true,
};

function transformBackendToUI(backendPrefs: Record<string, Record<string, boolean>>): Record<string, boolean> {
  if (!backendPrefs || typeof backendPrefs !== 'object') {
    return { ...DEFAULT_PREFERENCES };
  }

  // Check if it's already in flat format (from a previous save by our UI)
  if (typeof Object.values(backendPrefs)[0] === 'boolean') {
    return backendPrefs as unknown as Record<string, boolean>;
  }

  // Derive email preference: true if ANY notification type has email enabled
  let emailEnabled = false;
  let inAppEnabled = true;

  for (const typePrefs of Object.values(backendPrefs)) {
    if (typePrefs && typeof typePrefs === 'object') {
      if (typePrefs.email) emailEnabled = true;
      if (typePrefs.in_app === false) inAppEnabled = false;
    }
  }

  return {
    EMAIL_NOTIFICATIONS: emailEnabled,
    SYSTEM_NOTIFICATIONS: inAppEnabled,
  };
}

function transformUIToBackend(uiPrefs: Record<string, boolean>): Record<string, boolean> {
  // Send as flat map - backend will merge into existing preferences
  return uiPrefs;
}

export const settingsService = {
  getNotificationPreferences: async (): Promise<Record<string, boolean>> => {
    try {
      const response = await apiClient.get(API_ENDPOINTS.NOTIFICATION_PREFERENCES);
      return transformBackendToUI(response.data);
    } catch {
      // Return defaults on error so settings page still renders
      return { ...DEFAULT_PREFERENCES };
    }
  },

  updateNotificationPreferences: async (preferences: Record<string, boolean>): Promise<void> => {
    await apiClient.put(API_ENDPOINTS.NOTIFICATION_PREFERENCES, transformUIToBackend(preferences));
  }
};

