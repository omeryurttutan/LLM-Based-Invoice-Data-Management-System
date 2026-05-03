# Kimlik Doğrulama ve Altyapı Sorun Giderme Raporu

Bu rapor, Fatura OCR projesinde karşılaşılan kimlik doğrulama (authentication) ve sistem altyapısı (infrastructure) sorunlarını, bu sorunların kök nedenlerini ve uygulanan çözümleri detaylandırmak amacıyla hazırlanmıştır.

## 1. Veritabanı ve Şifreleme Sorunları

### 1.1. Şifre Hash Uyuşmazlığı
*   **Sorun:** Sistemde kayıtlı varsayılan kullanıcılar (admin@demo.com vb.) doğru şifre girilmesine rağmen "Geçersiz e-posta veya şifre" hatası veriyordu.
*   **Sebep:** `V3__insert_default_data.sql` ve `V41__insert_test_users.sql` dosyalarındaki Bcrypt hash değerleri, beklenen `Admin123!` şifresi ile üretilmemişti.
*   **Çözüm:** Uygulamanın kendi `BCryptPasswordEncoder` mekanizması kullanılarak `Admin123!` için yeni bir hash üretildi ve tüm SQL migration dosyaları güncellendi. `flyway:repair` komutu ile veritabanı şifre uyumluluğu sağlandı.

### 1.2. PostgreSQL İleri Veri Tipleri (inet ve jsonb)
*   **Sorun:** Kullanıcı giriş yaptığında sistem "Audit Log" (Denetim Kaydı) tutmaya çalışırken 500 hatası veriyordu.
*   **Sebep:** PostgreSQL üzerindeki `ip_address` (inet) ve `metadata` (jsonb) kolonları, Hibernate tarafından düz metin (varchar) olarak gönderilmeye çalışıldığı için Postgres tarafından reddediliyordu.
*   **Çözüm:** `AuditLogJpaEntity.java` içinde ilgili alanlara `@org.hibernate.annotations.ColumnTransformer(write = "?::inet")` ve `?::jsonb` anotasyonları eklenerek, veritabanı seviyesinde tip dönüşümü (explicit casting) zorunlu kılındı.

---

## 2. Altyapı ve Konfigürasyon Sorunları

### 2.1. Redis ve İstek Sınırlama (Rate Limiting)
*   **Sorun:** Backend, Redis bağlantısı kurulamadığı için servis dışı kalıyordu.
*   **Sebep:** Güvenlik katmanında yer alan `LoginAttemptService`, hatalı giriş denemelerini takip etmek için Redis'e ihtiyaç duyuyordu.
*   **Çözüm:** Proje kök dizinine bir `docker-compose.yml` eklendi. Redis (port 6380) ve RabbitMQ (port 5673) servisleri ayağa kaldırılarak backend'in bu servislerle tam uyumlu çalışması sağlandı.

### 2.2. Port Çakışmaları
*   **Sorun:** Docker üzerindeki PostgreSQL ile yerel sistemdeki PostgreSQL çakışıyordu.
*   **Sebep:** Her iki servis de varsayılan 5432 portunu kullanmaya çalışıyordu.
*   **Çözüm:** Docker servisindeki Postgres portu **5436** olarak değiştirildi. Bu sayede yerel veritabanı bozulmadan, projenin ihtiyaç duyduğu izolasyon sağlanmış oldu.

---

## 3. Frontend-Backend Entegrasyon Sorunları

### 3.1. Eksik Oturum Doğrulama Uç Noktası (/auth/me)
*   **Sorun:** Giriş başarılı olmasına rağmen kullanıcı panel yerine tekrar giriş ekranına atılıyordu (Redirect Loop).
*   **Sebep:** Frontend `AuthProvider` katmanı, her sayfa değişiminde token'ı doğrulamak için `/api/v1/auth/me` adresine istek atıyordu. Backend'de bu endpoint henüz kodlanmamıştı.
*   **Çözüm:** Backend tarafında `AuthController` ve `AuthenticationService` genişletilerek güncel kullanıcı bilgilerini dönen `/me` endpoint'i implement edildi.

### 3.2. Global API Yanıt Sarmalayıcı (ApiResponse Wrapper)
*   **Sorun:** Frontend veriyi API'den çekmesine rağmen "undefined" hataları alıyordu.
*   **Sebep:** Backend tüm yanıtları `{ success: true, data: { ... } }` yapısında gönderirken, frontend doğrudan payload'u bekliyordu.
*   **Çözüm:** `frontend/src/lib/api-client.ts` üzerinde merkezi bir interceptor (önleyici) yazılarak, gelen yanıtlar içindeki `data` katmanı otomatik olarak açıldı (unwrapping).

### 3.3. Çoklu Dil (i18n) Context ve Hook Hataları
*   **Sorun 1:** `NextIntlClientProvider not found` hatası.
*   **Sebep:** `RootLayout` içindeki sağlayıcıya `locale` prop'u geçilmemişti.
*   **Çözüm:** `layout.tsx` dosyasında `NextIntlClientProvider` bileşenine `locale` eklendi.

*   **Sorun 2:** `Failed to call useTranslations... context not found` runtime hatası (Özellikle grafiklerde).
*   **Sebep:** `useTranslations` hook'unun bileşen render body'si içinde (prop olarak) illegal bir şekilde çağrılması.
*   **Çözüm:** `MonthlyTrendChart.tsx` ve `ExtractionPerformanceCard.tsx` gibi bileşenlerde hook'lar bileşen body'sinin en üstüne (top-level) taşındı.

*   **Sorun 3:** Panellerde ham çeviri anahtarlarının (`dashboard.pendingActions.title` gibi) görünmesi.
*   **Sebep:** `dashboard.json` içindeki anahtar isimleri ile bileşenin beklediği isimlerin uyuşmaması.
*   **Çözüm:** `dashboard.json` güncellenerek `actions` bloğu `pendingActions` olarak yeniden adlandırıldı ve eksik alt anahtarlar eklendi.

## 3. Frontend-Backend Entegrasyon Sorunları (Devam)

### 3.4. Dashboard Yetkilendirme ve API İstemcisi Sorunu
*   **Sorun:** Dashboard üzerindeki bazı kartlar (Bekleyen İşlemler, Sistem Durumu vb.) 401 Unauthorized veya 403 Forbidden hatası veriyordu.
*   **Sebep:** `DashboardService` içinde veri çeken fonksiyonlar, `@/services/api.ts` dosyasındaki kimlik doğrulaması (Authorization header) desteği olmayan basit bir axios istemcisini kullanıyordu. Ayrıca yeni oluşturulan kullanıcıların varsayılan rolleri admin yetkisi gerektiren `/admin/**` uç noktalarına erişemiyordu.
*   **Çözüm:** 
    1.  `DashboardService` içindeki tüm içe aktarmalar, `@/lib/api-client.ts` dosyasındaki merkezi ve yetkilendirilmiş `apiClient` ile değiştirildi.
    2.  `SystemStatusService` uç noktası `/api/v1/admin/system/status` şeklinde yetkilendirilmiş bir yapıya bağlandı.

### 3.5. Grafik ve Panel Veri Yapısı Uyuşmazlığı (SystemHealthPanel çökmesi)
*   **Sorun:** Dashboard açıldığında sayfa beyaz ekran veriyor veya `Cannot read properties of undefined (reading 'every')` hatasıyla çöküyordu.
*   **Sebep:** 
    1.  Backend `SystemStatusService` veriyi frontend'in beklediği DTO yapısıyla (`services`, `resources`, `alerts`) değil, ham bir `health` ve `metrics` haritası olarak dönüyordu.
    2.  Frontend tarafında `data.services.every(...)` gibi çağrılarda `data` veya `services` boş geldiğinde koruyucu kontrol (defensive checking) bulunmuyordu.
*   **Çözüm:** 
    1.  Backend `SystemStatusService` sınıfu refaktöri edilerek frontend DTO yapısına tam uyumlu hale getirildi. 
    2.  Frontend `SystemHealthPanel.tsx` dosyasına `optional chaining` ve varsayılan değerler (`data.services || []`) eklenerek çalışma zamanı hataları engellendi.

### 3.6. Eksik Çeviri Anahtarları (statusTimeline vb.)
*   **Sorun:** Dashboard grafiklerinde başlıkların veya etiketlerin eksik olması veya anahtar olarak görünmesi.
*   **Sebep:** `dashboard.json` dosyasında `statusTimeline` ve bazı alt anahtarların tanımlanmamış olması.
*   **Çözüm:** `frontend/src/messages/tr/dashboard.json` dosyası güncellenerek eksik tüm anahtarlar eklendi.

---

## 4. Performans ve Kullanıcı Arayüzü (UI) Sorunları

### 4.1. Hook Seviyesinde Sonsuz Döngüler (Infinite Fetching)
*   **Sorun:** Uygulama açıldığında veya toplu işlem (upload) başlatıldığında sistem donuyor ve "Too many requests" (429) hatası alınıyordu.
*   **Sebep:** `useBatchStatus` ve `useUpload` hook'ları içinde kullanılan `useEffect` blokları, veri her güncellendiğinde (veya yeni bir WebSocket bildirimi geldiğinde) tekrar tetiklenip sonsuz bir `refetch()` döngüsü başlatıyordu.
*   **Çözüm:** `useRef` hook'u kullanılarak `lastProcessedNotificationId` ve `processedBatchId` takibi yapıldı. Bir bildirim veya işlem sonucu zaten işlenmişse, efektin tekrar çalışması engellenerek sonsuz döngü kırıldı.

### 4.2. i18n Çeviri Anahtarlarının Ham Görünmesi
*   **Sorun:** Dashboard ve sidebar üzerinde metinler yerine `dashboard.title`, `navigation.sidebar.dashboard` gibi JSON anahtarları görünüyordu.
*   **Sebep:** `src/i18n.ts` içindeki dinamik `await import` kullanımı, mevcut Next.js/Webpack ortamında dil dosyalarını (dictionary) zamanında veya doğru bir şekilde yükleyemiyordu.
*   **Çözüm:** Dinamik import yapısı, `tr` ve `en` dosyalarının en üstte statik olarak içe aktarıldığı (`static import`) güvenli bir yapıya dönüştürüldü.

### 4.3. Kademeli Rate Limit Hataları ve UI Kilitlenmesi
*   **Sorun:** Dashboard'a girildiğinde 8 parelel istek atıldığı için backend limitlerine takılıp arka arkaya 10-15 tane "Hata" uyarısı (toast) çıkıyor ve tarayıcı kilitleniyordu.
*   **Sebep:** 
    1. React Query başarısız olan 429 (Rate Limit) hatalarını varsayılan olarak bekleyip tekrar deniyordu.
    2. Her hata isteği yeni bir toast bildirimi açıyordu.
*   **Çözüm:** 
    1. `QueryProvider.tsx` üzerinde 429, 401 ve 404 hataları için `retry` mekanizması kapatıldı.
    2. `api-client.ts` üzerindeki rate limit uyarısına sabit bir `id` verilerek, aynı anda 10 hata gelse bile ekranda tek bir uyarının (overwrite) görünmesi sağlandı.

### 4.4. Redirection ve Landing Page Eksikliği
*   **Sorun:** `localhost:3001/` adresine girildiğinde direkt `/dashboard`'a yönlendirme yapılması sistemde takılmalara sebep olabiliyordu ve karşılama sayfası (Landing Page) bulunmuyordu.
*   **Çözüm:** `next.config.js` üzerindeki otomatik yönlendirme kaldırıldı. `src/app/(public)/page.tsx` adresi altına modern ve sade bir Landing Page (Karşılama Sayfası) eklenerek kullanıcının sisteme bilinçli bir şekilde girmesi sağlandı.

---

## Özet ve Tavsiyeler
İleride benzer bir problemle karşılaşıldığında;
1.  Öncelikle Docker servislerinin (`docker-compose ps`) ayakta olduğunu doğrulayın.
2.  Backend loglarında "SQLGrammarException" görülüyorsa veritabanı tip uyumlarını kontrol edin.
3.  Frontend tarafında login döngüsü oluşuyorsa `/auth/me` isteğinin network sekmesinde 200 dönüp dönmediğini kontrol edin.
4.  Çeviri hataları alıyorsanız, hook'ların React kurallarına uygun (en üst seviyede) çağrıldığından emin olun.
5.  Yeni bir panel eklediğinizde `messages/tr/*.json` dosyalarındaki anahtarların bileşendeki `t('key')` kullanımıyla birebir uyuştuğunu teyit edin.
6.  **Sonsuz Döngü Kontrolü:** Eğer bir `useEffect` içinde `refetch` veya state güncellemesi yapıyorsanız, mutlaka işlemi bir `ref` veya dış kontrol mekanizmasıyla limitleyin.
7.  **Rate Limit:** Bir sayfada çok fazla paralel istek atılıyorsa, React Query'nin retry stratejisini dikkatli yapılandırın.
8.  **Kritik:** Yeni bir frontend servisi oluşturduğunuzda mutlaka `@/lib/api-client` (veya interceptor içeren merkezi istemci) kullanıldığından emin olun, aksi halde token gönderilmediği için 401 hatası alırsınız.
