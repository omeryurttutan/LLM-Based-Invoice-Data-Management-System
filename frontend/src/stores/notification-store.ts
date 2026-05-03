import { create } from 'zustand';
import { Notification } from '@/types/notification';

interface NotificationState {
    notifications: Notification[];
    unreadCount: number;
    isConnected: boolean;
    isDropdownOpen: boolean;

    // Actions
    addNotification: (notification: Notification) => void;
    setNotifications: (notifications: Notification[]) => void;
    setUnreadCount: (count: number) => void;
    markAsRead: (id: number) => void;
    markAllAsRead: () => void;
    removeNotification: (id: number) => void;
    setConnected: (status: boolean) => void;
    toggleDropdown: () => void;
    setDropdownOpen: (isOpen: boolean) => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
    notifications: [],
    unreadCount: 0,
    isConnected: false,
    isDropdownOpen: false,

    addNotification: (notification) => set((state) => {
        // Prevent duplicates if needed, though ID should be unique from backend
        const exists = state.notifications.some(n => n.id === notification.id);
        if (exists) return state;

        return {
            notifications: [notification, ...state.notifications].slice(0, 50), // Keep last 50
            unreadCount: state.unreadCount + 1
        };
    }),

    setNotifications: (notifications) => set({ notifications }),

    setUnreadCount: (unreadCount) => set({ unreadCount }),

    markAsRead: (id) => set((state) => {
        const notification = state.notifications.find(n => n.id === id);
        if (notification && !notification.read) {
            return {
                notifications: state.notifications.map(n =>
                    n.id === id ? { ...n, read: true } : n
                ),
                unreadCount: Math.max(0, state.unreadCount - 1)
            };
        }
        return state;
    }),

    markAllAsRead: () => set((state) => ({
        notifications: state.notifications.map(n => ({ ...n, read: true })),
        unreadCount: 0
    })),

    removeNotification: (id) => set((state) => {
        const notification = state.notifications.find(n => n.id === id);
        const wasUnread = notification ? !notification.read : false;

        return {
            notifications: state.notifications.filter(n => n.id !== id),
            unreadCount: wasUnread ? Math.max(0, state.unreadCount - 1) : state.unreadCount
        };
    }),

    setConnected: (isConnected) => set({ isConnected }),

    toggleDropdown: () => set((state) => ({ isDropdownOpen: !state.isDropdownOpen })),

    setDropdownOpen: (isDropdownOpen) => set({ isDropdownOpen }),
}));
