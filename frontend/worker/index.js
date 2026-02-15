'use strict';

// Import Workbox modules
import { clientsClaim } from 'workbox-core';
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching';
import { registerRoute } from 'workbox-routing';
import { CacheFirst, NetworkFirst, StaleWhileRevalidate } from 'workbox-strategies';
import { ExpirationPlugin } from 'workbox-expiration';

// Claim clients immediately
clientsClaim();
self.skipWaiting();

// Precache resources
// self.__WB_MANIFEST is ignored if we use GenerateSW, but this file is intended for InjectManifest
cleanupOutdatedCaches();
precacheAndRoute(self.__WB_MANIFEST || []);

// -----------------------------------------------------------------------------
// PUSH NOTIFICATIONS (Phase 28 Logic)
// -----------------------------------------------------------------------------

self.addEventListener('push', (event) => {
    if (!event.data) {
        console.log('Push event but no data');
        return;
    }

    try {
        const data = event.data.json();
        const options = {
            body: data.body || 'Yeni bildirim',
            icon: '/icons/icon-192x192.png',
            badge: '/icons/icon-72x72.png',
            vibrate: [100, 50, 100],
            data: {
                url: data.url || '/',
                id: data.id
            },
            tag: data.tag || 'general-notification',
            renotify: true
        };

        event.waitUntil(
            self.registration.showNotification(data.title || 'FaturaOCR', options)
        );
    } catch (err) {
        console.error('Error processing push event:', err);
        // Fallback for non-JSON data
        const options = {
            body: event.data.text(),
            icon: '/icons/icon-192x192.png',
            data: { url: '/' }
        };
        event.waitUntil(
            self.registration.showNotification('FaturaOCR', options)
        );
    }
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();

    const urlToOpen = new URL(event.notification.data.url || '/', self.location.origin).href;

    const promiseChain = clients.matchAll({
        type: 'window',
        includeUncontrolled: true
    }).then((windowClients) => {
        // Check if there is already a window open with this URL
        for (let i = 0; i < windowClients.length; i++) {
            const client = windowClients[i];
            if (client.url === urlToOpen && 'focus' in client) {
                return client.focus();
            }
        }
        // If not, open a new window
        if (clients.openWindow) {
            return clients.openWindow(urlToOpen);
        }
    });

    event.waitUntil(promiseChain);
});

// -----------------------------------------------------------------------------
// CACHING STRATEGIES
// -----------------------------------------------------------------------------

// 1. Static Assets - Cache First
registerRoute(
    ({ request }) => request.destination === 'image' ||
        request.destination === 'style' ||
        request.destination === 'script' ||
        request.destination === 'font',
    new CacheFirst({
        cacheName: 'static-assets',
        plugins: [
            new ExpirationPlugin({
                maxEntries: 200,
                maxAgeSeconds: 365 * 24 * 60 * 60, // 365 days
            }),
        ],
    })
);

// 2. HTML Pages - Network First
registerRoute(
    ({ request }) => request.mode === 'navigate',
    new NetworkFirst({
        cacheName: 'pages',
        networkTimeoutSeconds: 3,
        plugins: [
            new ExpirationPlugin({
                maxEntries: 50,
                maxAgeSeconds: 24 * 60 * 60, // 24 hours
            }),
        ],
    })
);

// 3. API Responses - Stale While Revalidate
registerRoute(
    ({ url }) => url.pathname.startsWith('/api/v1/') && !url.pathname.includes('/auth/'),
    new StaleWhileRevalidate({
        cacheName: 'api-responses',
        plugins: [
            new ExpirationPlugin({
                maxEntries: 100,
                maxAgeSeconds: 15 * 60, // 15 minutes
            }),
        ],
    })
);

// 4. Offline Fallback
// This is handled by NetworkFirst for pages, but if that fails, we can catch it
// and serve the offline.html manually if needed, but workbox's NetworkFirst usually handles it
// via the cache. For explicit offline page fallback:
const pageFallback = new NetworkFirst({
    cacheName: 'pages',
    plugins: [
        new ExpirationPlugin({ maxEntries: 50 }),
    ]
});

registerRoute(
    ({ request }) => request.mode === 'navigate',
    async (options) => {
        try {
            return await pageFallback.handle(options);
        } catch (error) {
            return caches.match('/offline.html');
        }
    }
);
