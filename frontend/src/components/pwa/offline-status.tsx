'use client';

import { useState, useEffect } from 'react';
import { WifiOff, Wifi } from 'lucide-react';
import { toast } from 'sonner';

export function OfflineStatus() {
    const [isOffline, setIsOffline] = useState(false);

    useEffect(() => {
        const handleOnline = () => {
            setIsOffline(false);
            toast.success('Bağlantı yeniden sağlandı', {
                icon: <Wifi className="h-4 w-4" />,
                duration: 3000
            });
        };

        const handleOffline = () => {
            setIsOffline(true);
        };

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        // Check initial status
        if (!navigator.onLine) {
            setIsOffline(true);
        }

        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, []);

    if (!isOffline) return null;

    return (
        <div className="fixed top-0 left-0 right-0 z-[60] bg-yellow-500/90 text-white px-4 py-1 text-center text-xs font-medium backdrop-blur-sm">
            <div className="flex items-center justify-center gap-2">
                <WifiOff className="h-3 w-3" />
                <span>Çevrimdışı moddasınız. Bazı özellikler kısıtlı olabilir.</span>
            </div>
        </div>
    );
}
