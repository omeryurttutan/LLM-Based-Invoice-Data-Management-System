import Link from "next/link";
import { FileText } from "lucide-react";
import { PublicNav, PublicHeroButtons } from "@/components/layout/public-nav";

export default function LandingPage() {
    return (
        <div className="flex flex-col min-h-screen bg-background text-foreground">
            <header className="px-6 lg:px-14 h-20 flex items-center border-b">
                <Link className="flex items-center justify-center font-bold text-2xl gap-2" href="#">
                    <FileText className="h-8 w-8 text-primary" />
                    <span>Fatura OCR</span>
                </Link>
                <nav className="ml-auto flex items-center gap-4 sm:gap-6">
                    <PublicNav />
                </nav>
            </header>
            <main className="flex-1">
                <section className="w-full py-12 md:py-24 lg:py-32 xl:py-48 flex items-center justify-center">
                    <div className="container px-4 md:px-6">
                        <div className="flex flex-col items-center space-y-4 text-center">
                            <div className="space-y-2">
                                <h1 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl lg:text-6xl/none">
                                    Faturalarınızı Yapay Zeka ile Yönetin
                                </h1>
                                <p className="mx-auto max-w-[700px] text-muted-foreground md:text-xl pt-4">
                                    Gelişmiş OCR ve LLM teknolojileriyle faturalarınızdan verileri otomatik olarak çekin, kategorize edin ve doğrulayın. İş süreçlerinizi hızlandırın.
                                </p>
                            </div>
                            <PublicHeroButtons />
                        </div>
                    </div>
                </section>
            </main>
            <footer className="flex flex-col gap-2 sm:flex-row py-6 w-full shrink-0 items-center px-4 md:px-6 border-t">
                <p className="text-xs text-muted-foreground">© 2026 Fatura OCR. Tüm Hakları Saklıdır.</p>
                <nav className="sm:ml-auto flex gap-4 sm:gap-6">
                    <Link className="text-xs hover:underline underline-offset-4 text-muted-foreground" href="#">
                        Kullanım Şartları
                    </Link>
                    <Link className="text-xs hover:underline underline-offset-4 text-muted-foreground" href="#">
                        Gizlilik Politikası
                    </Link>
                </nav>
            </footer>
        </div>
    );
}
