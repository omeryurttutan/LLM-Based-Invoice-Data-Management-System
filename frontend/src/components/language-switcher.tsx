'use client';

import { useLocale, useTranslations } from 'next-intl';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Languages } from 'lucide-react';

export function LanguageSwitcher() {
    const locale = useLocale();
    const router = useRouter();
    const t = useTranslations('navigation.header');

    const switchLocale = (newLocale: string) => {
        // Set cookie for next-intl
        document.cookie = `NEXT_LOCALE=${newLocale}; path=/; max-age=31536000; SameSite=Lax`;

        // Use router.refresh() to re-fetch server components with new locale
        // instead of window.location.reload() which can crash SSR without middleware
        router.refresh();
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label={t('userMenu.settings')}>
                    <Languages className="h-[1.2rem] w-[1.2rem]" />
                    <span className="sr-only">Dil Seçimi</span>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => switchLocale('tr')} className={locale === 'tr' ? 'bg-accent' : ''}>
                    🇹🇷 Türkçe
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => switchLocale('en')} className={locale === 'en' ? 'bg-accent' : ''}>
                    🇬🇧 English
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

