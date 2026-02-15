'use client';

import { useLocale, useTranslations } from 'next-intl';
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
    const t = useTranslations('navigation.header');

    const switchLocale = (newLocale: string) => {
        // Set cookie for next-intl middleware
        document.cookie = `NEXT_LOCALE=${newLocale}; path=/; max-age=31536000; SameSite=Lax`;

        // Refresh to apply new locale
        window.location.reload();
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
