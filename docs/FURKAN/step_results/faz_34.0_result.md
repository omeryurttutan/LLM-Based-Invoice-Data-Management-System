# Faz 34.0: Frontend Internationalization (i18n)

## Genel Bakış
Bu fazda, Fatura OCR frontend uygulaması için kapsamlı bir uluslararasılaştırma (i18n) altyapısı kurulmuş ve mevcut bileşenler bu altyapıyı kullanacak şekilde refaktör edilmiştir. Uygulama artık Türkçe (`tr`) ve İngilizce (`en`) dillerini desteklemekte olup, varsayılan dil Türkçe olarak ayarlanmıştır.

## Yapılan Değişiklikler

### 1. Temel Altyapı Kurulumu
- **Kütüphane:** `next-intl` entegrasyonu tamamlandı.
- **Middleware:** `src/middleware.ts` yapılandırılarak cookie tabanlı dil desteği sağlandı (`localePrefix: 'never'`).
- **Request Config:** `src/i18n.ts` ile sunucu taraflı mesaj yükleme mekanizması kuruldu.
- **Provider:** `src/app/layout.tsx` dosyasına `NextIntlClientProvider` eklendi.

### 2. Çeviri Yönetimi
- **Dosya Yapısı:** `src/messages/tr` ve `src/messages/en` dizinleri altında modüler JSON dosyaları oluşturuldu:
  - `common.json`, `dashboard.json`, `invoices.json`, `auth.json`, vb.
- **Senkronizasyon:** `scripts/check-i18n.js` scripti yazılarak TR ve EN dosyaları arasındaki eksik anahtarların tespiti sağlandı.

### 3. Bileşen Refaktör İşlemleri
Aşağıdaki bileşenler hardcoded metinlerden arındırıldı ve `useTranslations`, `useFormatter` hook'larını kullanacak şekilde güncellendi:

#### Dashboard
- `SummaryCards` (Para birimi formatlama düzeltildi)
- `MonthlyTrendChart` (Grafik başlıkları ve legend çevrildi)
- `CategoryDistributionChart`
- `TopSuppliersChart`
- `StatusTimelineChart` (Tarih formatlama düzeltildi)
- `PendingActionsList`
- `ExtractionPerformanceCard` (Admin paneli)
- `SystemHealthPanel` (Admin paneli)

#### Faturalar
- `InvoicesPage` (Tablo başlıkları, durum etiketleri, tarih/tutar formatlama)
- `FilterPanel` (Filtre başlıkları ve işlem butonları)
- `ExportDialog` (Onaylanmış çeviri kullanımı)

### 4. Dil Değiştirme
- `LanguageSwitcher` bileşeni `src/components/layout/header.tsx` içerisine entegre edildi. Kullanıcılar bayrak ikonları ile dil değişimi yapabilmektedir.

## Doğrulama
- Tüm dashboard ve fatura listesi bileşenlerinin seçilen dile göre dinamik olarak güncellendiği doğrulandı.
- `api-client.ts` içindeki hata mesajlarının çeviri anahtarları ile eşleştiği kontrol edildi.
- Tarih ve para birimi formatlarının (`TL`/`USD`, `GG.AA.YYYY`/`MM/DD/YYYY`) yerel ayarlara uygunluğu sağlandı.

## Sonuç
Frontend uygulaması artık çoklu dil desteğine tam uyumlu hale getirilmiştir. Yeni eklenecek özellikler için `src/messages/` altındaki ilgili dosyalara yeni anahtarlar eklenerek ilerlenebilir.
