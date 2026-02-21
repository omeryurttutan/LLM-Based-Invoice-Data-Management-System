'use client';

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ArrowRight, LogOut } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import { useTranslations } from "next-intl";

export function PublicNav() {
    const { isAuthenticated, logout } = useAuth();
    // Assuming you have translations, if not we'll use Turkish directly for now
    // as it was in the original page.tsx
    
    if (isAuthenticated) {
        return (
            <>
                <Button variant="ghost" className="text-sm font-medium hover:underline underline-offset-4 mt-2" onClick={logout}>
                    <LogOut className="mr-2 h-4 w-4" />
                    Çıkış Yap
                </Button>
                <Link href="/dashboard">
                    <Button>
                        Sisteme Git <ArrowRight className="ml-2 h-4 w-4" />
                    </Button>
                </Link>
            </>
        );
    }

    return (
        <>
            <Link className="text-sm font-medium hover:underline underline-offset-4 mt-2" href="/login">
                Giriş Yap
            </Link>
            <Link href="/login?redirect=/dashboard">
                <Button>
                    Sisteme Git <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
            </Link>
        </>
    );
}

export function PublicHeroButtons() {
    const { isAuthenticated } = useAuth();
    
    if (isAuthenticated) {
        return (
            <div className="space-x-4 pt-4">
                <Link href="/dashboard">
                    <Button size="lg" className="h-12 px-8">Dashboard&apos;a Git</Button>
                </Link>
            </div>
        );
    }

    return (
        <div className="space-x-4 pt-4">
            <Link href="/login">
                <Button size="lg" className="h-12 px-8">Hemen Başla</Button>
            </Link>
            <Link href="/login?redirect=/dashboard">
                <Button variant="outline" size="lg" className="h-12 px-8">
                    Dashboard&apos;u İncele
                </Button>
            </Link>
        </div>
    );
}
