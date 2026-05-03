import Link from "next/link";
import { FileText, ScanText, Brain, ShieldCheck, BarChart3, Zap, Globe, Check, Mail, MapPin, Phone } from "lucide-react";
import { PublicNav, PublicHeroButtons } from "@/components/layout/public-nav";

export default function LandingPage() {
    return (
        <div className="flex flex-col min-h-screen bg-background text-foreground">
            <header className="sticky top-0 z-50 px-6 lg:px-14 h-20 flex items-center border-b bg-background/80 backdrop-blur-md">
                <Link className="flex items-center justify-center font-bold text-2xl gap-2 mr-8" href="/">
                    <FileText className="h-8 w-8 text-primary" />
                    <span>Fatura OCR</span>
                </Link>
                <nav className="hidden md:flex items-center gap-6">
                    <Link className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors" href="#ozellikler">
                        Özellikler
                    </Link>
                    <Link className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors" href="#fiyatlandirma">
                        Fiyatlandırma
                    </Link>
                    <Link className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors" href="#hakkimizda">
                        Hakkımızda
                    </Link>
                    <Link className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors" href="#iletisim">
                        İletişim
                    </Link>
                </nav>
                <div className="ml-auto flex items-center gap-4 sm:gap-6">
                    <PublicNav />
                </div>
            </header>

            <main className="flex-1">
                {/* Hero Section */}
                <section className="w-full py-16 md:py-28 lg:py-36 xl:py-48 flex items-center justify-center">
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

                {/* Özellikler Section */}
                <section id="ozellikler" className="w-full py-16 md:py-24 border-t bg-muted/30">
                    <div className="container px-4 md:px-6 mx-auto">
                        <div className="text-center mb-12">
                            <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl">Özellikler</h2>
                            <p className="mx-auto max-w-[600px] text-muted-foreground mt-3">
                                Fatura OCR platformunun sunduğu güçlü özelliklerle iş akışlarınızı dönüştürün.
                            </p>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 max-w-6xl mx-auto">
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card hover:shadow-lg transition-shadow">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <ScanText className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-2">Akıllı OCR</h3>
                                <p className="text-sm text-muted-foreground">
                                    PDF, JPEG, PNG formatlarındaki faturalarınızı yükleyin, gelişmiş optik karakter tanıma ile tüm verileri otomatik olarak çıkarın.
                                </p>
                            </div>
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card hover:shadow-lg transition-shadow">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <Brain className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-2">LLM Destekli Analiz</h3>
                                <p className="text-sm text-muted-foreground">
                                    Büyük dil modelleri (Gemini, OpenAI) ile fatura verilerini anlayın, eksik alanları tamamlayın ve doğruluk oranını artırın.
                                </p>
                            </div>
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card hover:shadow-lg transition-shadow">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <ShieldCheck className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-2">Otomatik Doğrulama</h3>
                                <p className="text-sm text-muted-foreground">
                                    Çıkarılan fatura verileri otomatik olarak doğrulanır; KDV hesapları, tarih formatları ve tutar kontrolleri anında yapılır.
                                </p>
                            </div>
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card hover:shadow-lg transition-shadow">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <BarChart3 className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-2">Detaylı Dashboard</h3>
                                <p className="text-sm text-muted-foreground">
                                    Fatura istatistiklerini, kategori dağılımlarını, aylık trendleri ve tedarikçi analizlerini tek bir panelde görüntüleyin.
                                </p>
                            </div>
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card hover:shadow-lg transition-shadow">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <Zap className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-2">Hızlı İşlem</h3>
                                <p className="text-sm text-muted-foreground">
                                    Faturalarınız saniyeler içinde işlenir. Toplu yükleme desteğiyle onlarca faturayı aynı anda analiz edin.
                                </p>
                            </div>
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card hover:shadow-lg transition-shadow">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <Globe className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-2">Çoklu Dil Desteği</h3>
                                <p className="text-sm text-muted-foreground">
                                    Türkçe ve İngilizce arayüz desteği ile uluslararası ekiplerle kolayca çalışın. Daha fazla dil yakında eklenecek.
                                </p>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Fiyatlandırma Section */}
                <section id="fiyatlandirma" className="w-full py-16 md:py-24 border-t">
                    <div className="container px-4 md:px-6 mx-auto">
                        <div className="text-center mb-12">
                            <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl">Fiyatlandırma</h2>
                            <p className="mx-auto max-w-[600px] text-muted-foreground mt-3">
                                İşletmenizin ihtiyacına uygun planı seçin. Tüm planlar 14 gün ücretsiz deneme içerir.
                            </p>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
                            {/* Başlangıç */}
                            <div className="flex flex-col p-8 rounded-xl border bg-card">
                                <h3 className="text-xl font-bold">Başlangıç</h3>
                                <p className="text-sm text-muted-foreground mt-1">Küçük işletmeler için</p>
                                <div className="mt-4 mb-6">
                                    <span className="text-4xl font-bold">₺199</span>
                                    <span className="text-muted-foreground">/ay</span>
                                </div>
                                <ul className="space-y-3 flex-1">
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Aylık 100 fatura işleme
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Temel OCR çıkarımı
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> E-posta desteği
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> 1 kullanıcı
                                    </li>
                                </ul>
                            </div>
                            {/* Profesyonel */}
                            <div className="flex flex-col p-8 rounded-xl border-2 border-primary bg-card relative">
                                <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-primary text-primary-foreground text-xs font-semibold px-3 py-1 rounded-full">
                                    Popüler
                                </div>
                                <h3 className="text-xl font-bold">Profesyonel</h3>
                                <p className="text-sm text-muted-foreground mt-1">Büyüyen işletmeler için</p>
                                <div className="mt-4 mb-6">
                                    <span className="text-4xl font-bold">₺499</span>
                                    <span className="text-muted-foreground">/ay</span>
                                </div>
                                <ul className="space-y-3 flex-1">
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Aylık 1.000 fatura işleme
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> LLM destekli akıllı analiz
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Öncelikli destek
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> 5 kullanıcı
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Dashboard ve raporlama
                                    </li>
                                </ul>
                            </div>
                            {/* Kurumsal */}
                            <div className="flex flex-col p-8 rounded-xl border bg-card">
                                <h3 className="text-xl font-bold">Kurumsal</h3>
                                <p className="text-sm text-muted-foreground mt-1">Büyük ölçekli şirketler için</p>
                                <div className="mt-4 mb-6">
                                    <span className="text-4xl font-bold">₺999</span>
                                    <span className="text-muted-foreground">/ay</span>
                                </div>
                                <ul className="space-y-3 flex-1">
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Sınırsız fatura işleme
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Tüm LLM modelleri
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> 7/24 destek
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Sınırsız kullanıcı
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> API erişimi
                                    </li>
                                    <li className="flex items-center gap-2 text-sm">
                                        <Check className="h-4 w-4 text-green-500 shrink-0" /> Özel entegrasyon
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Hakkımızda Section */}
                <section id="hakkimizda" className="w-full py-16 md:py-24 border-t bg-muted/30">
                    <div className="container px-4 md:px-6 mx-auto">
                        <div className="text-center mb-12">
                            <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl">Hakkımızda</h2>
                            <p className="mx-auto max-w-[600px] text-muted-foreground mt-3">
                                Fatura OCR, işletmelerin fatura yönetim süreçlerini dijitalleştirmek için kurulmuş bir teknoloji girişimidir.
                            </p>
                        </div>
                        <div className="max-w-3xl mx-auto space-y-6 text-center">
                            <p className="text-muted-foreground leading-relaxed">
                                Ekibimiz, yapay zeka, makine öğrenimi ve bulut teknolojileri alanlarında uzmanlaşmış mühendislerden oluşmaktadır.
                                Amacımız, geleneksel fatura işleme süreçlerini ortadan kaldırarak şirketlerin zamandan ve maliyetten tasarruf etmesini sağlamaktır.
                            </p>
                            <p className="text-muted-foreground leading-relaxed">
                                Platformumuz, en son OCR ve büyük dil modeli teknolojilerini kullanarak faturalardan veri çıkarma, doğrulama
                                ve kategorizasyon işlemlerini tam otomatik hale getirmektedir. Türkiye&apos;deki küçük ve orta ölçekli işletmelerden
                                büyük kurumsal şirketlere kadar geniş bir müşteri yelpazesine hizmet vermekteyiz.
                            </p>
                            <div className="grid grid-cols-3 gap-8 pt-8">
                                <div>
                                    <div className="text-3xl font-bold text-primary">10K+</div>
                                    <p className="text-sm text-muted-foreground mt-1">İşlenen Fatura</p>
                                </div>
                                <div>
                                    <div className="text-3xl font-bold text-primary">%99.2</div>
                                    <p className="text-sm text-muted-foreground mt-1">Doğruluk Oranı</p>
                                </div>
                                <div>
                                    <div className="text-3xl font-bold text-primary">500+</div>
                                    <p className="text-sm text-muted-foreground mt-1">Aktif Kullanıcı</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                {/* İletişim Section */}
                <section id="iletisim" className="w-full py-16 md:py-24 border-t">
                    <div className="container px-4 md:px-6 mx-auto">
                        <div className="text-center mb-12">
                            <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl">İletişim</h2>
                            <p className="mx-auto max-w-[600px] text-muted-foreground mt-3">
                                Sorularınız mı var? Bizimle iletişime geçin, size yardımcı olalım.
                            </p>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl mx-auto">
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <Mail className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-1">E-posta</h3>
                                <p className="text-sm text-muted-foreground">destek@faturaocr.com</p>
                            </div>
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <Phone className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-1">Telefon</h3>
                                <p className="text-sm text-muted-foreground">+90 (212) 555 00 00</p>
                            </div>
                            <div className="flex flex-col items-center text-center p-6 rounded-xl border bg-card">
                                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center mb-4">
                                    <MapPin className="h-6 w-6 text-primary" />
                                </div>
                                <h3 className="text-lg font-semibold mb-1">Adres</h3>
                                <p className="text-sm text-muted-foreground">İstanbul, Türkiye</p>
                            </div>
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
