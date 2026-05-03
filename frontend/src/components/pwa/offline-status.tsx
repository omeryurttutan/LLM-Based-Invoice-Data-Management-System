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
        <div className="fixed top-16 left-0 right-0 z-[60] flex justify-center px-4 pointer-events-none animate-in slide-in-from-top duration-300">
            <div className="bg-yellow-600 text-white px-4 py-2 rounded-full shadow-lg flex items-center gap-2 text-sm font-medium backdrop-blur-sm pointer-events-auto">
                <WifiOff className="h-4 w-4" />
                <span>Çevrimdışı moddasınız. Bağlantı bekleniyor...</span>
            </div>
        </div>
    );
}
