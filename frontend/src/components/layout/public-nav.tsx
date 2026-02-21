'use client';

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ArrowRight } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import { useTranslations } from "next-intl";
import { UserMenu } from "@/components/layout/user-menu";

export function PublicNav() {
    const { isAuthenticated, logout } = useAuth();
    // Assuming you have translations, if not we'll use Turkish directly for now
    // as it was in the original page.tsx
    
    if (isAuthenticated) {
        return (
            <div className="flex items-center gap-4">
                <UserMenu />
            </div>
        );
    }

    return (
        <div className="flex items-center gap-4">
            <Link className="text-sm font-medium hover:underline underline-offset-4" href="/login">
                Giriş Yap
            </Link>
            <Link href="/login?redirect=/dashboard">
                <Button>
                    Sisteme Git <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
            </Link>
        </div>
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
