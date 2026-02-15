import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import { ThemeProvider } from '@/components/providers/theme-provider'
import QueryProvider from '@/components/providers/query-provider'
import { ToastProvider } from '@/components/providers/toast-provider'
import { AuthProvider } from '@/components/providers/auth-provider'
import { InstallPrompt } from '@/components/pwa/install-prompt'
import { UpdatePrompt } from '@/components/pwa/update-prompt'
import { OfflineStatus } from '@/components/pwa/offline-status'

import { NextIntlClientProvider } from 'next-intl';
import { getLocale, getMessages } from 'next-intl/server';

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
    title: 'Fatura OCR ve Veri Yönetim Sistemi',
    description: 'Invoice OCR and Data Management System',
}

export default async function RootLayout({
    children,
}: {
    children: React.ReactNode
}) {
    const locale = await getLocale();
    const messages = await getMessages();

    return (
        <html lang={locale} suppressHydrationWarning>
            <head>
                <link rel="manifest" href="/manifest.json" />
                <meta name="theme-color" content="#1e40af" />
                <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover" />
                <meta name="apple-mobile-web-app-capable" content="yes" />
                <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
            </head>
            <body className={inter.className}>
                <NextIntlClientProvider messages={messages}>
                    <QueryProvider>
                        <ThemeProvider
                            attribute="class"
                            defaultTheme="system"
                            enableSystem
                            disableTransitionOnChange
                        >
                            <AuthProvider>
                                {children}
                                <OfflineStatus />
                                <UpdatePrompt />
                                <InstallPrompt />
                            </AuthProvider>
                            <ToastProvider />
                        </ThemeProvider>
                    </QueryProvider>
                </NextIntlClientProvider>
            </body>
        </html>
    )
}
