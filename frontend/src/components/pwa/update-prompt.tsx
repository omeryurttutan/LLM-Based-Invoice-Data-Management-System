'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { RefreshCw } from 'lucide-react';
import { toast } from 'sonner';

export function UpdatePrompt() {
    const [showReload, setShowReload] = useState(false);
    const [waitingWorker, setWaitingWorker] = useState<ServiceWorker | null>(null);

    useEffect(() => {
        if (
            typeof window !== 'undefined' &&
            'serviceWorker' in navigator &&
            // @ts-ignore
            window.workbox !== undefined
        ) {
            // @ts-ignore
            const wb = window.workbox;

            wb.addEventListener('waiting', () => {
                setShowReload(true);
                setWaitingWorker(null); // Workbox handles the registration
            });

            wb.register();
        }
    }, []);

    // Alternative method using standard SW API if workbox window isn't available
    useEffect(() => {
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.getRegistration().then(reg => {
                if (reg && reg.waiting) {
                    setWaitingWorker(reg.waiting);
                    setShowReload(true);
                }

                if (reg) {
                    reg.addEventListener('updatefound', () => {
                        const newWorker = reg.installing;
                        if (newWorker) {
                            newWorker.addEventListener('statechange', () => {
                                if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                                    setWaitingWorker(newWorker);
                                    setShowReload(true);
                                }
                            });
                        }
                    });
                }
            });
        }
    }, []);

    const reloadPage = () => {
        if (waitingWorker) {
            waitingWorker.postMessage({ type: 'SKIP_WAITING' });
            // Also accept 'message' event listener in SW for some configurations
            waitingWorker.postMessage('SKIP_WAITING');
        }

        // Also try Workbox message
        // @ts-ignore
        if (window.workbox) {
            // @ts-ignore
            window.workbox.messageSkipWaiting();
        }

        setTimeout(() => {
            window.location.reload();
        }, 300); // Give it a moment
    };

    if (!showReload) return null;

    return (
        <div className="fixed top-16 left-0 right-0 z-50 flex justify-center px-4 animate-in slide-in-from-top duration-300">
            <div className="bg-primary text-primary-foreground px-4 py-3 rounded-full shadow-lg flex items-center gap-4 text-sm font-medium">
                <span>Yeni sürüm mevcut</span>
                <Button
                    variant="secondary"
                    size="sm"
                    className="h-7 px-3 text-xs"
                    onClick={reloadPage}
                >
                    <RefreshCw className="mr-1.5 h-3 w-3" />
                    Güncelle
                </Button>
            </div>
        </div>
    );
}
