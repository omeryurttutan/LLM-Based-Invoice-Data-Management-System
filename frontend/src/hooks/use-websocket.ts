import { useEffect, useRef, useCallback } from 'react';
import { Client, Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/stores/auth-store';
import { useNotificationStore } from '@/stores/notification-store';
import { toast } from 'sonner';

const RECONNECT_DELAY_BASE = 5000;
const MAX_RECONNECT_DELAY = 60000;

export function useWebSocket() {
    const { accessToken, isAuthenticated } = useAuthStore();
    const { addNotification, setConnected } = useNotificationStore();
    const clientRef = useRef<Client | null>(null);
    const reconnectDelayRef = useRef(RECONNECT_DELAY_BASE);
    const connectionTimeoutRef = useRef<NodeJS.Timeout>();

    const connect = useCallback(() => {
        if (!isAuthenticated || !accessToken) return;

        // Clear any pending connection attempts
        if (connectionTimeoutRef.current) {
            clearTimeout(connectionTimeoutRef.current);
        }

        const socketUrl = process.env.NEXT_PUBLIC_API_URL
            ? `${process.env.NEXT_PUBLIC_API_URL.replace('/api/v1', '')}/ws`
            : 'http://localhost:8082/ws';

        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            connectHeaders: {
                Authorization: `Bearer ${accessToken}`,
            },
            debug: (str) => {
                if (process.env.NODE_ENV === 'development') {
                    console.log('[WS] ' + str);
                }
            },
            reconnectDelay: 5000, // Automatic reconnect managed by stompjs
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = (frame) => {
            console.log('[WS] Connected');
            setConnected(true);
            reconnectDelayRef.current = RECONNECT_DELAY_BASE; // Reset backoff

            client.subscribe('/user/queue/notifications', (message) => {
                try {
                    const notification = JSON.parse(message.body);
                    addNotification(notification);

                    // Show toast
                    toast(notification.title, {
                        description: notification.message,
                        action: {
                            label: 'Görüntüle',
                            onClick: () => console.log('Navigate to:', notification), // TO DO: Implement navigation
                        },
                        // Simple mapping for visuals
                        // success: "success", error: "error", etc can be handled by sonner types if needed
                        // for now default styling, refined in component
                        style: {
                            borderLeft: `4px solid ${notification.severity === 'SUCCESS' ? '#22c55e' :
                                notification.severity === 'ERROR' ? '#ef4444' :
                                    notification.severity === 'WARNING' ? '#eab308' : '#3b82f6'
                                }`
                        }
                    });

                } catch (error) {
                    console.error('[WS] Error parsing message:', error);
                }
            });
        };

        client.onStompError = (frame) => {
            console.error('[WS] Broker reported error: ' + frame.headers['message']);
            console.error('[WS] Additional details: ' + frame.body);
            setConnected(false);
        };

        client.onWebSocketClose = () => {
            console.log('[WS] Connection closed');
            setConnected(false);
        };

        client.activate();
        clientRef.current = client;

    }, [accessToken, isAuthenticated, addNotification, setConnected]);

    const disconnect = useCallback(() => {
        if (clientRef.current) {
            clientRef.current.deactivate();
            clientRef.current = null;
            setConnected(false);
        }
    }, [setConnected]);

    useEffect(() => {
        if (isAuthenticated) {
            connect();
        } else {
            disconnect();
        }

        return () => {
            disconnect();
        };
    }, [isAuthenticated, connect, disconnect]);

    // Handle manual reconnect if needed, or let stompjs handle it
    // StompJS has built-in reconnect logic via `reconnectDelay` config
}
