'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription, SheetFooter } from '@/components/ui/sheet';
import { Download, X } from 'lucide-react';
import Image from 'next/image';

export function InstallPrompt() {
    const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
    const [showPrompt, setShowPrompt] = useState(false);

    useEffect(() => {
        const handler = (e: any) => {
            e.preventDefault();
            setDeferredPrompt(e);

            // Check if user has already dismissed
            const hasDismissed = localStorage.getItem('pwa-install-dismissed');
            if (hasDismissed) {
                const dismissedTime = parseInt(hasDismissed);
                const now = Date.now();
                // Show again after 7 days
                if (now - dismissedTime < 7 * 24 * 60 * 60 * 1000) return;
            }

            // Show after a delay to not be intrusive
            // Also check if we are on mobile (simplified check)
            const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
            if (isMobile) {
                setTimeout(() => setShowPrompt(true), 3000);
            }
        };

        window.addEventListener('beforeinstallprompt', handler);

        return () => window.removeEventListener('beforeinstallprompt', handler);
    }, []);

    const handleInstall = async () => {
        if (!deferredPrompt) return;
        deferredPrompt.prompt();
        const { outcome } = await deferredPrompt.userChoice;
        if (outcome === 'accepted') {
            setDeferredPrompt(null);
            setShowPrompt(false);
        }
    };

    const handleDismiss = () => {
        setShowPrompt(false);
        localStorage.setItem('pwa-install-dismissed', Date.now().toString());
    };

    if (!showPrompt) return null;

    return (
        <div className="fixed bottom-0 left-0 right-0 p-4 z-50 animate-in slide-in-from-bottom duration-500">
            <div className="bg-background border rounded-lg shadow-lg p-4 flex flex-col gap-4 max-w-sm mx-auto">
                <div className="flex items-start justify-between">
                    <div className="flex gap-3">
                        <div className="relative w-12 h-12 rounded-lg overflow-hidden bg-primary/10">
                            <Image
                                src="/icons/icon-192x192.png"
                                alt="App Icon"
                                width={48}
                                height={48}
                                className="object-cover"
                            />
                        </div>
                        <div>
                            <h3 className="font-semibold text-foreground">FaturaOCR&apos;u Yükle</h3>
                            <p className="text-sm text-muted-foreground">Ana ekrana ekleyerek daha hızlı erişin.</p>
                        </div>
                    </div>
                    <Button variant="ghost" size="icon" className="h-6 w-6" onClick={handleDismiss}>
                        <X className="h-4 w-4" />
                    </Button>
                </div>
                <div className="flex gap-2">
                    <Button variant="outline" className="flex-1" onClick={handleDismiss}>
                        Daha Sonra
                    </Button>
                    <Button className="flex-1" onClick={handleInstall}>
                        <Download className="mr-2 h-4 w-4" />
                        Yükle
                    </Button>
                </div>
            </div>
        </div>
    );
}
