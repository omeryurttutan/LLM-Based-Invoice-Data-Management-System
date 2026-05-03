# Oturum Raporu — 9 Mart 2026

Bu belge, bu oturum boyunca karşılaşılan sorunları, eklenen özellikleri, yapılan değişiklikleri ve uygulanan çözümleri detaylı olarak kapsamaktadır.

---

## İçindekiler

1. [Multi-Tenant (Çoklu Şirket) Mimarisi](#1-multi-tenant-çoklu-şirket-mimarisi)
2. [Güvenlik İyileştirmeleri](#2-güvenlik-i̇yileştirmeleri)
3. [Sorun: Login JDBC Exception](#3-sorun-login-jdbc-exception)
4. [Sorun: Backend Başlatma Hatası (Encryption Key)](#4-sorun-backend-başlatma-hatası-encryption-key)
5. [Sorun: 401 Unauthorized Hatası](#5-sorun-401-unauthorized-hatası)
6. [Sorun: Login Sonrası Yanlış Sayfa Yönlendirmesi](#6-sorun-login-sonrası-yanlış-sayfa-yönlendirmesi)
7. [Sorun: Fatura Yükleme Başarısızlığı](#7-sorun-fatura-yükleme-başarısızlığı)
8. [Sorun: Dashboard Verilerinin Gözükmemesi](#8-sorun-dashboard-verilerinin-gözükmemesi)
9. [Sorun: CORS X-Company-Id Header Eksikliği](#9-sorun-cors-x-company-id-header-eksikliği)
10. [Sorun: Extraction Service Health Check Port Uyumsuzluğu](#10-sorun-extraction-service-health-check-port-uyumsuzluğu)
11. [Sorun: Backend-Extraction JSON Alan Adı Uyumsuzluğu (data vs invoice_data)](#11-sorun-backend-extraction-json-alan-adı-uyumsuzluğu-data-vs-invoice_data)
12. [Sorun: LLM Yanıtı JSON Parse Hatası](#12-sorun-llm-yanıtı-json-parse-hatası)
13. [Sorun: Multipart Boundary Eksikliği (400 Bad Request)](#13-sorun-multipart-boundary-eksikliği-400-bad-request)
14. [Sorun: Extraction Verilerinin Veritabanına Null Olarak Kaydedilmesi](#14-sorun-extraction-verilerinin-veritabanına-null-olarak-kaydedilmesi)
15. [Değiştirilen Dosyaların Listesi](#15-değiştirilen-dosyaların-listesi)

---

## 1. Multi-Tenant (Çoklu Şirket) Mimarisi

### Amaç
Bir muhasebecinin farklı şirketlere ait faturaları birbirine karıştırmadan yönetebilmesi için çoklu şirket (multi-tenant) desteği eklenmesi.

### Yapılan Değişiklikler

#### Backend
- **`UserCompanyAccess` Entity:** Kullanıcı-Şirket arasında M:N (çoğa-çok) ilişki kurulması için yeni ara tablo oluşturuldu.
- **`User` Entity:** Tekil `companyId` yerine `List<UserCompanyAccess>` ilişkisi eklendi.
- **`CompanyContextFilter`:** HTTP isteklerinden `X-Company-Id` header'ını okuyarak aktif şirketi belirleyen filtre yazıldı.
- **`CompanyService`:** Yeni şirket oluşturma işlemi güncellendi; oluşturan kullanıcıya otomatik olarak erişim yetkisi verildi.
- **`UserRepositoryAdapter`:** JPA entity mapping'de `companyAccesses` koleksiyonunun doğru şekilde persist edilmesi sağlandı.
- **`CompanyController`:** Şirket oluşturma yetkisi `ADMIN`'den `ADMIN + ACCOUNTANT` olarak genişletildi.

#### Frontend
- **Şirket Seçici (Company Switcher):** Dashboard header'ına şirketler arası geçiş yapabilen dropdown eklendi.
- **Müşteri Ekleme Diyaloğu:** `CreateCompanyDialog` bileşeni ile yeni müşteri (şirket) oluşturma arayüzü eklendi.
- **Kullanıcı Yönetimi:** Şirket içi kullanıcı davet etme arayüzü eklendi.
- **`api-client.ts`:** Her istekte `X-Company-Id` header'ının otomatik eklenmesi sağlandı.

---

## 2. Güvenlik İyileştirmeleri

### Amaç
Veri izolasyonunun sağlanması, yetkisiz erişimin engellenmesi ve IDOR/BOLA saldırılarının önlenmesi.

### Yapılan Değişiklikler

- **`CompanyContextFilter`:** Yetkisiz `X-Company-Id` header'ı gönderildiğinde `403 Forbidden` döndürülmesi sağlandı.
- **`CompanyService`:** `updateCompany` metodunda aktif şirket kimliği doğrulaması eklendi.
- **`UserManagementService`:** Yeni oluşturulan alt kullanıcılar için cross-reference mapping hatası düzeltildi.
- **`SecurityConfig`:** Endpoint yetkilendirmeleri `ADMIN` → `ADMIN + ACCOUNTANT + MANAGER` olarak genişletildi.
- **Frontend `nav-config.ts`:** Sidebar menü erişim rolleri güncellendi.

---

## 3. Sorun: Login JDBC Exception

### Belirti
Giriş yapıldığında `500 Internal Server Error` ve ekranda veritabanı hata kodu görüntülendi.

### Kök Neden
`V44__phase_44_multi_tenant_user_company.sql` migration dosyasında `user_company_access` tablosu oluşturulurken `is_deleted` ve `deleted_at` sütunları eksik bırakılmıştı. `BaseJpaEntity` sınıfı bu alanları zorunlu kılıyordu ve Hibernate sorgu sırasında bu sütunları aradığında PostgreSQL hatası fırlatıyordu.

### Çözüm
Yeni bir migration dosyası oluşturuldu:

**Dosya:** `V45__add_delete_fields_to_user_company_access.sql`
```sql
ALTER TABLE user_company_access ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE user_company_access ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
```

### Güvenlik Notu
Veritabanı hata mesajlarının frontend'e iletilmesi güvenlik açığı oluşturuyordu. Bu düzeltme ile hata oluşması engellendi.

---

## 4. Sorun: Backend Başlatma Hatası (Encryption Key)

### Belirti
Spring Boot uygulaması başlatılırken `IllegalStateException` hatası ile crash oluyordu.

### Kök Neden
`EncryptionService`, KVKK (Kişisel Verilerin Korunması Kanunu) uyumluluğu için Base64 formatında 256-bit (32 byte) bir şifreleme anahtarı gerektiriyordu. `KVKK_ENCRYPTION_KEY` ortam değişkeni tanımlı değildi veya geçersiz formattaydı.

### Çözüm
Geçerli bir anahtar `openssl rand -base64 32` komutu ile oluşturulup ortam değişkeni olarak ayarlandı:
```bash
export KVKK_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

### Dikkat Edilmesi Gerekenler
- Üretim ortamında bu anahtar güvenli bir şekilde (örn. vault, secrets manager) saklanmalıdır.
- Anahtar değiştiğinde daha önce şifrelenmiş veriler okunamaz hale gelir.

---

## 5. Sorun: 401 Unauthorized Hatası

### Belirti
Frontend'den `/api/v1/auth/login` endpoint'ine giriş isteği gönderildiğinde `401 UNAUTHORIZED` yanıtı alınıyordu.

### Kök Neden
`SecurityConfig.java` dosyasında public (kimlik doğrulama gerektirmeyen) endpoint'ler arasında login yolu yanlış tanımlanmıştı:
- **Yanlış:** `/api/v1/users/login`
- **Doğru:** `/api/v1/auth/login`

Frontend'in `auth-service.ts` ve `endpoints.ts` dosyaları `/auth/login` yolunu kullanıyordu, ancak backend güvenlik yapılandırması bunu whitelist'e almamıştı.

### Çözüm
`SecurityConfig.java` dosyasında public endpoint düzeltildi:
```java
// Önceki (yanlış)
.requestMatchers("/api/v1/users/login").permitAll()
// Sonraki (doğru)
.requestMatchers("/api/v1/auth/login").permitAll()
```

---

## 6. Sorun: Login Sonrası Yanlış Sayfa Yönlendirmesi

### Belirti
Kullanıcı başarıyla giriş yaptıktan sonra landing page'e (`/`) yönlendiriliyordu. Oysa doğrudan dashboard'a (`/dashboard`) gitmesi gerekiyordu.

### Kök Neden
`frontend/src/app/(auth)/login/page.tsx` dosyasında varsayılan yönlendirme URL'si `/` olarak ayarlanmıştı:
```typescript
const redirectTo = searchParams.get('redirect') || '/';
```

### Çözüm
Varsayılan yönlendirme hedefi `/dashboard` olarak değiştirildi:
```typescript
const redirectTo = searchParams.get('redirect') || '/dashboard';
```

**Değiştirilen Dosya:** `frontend/src/app/(auth)/login/page.tsx`

---

## 7. Sorun: Fatura Yükleme Başarısızlığı

### Belirti
Fatura yüklendiğinde "Yükleme başarısız oldu" mesajı görüntülendi. Backend `INTERNAL_ERROR: Extraction failed` yanıtı döndü.

### Kök Neden — İki Ayrı Sorun

#### Sorun 7a: Yanlış Extraction Service URI
`PythonExtractionClient.java` dosyasında extraction servise gönderilen endpoint yanlıştı:
- **Yanlış:** `/extract`
- **Doğru:** `/api/v1/extraction/extract`

FastAPI uygulamasında router `app.include_router(extraction.router, prefix="/api/v1/extraction")` olarak mount edilmişti, yani endpoint'in tam yolu `/api/v1/extraction/extract` idi.

**Hata:** `400 Bad Request: "Invalid HTTP request received."`

#### Sorun 7b: Host Çözümleme Hatası
`PythonExtractionClient.java` dosyasında varsayılan URL `http://extraction-service:8000` idi. Bu hostname Docker ağında çözümlenebilir ancak lokal Maven çalıştırmada çözümlenemez.

### Çözüm

**URI Düzeltmesi — `PythonExtractionClient.java`:**
```java
// Önceki
.uri("/extract")
// Sonraki
.uri("/api/v1/extraction/extract")
```

**Lokal URL Yapılandırması — `application.yml`:**
```yaml
upload:
  extraction-service-url: ${EXTRACTION_SERVICE_URL:http://localhost:8000}
```

**Değiştirilen Dosyalar:**
- `backend/src/main/java/com/faturaocr/infrastructure/adapter/extraction/PythonExtractionClient.java`
- `backend/src/main/resources/application.yml`

---

## 8. Sorun: Dashboard Verilerinin Gözükmemesi

### Belirti
Dashboard sayfasında tüm paneller (KPI kartları, grafikler, tablolar) boş görünüyordu. Veri yüklenmiyordu.

### Kök Neden
`DashboardService` sınıfında `@Cacheable` ile Redis cache kullanılıyordu. `CacheConfig.java` dosyasındaki `ObjectMapper`, `activateDefaultTyping` olmadan yapılandırılmıştı. Bu durumda:

1. İlk çağrıda DTO nesnesi Redis'e JSON olarak kaydedildi (ancak Java class type bilgisi olmadan).
2. Spring DevTools `RestartClassLoader` ile uygulama yeniden başlatıldığında, Redis'teki JSON verisi farklı bir classloader ile deserialize edildi.
3. Jackson, type bilgisi olmadan veriyi `LinkedHashMap` olarak döndürdü.
4. `LinkedHashMap`, `DashboardStatsResponse`'a cast edilemedi → `ClassCastException` fırlatıldı.

**Hata:** `class java.util.LinkedHashMap cannot be cast to class com.faturaocr.application.dashboard.dto.DashboardStatsResponse`

### Çözüm

**1. ObjectMapper Yapılandırması — `CacheConfig.java`:**
```java
objectMapper.activateDefaultTyping(
    objectMapper.getPolymorphicTypeValidator(),
    ObjectMapper.DefaultTyping.NON_FINAL);
```
Bu ayar, Redis'e kaydedilen JSON'a Java class type bilgisi ekler ve doğru sınıfa deserialize edilmesini garanti eder.

**2. Eski Cache Temizliği:**
```bash
docker exec fatura_ocr_redis redis-cli FLUSHALL
```

**Doğrulama:** Düzeltme sonrası `/api/v1/dashboard/stats` endpoint'i 17 fatura ve 12 aylık trend verisi başarıyla döndürdü.

**Değiştirilen Dosya:** `backend/src/main/java/com/faturaocr/infrastructure/config/CacheConfig.java`

---

## 9. Sorun: CORS X-Company-Id Header Eksikliği

### Belirti
Frontend'den fatura yükleme isteği gönderildiğinde tarayıcı `X-Company-Id` header'ını engelliyordu. Şirket bağlamı (company context) backend'e ulaşmıyordu.

### Kök Neden
`SecurityConfig.java` dosyasındaki CORS yapılandırmasında `allowedHeaders` listesinde `X-Company-Id` tanımlı değildi. Tarayıcı bu header'ı preflight (OPTIONS) kontrolünde reddediyordu.

### Çözüm
`SecurityConfig.java` dosyasındaki CORS `allowedHeaders` listesine `X-Company-Id` eklendi:
```java
configuration.setAllowedHeaders(List.of(
    "Authorization", "Content-Type", "Accept",
    "X-Requested-With", "X-Internal-API-Key", "X-Company-Id"
));
```

**Değiştirilen Dosya:** `backend/src/main/java/com/faturaocr/infrastructure/common/config/SecurityConfig.java`

---

## 10. Sorun: Extraction Service Health Check Port Uyumsuzluğu

### Belirti
Backend health endpoint'i extraction service durumunu `DOWN` olarak raporluyordu, ancak extraction service aslında çalışıyordu.

### Kök Neden
`ExtractionServiceHealthIndicator.java` dosyasındaki varsayılan URL `http://localhost:8001` olarak tanımlıydı, ancak extraction service `http://localhost:8000` portunda çalışıyordu (`application.yml`'daki yapılandırmaya uygun olarak).

### Çözüm
Varsayılan URL düzeltildi:
```java
// Önceki
@Value("${upload.extraction-service-url:http://localhost:8001}")
// Sonraki
@Value("${upload.extraction-service-url:http://localhost:8000}")
```

**Değiştirilen Dosya:** `backend/src/main/java/com/faturaocr/infrastructure/monitoring/ExtractionServiceHealthIndicator.java`

---

## 11. Sorun: Backend-Extraction JSON Alan Adı Uyumsuzluğu (data vs invoice_data)

### Belirti
Extraction service başarılı yanıt döndürse bile backend'de deserialization hatası oluşuyordu.

### Kök Neden
Python extraction service yanıtında çıkarılan fatura verisi `"data"` anahtarı altında döndürülüyordu. Ancak Java backend'deki `ExtractionResult.java` DTO'su `"invoice_data"` anahtarını bekliyordu:
```java
// Önceki (yanlış)
@JsonProperty("invoice_data")
private InvoiceData invoiceData;
```

### Çözüm
`@JsonProperty` anotasyonu düzeltildi:
```java
// Sonraki (doğru)
@JsonProperty("data")
private InvoiceData invoiceData;
```

**Değiştirilen Dosya:** `backend/src/main/java/com/faturaocr/infrastructure/adapter/extraction/dto/ExtractionResult.java`

---

## 12. Sorun: LLM Yanıtı JSON Parse Hatası

### Belirti
Gemini LLM başarılı yanıt döndürüyordu ancak extraction service `"Failed to parse JSON from LLM response"` hatası veriyordu. İki ayrı alt sorun tespit edildi.

### Kök Neden — Sorun 12a: Bozuk JSON Yapısı
Gemini modeli bazen bozuk JSON üretiyordu:
- Dizi elemanlarında kapanış süslü parantezi (`}`) eksik kalıyordu: `118.00\n    ,\n    {` yerine `118.00\n    },\n    {` olmalıydı
- `Expecting property name enclosed in double quotes: line 20 column 5` hatası oluşuyordu

### Kök Neden — Sorun 12b: Kesilmiş Yanıt (Truncated Output)
`max_output_tokens` değeri 4096 olarak ayarlıydı. 10 kalemli faturaların JSON çıktısı bu limiti aşıyordu ve yanıt kesiliyordu:
```json
"total_amount": 3295.
```
JSON eksik kalıyordu, kapanış parantezleri ve son alanlar (`currency`, `notes`) yoktu.

### Çözüm

**1. ResponseParser JSON Onarım Mekanizması — `response_parser.py`:**
`_clean_text()` metoduna üç aşamalı JSON onarımı eklendi:
```python
# 1. Eksik kapanış parantezi düzeltme (dizi elemanlarında)
text = re.sub(r'(\d+\.?\d*)\s*\n(\s*),\s*\n(\s*)\{', r'\1\n\2},\n\3{', text)

# 2. Kesilmiş ondalık sayı düzeltme ("3295." → "3295.0")
text = re.sub(r'(\d+)\.$', r'\g<1>.0', text)

# 3. Kapanmamış parantezleri otomatik kapatma
open_braces = text.count('{') - text.count('}')
open_brackets = text.count('[') - text.count(']')
text += ']' * open_brackets + '}' * open_braces
```

**2. Max Output Tokens Artırımı — `settings.py`:**
```python
# Önceki
GEMINI_MAX_OUTPUT_TOKENS: int = 4096
GEMINI_2_5_FLASH_MAX_OUTPUT_TOKENS: int = 4096
# Sonraki
GEMINI_MAX_OUTPUT_TOKENS: int = 8192
GEMINI_2_5_FLASH_MAX_OUTPUT_TOKENS: int = 8192
```

**Değiştirilen Dosyalar:**
- `extraction-service/app/services/llm/response_parser.py`
- `extraction-service/app/config/settings.py`

### Doğrulama
Düzeltme sonrası `bos-fatura-ornegi_x.png` dosyası başarıyla işlendi:
- Fatura No: A2, Satıcı: Logo, Alıcı: Şule Kaplan
- 10 kalem, Ara Toplam: ₺2.793, KDV: ₺502,74, Genel Toplam: ₺3.295,74
- Güven Skoru: %77,3, Provider: GEMINI_3_FLASH

---

## 13. Sorun: Multipart Boundary Eksikliği (400 Bad Request)

### Belirti
Extraction service doğrudan `curl` ile çağrıldığında başarılı çalışıyordu, ancak frontend üzerinden fatura yüklendiğinde:
```
400 Bad Request: "Invalid HTTP request received."
```
hatası oluşuyordu.

### Kök Neden
`PythonExtractionClient.java` dosyasında Spring `RestClient` + `MultipartBodyBuilder` kombinasyonu kullanılıyordu. Bu kombinasyonun iki sorunu vardı:

1. **Multipart Boundary Eksikliği:** `contentType(MediaType.MULTIPART_FORM_DATA)` açıkça ayarlandığında, Spring `Content-Type` header'ına multipart `boundary` parametresini eklemiyordu. Uvicorn/Starlette sunucusu boundary olmadan multipart body'yi parse edemiyordu.

2. **Varsayılan URL Sorunu:** Varsayılan URL `http://extraction-service:8000` idi (Docker hostname), lokal geliştirmede çözümlenemiyordu.

### Çözüm
`PythonExtractionClient.java` tamamen yeniden yazıldı. `RestClient` + `MultipartBodyBuilder` yerine `RestTemplate` + `LinkedMultiValueMap` kullanıldı:

```java
// Önceki (bozuk)
MultipartBodyBuilder builder = new MultipartBodyBuilder();
builder.part("file", new FileSystemResource(filePath));
return restClient.post()
    .contentType(MediaType.MULTIPART_FORM_DATA)
    .body(builder.build())
    .retrieve()
    .body(ExtractionResult.class);

// Sonraki (çalışan)
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.MULTIPART_FORM_DATA);
MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", new FileSystemResource(filePath));
HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
ResponseEntity<ExtractionResult> response = restTemplate.postForEntity(
    extractionServiceUrl + "/api/v1/extraction/extract",
    requestEntity, ExtractionResult.class);
```

`RestTemplate`, `LinkedMultiValueMap` body'si ile birlikte `Content-Type` header'ına otomatik olarak doğru `boundary` parametresini ekler.

**Değiştirilen Dosya:** `backend/src/main/java/com/faturaocr/infrastructure/adapter/extraction/PythonExtractionClient.java`

---

## 14. Sorun: Extraction Verilerinin Veritabanına Null Olarak Kaydedilmesi

### Belirti
Fatura başarıyla yükleniyor ve extraction service başarıyla JSON yanıt döndürüyordu, ancak faturalar listesinde fatura numarası, tedarikçi adı, tutarlar gibi tüm alanlar boş görünüyordu.

### Kök Neden — Pydantic Alias Uyumsuzluğu
Python extraction service'teki `InvoiceData` Pydantic modeli Türkçe alias'lar kullanıyordu ve FastAPI varsayılan olarak `by_alias=True` ile serialize ediyordu:

| Extraction Service Döndürüyor | Java DTO Bekliyordu | Eşleşme |
|---|---|---|
| `fatura_no` | `invoice_number` | ❌ |
| `tarih` | `invoice_date` | ❌ |
| `gonderici_unvan` | `supplier_name` | ❌ |
| `gonderici_vkn` | `supplier_tax_id` | ❌ |
| `gonderici_adres` | `supplier_address` | ❌ |
| `genel_toplam` | `total_amount` | ❌ |
| `vergi_toplam` | `tax_amount` | ❌ |
| `para_birimi` | `currency` | ❌ |
| `kalemler` | `items` | ❌ |

Tüm `@JsonProperty` anotasyonları yanlış alan adlarını kullandığı için Jackson deserializer hiçbir alanı eşleştiremiyordu ve tüm değerler `null` olarak kalıyordu.

### Çözüm

**1. ExtractionResult.java — Tüm Anotasyonlar Düzeltildi:**
```java
// Önceki (yanlış İngilizce alan adları)
@JsonProperty("invoice_number") private String invoiceNumber;
@JsonProperty("invoice_date") private LocalDate invoiceDate;
@JsonProperty("supplier_name") private String supplierName;

// Sonraki (doğru Türkçe alias'lar)
@JsonProperty("fatura_no") private String invoiceNumber;
@JsonProperty("tarih") private LocalDate invoiceDate;
@JsonProperty("gonderici_unvan") private String supplierName;
```

Eklenen eksik alanlar: `buyerName` (`alici_unvan`), `buyerTaxNumber` (`alici_vkn`), `subtotal` (`ara_toplam`), `notes` (`notlar`), `unit` (`birim`).

**2. InvoiceUploadService.java — Eksik Mapping'ler Eklendi:**
Önceden sadece 8 alan map ediliyordu. Düzeltme sonrası:
- `currency` → `Currency.valueOf()` ile enum'a dönüştürme
- `subtotal` → ara toplam
- `buyerTaxNumber` → alıcı VKN
- `notes` → fatura notları
- **Fatura kalemleri (items)** → Her kalem için `InvoiceItem` entity oluşturularak `invoice.addItem()` ile eklendi (açıklama, miktar, birim fiyat, KDV oranı, KDV tutarı, satır toplamı)

**Değiştirilen Dosyalar:**
- `backend/src/main/java/com/faturaocr/infrastructure/adapter/extraction/dto/ExtractionResult.java`
- `backend/src/main/java/com/faturaocr/application/invoice/service/InvoiceUploadService.java`

---

## 15. Değiştirilen Dosyaların Listesi

### Önceki Oturumdan (Bölüm 1-8)

| # | Dosya | Değişiklik Türü | Açıklama |
|---|-------|----------------|----------|
| 1 | `backend/src/main/resources/db/migration/V45__add_delete_fields_to_user_company_access.sql` | **YENİ** | Eksik `is_deleted` ve `deleted_at` sütunlarını ekleyen migration |
| 2 | `backend/src/main/java/com/faturaocr/infrastructure/common/config/SecurityConfig.java` | Düzenleme | Login endpoint whitelist düzeltmesi |
| 3 | `frontend/src/app/(auth)/login/page.tsx` | Düzenleme | Varsayılan yönlendirme `/` → `/dashboard` |
| 4 | `backend/src/main/java/com/faturaocr/infrastructure/adapter/extraction/PythonExtractionClient.java` | Düzenleme | Extraction URI `/extract` → `/api/v1/extraction/extract` |
| 5 | `backend/src/main/resources/application.yml` | Düzenleme | `extraction-service-url` lokal yapılandırması eklendi |
| 6 | `backend/src/main/java/com/faturaocr/infrastructure/config/CacheConfig.java` | Düzenleme | `activateDefaultTyping` ile cache seri hale getirme düzeltmesi |

### Bu Oturumdan (Bölüm 9-14)

| # | Dosya | Değişiklik Türü | Açıklama |
|---|-------|----------------|----------|
| 7 | `backend/src/main/java/com/faturaocr/infrastructure/common/config/SecurityConfig.java` | Düzenleme | CORS `allowedHeaders`'a `X-Company-Id` eklendi |
| 8 | `backend/src/main/java/com/faturaocr/infrastructure/monitoring/ExtractionServiceHealthIndicator.java` | Düzenleme | Varsayılan health check portu `8001` → `8000` düzeltildi |
| 9 | `backend/src/main/java/com/faturaocr/infrastructure/adapter/extraction/dto/ExtractionResult.java` | Düzenleme | `@JsonProperty` anotasyonları Türkçe Pydantic alias'larına güncellendi; `buyerName`, `subtotal`, `notes` eklendi |
| 10 | `backend/src/main/java/com/faturaocr/infrastructure/adapter/extraction/PythonExtractionClient.java` | Yeniden Yazıldı | `RestClient` → `RestTemplate` ile multipart boundary düzeltmesi; varsayılan URL `localhost:8000` yapıldı |
| 11 | `backend/src/main/java/com/faturaocr/application/invoice/service/InvoiceUploadService.java` | Düzenleme | `currency`, `subtotal`, `buyerTaxNumber`, `notes` ve fatura kalemlerinin (items) mapping'i eklendi |
| 12 | `extraction-service/app/services/llm/response_parser.py` | Düzenleme | JSON onarım mekanizması eklendi (eksik `}`, kesilmiş sayılar, kapanmamış parantezler) |
| 13 | `extraction-service/app/config/settings.py` | Düzenleme | `GEMINI_MAX_OUTPUT_TOKENS` ve `GEMINI_2_5_FLASH_MAX_OUTPUT_TOKENS` `4096` → `8192` artırıldı |

---

## Alınan Dersler ve Öneriler (9 Mart)

1. **Migration Dosyaları:** Yeni entity'ler oluşturulurken `BaseJpaEntity`'den miras alınan tüm alanların (`is_deleted`, `deleted_at`, `created_at`, `updated_at`) migration'a dahil edildiğinden emin olunmalıdır.
2. **Ortam Değişkenleri:** Hassas yapılandırmalar (şifreleme anahtarları, API anahtarları) ortam değişkenleri ile yönetilmeli ve `.env.example` dosyasında belgelenmeli.
3. **Frontend-Backend Uyumu:** Endpoint tanımları (`SecurityConfig`, `Controller`, Frontend `endpoints.ts`) arasında tutarlılık sağlanmalıdır.
4. **Redis Cache:** `activateDefaultTyping` kullanılarak cache'deki verilerin doğru Java sınıfına deserialize edilmesi garanti altına alınmalıdır. DevTools ortamında eski cache'ler sorun yaratabilir.
5. **Docker vs Lokal Geliştirme:** Servislerin Docker hostname'leri (ör. `extraction-service:8000`) lokal geliştirmede çözümlenemez. `application.yml`'da `localhost` fallback'leri tanımlanmalıdır.
6. **Hata Mesajları:** Veritabanı veya sistem hataları asla frontend'e iletilmemelidir. Global exception handler aracılığıyla standart hata formatları kullanılmalıdır.
7. **Pydantic Alias Uyumu:** Python Pydantic modelleri alias kullanıyorsa, FastAPI varsayılan olarak `by_alias=True` ile serialize eder. Java backend DTO'larındaki `@JsonProperty` anotasyonlarının bu alias'ları yansıtması gerekir, aksi halde tüm alanlar `null` olarak deserialize edilir.
8. **Spring RestClient vs RestTemplate:** Spring `RestClient` ile `MultipartBodyBuilder` kullanırken multipart `boundary` parametresi `Content-Type` header'ına otomatik eklenmeyebilir. `RestTemplate` + `LinkedMultiValueMap` kombinasyonu multipart dosya yüklemeleri için daha güvenilirdir.
9. **LLM Çıktı Güvenilirliği:** LLM'ler (Gemini dahil) her zaman geçerli JSON üretmez. Yanıt parser'ları eksik parantez, kesilmiş sayılar ve tamamlanmamış JSON gibi bozuk çıktıları onaracak şekilde dayanıklı (resilient) tasarlanmalıdır.
10. **Max Output Tokens:** Çok kalemli faturalar (10+) için `max_output_tokens` değeri yeterli büyüklükte olmalıdır. 4096 token, 10 kalemli bir fatura için yetersiz kalabilir; 8192 önerilir.

---
---

# Oturum Raporu — 12 Mart 2026

Bu belge, 12 Mart 2026 oturumu boyunca karşılaşılan sorunları, eklenen özellikleri, yapılan değişiklikleri ve uygulanan çözümleri detaylı olarak kapsamaktadır.

---

## İçindekiler (12 Mart)

16. [Sorun: Kullanıcı Sayfası Durum Kısmı Bozuk Görünüm](#16-sorun-kullanıcı-sayfası-durum-kısmı-bozuk-görünüm)
17. [Sorun: Admin Rolü Değişikliğinde Yanlış Hata Mesajı](#17-sorun-admin-rolü-değişikliğinde-yanlış-hata-mesajı)
18. [Özellik: SUPER_ADMIN Rolü Uygulaması](#18-özellik-super_admin-rolü-uygulaması)
19. [Özellik: VKN (Vergi Kimlik Numarası) Doğrulama Sistemi](#19-özellik-vkn-vergi-kimlik-numarası-doğrulama-sistemi)
20. [Özellik: SaaS Trial (Deneme Sürümü) Sistemi](#20-özellik-saas-trial-deneme-sürümü-sistemi)
21. [Özellik: Fatura Kota Yönetimi (QuotaService)](#21-özellik-fatura-kota-yönetimi-quotaservice)
22. [Özellik: Otomatik Abonelik Kontrolü (SubscriptionCheckScheduler)](#22-özellik-otomatik-abonelik-kontrolü-subscriptioncheckscheduler)
23. [Güvenlik: Yetki Yükseltme Engeli ve Endpoint Kilitleri](#23-güvenlik-yetki-yükseltme-engeli-ve-endpoint-kilitleri)
24. [Güvenlik: JWT ve CompanyContextFilter SUPER_ADMIN Desteği](#24-güvenlik-jwt-ve-companycontextfilter-super_admin-desteği)
25. [Güvenlik: Login'de Abonelik Durumu Kontrolü](#25-güvenlik-loginde-abonelik-durumu-kontrolü)
26. [Frontend: Kota Gösterimi ve Tip Güncellemeleri](#26-frontend-kota-gösterimi-ve-tip-güncellemeleri)
27. [Değiştirilen Dosyaların Listesi (12 Mart)](#27-değiştirilen-dosyaların-listesi-12-mart)

---

## 16. Sorun: Kullanıcı Sayfası Durum Kısmı Bozuk Görünüm

### Belirti
Kullanıcı yönetim sayfasındaki kullanıcıların "Durum" sütunu kötü ve düzensiz görünüyordu. Aktif/Pasif durumu düzgün biçimlendirilmemişti.

### Çözüm
Kullanıcı durum gösterimi CSS ile yeniden düzenlendi. Yerelleştirme dosyalarında (`common.json`) Türkçe ve İngilizce durum etiketleri güncellendi.

**Değiştirilen Dosyalar:**
- `frontend/src/messages/tr/common.json`
- `frontend/src/messages/en/common.json`

---

## 17. Sorun: Admin Rolü Değişikliğinde Yanlış Hata Mesajı

### Belirti
Admin kullanıcısı olarak giriş yapıldığında, kendi rolü "Manager" yapılmak istendiğinde "Hata oluştu" gibi genel bir hata mesajı görüntüleniyordu. İşlem aslında doğru bir şekilde reddediliyordu (kendi rolünü değiştirememe) ancak kullanıcıya gösterilen mesaj anlamsızdı.

### Kök Neden
Backend `"Cannot change your own role"` İngilizce hata mesajı döndürüyordu. Frontend'de bu mesaj doğru şekilde yakalanıp görüntülense de, kullanıcıya gösterilen mesaj yeterince açıklayıcı değildi. Ayrıca admin kullanıcının kendi rolünü değiştirmesi mantıksal olarak "yetkisiz işlem" uyarısı gerektiriyordu.

### Çözüm

**Backend — `UserManagementService.java`:**
Hata mesajları Türkçeye çevrildi ve daha açıklayıcı yapıldı:
```java
// Önceki
throw new DomainException("Cannot change your own role");
// Sonraki
throw new DomainException("Kendi rolünüzü değiştiremezsiniz");
```

**Frontend — `user-form-dialog.tsx`:**
SUPER_ADMIN, kota ve yeni Türkçe hata mesajları eklendi:
```typescript
if (errorMsg === 'Kendi rolünüzü değiştiremezsiniz' || errorMsg === 'Cannot change your own role') {
  toast.error(t('messages.unauthorizedRoleChange'));
} else if (errorMsg?.includes('SUPER_ADMIN')) {
  toast.error('SUPER_ADMIN rolü atanamaz veya değiştirilemez');
} else if (errorMsg?.includes('limitinize')) {
  toast.error(errorMsg); // Kota limiti uyarısı
}
```

**Değiştirilen Dosyalar:**
- `backend/src/main/java/com/faturaocr/application/user/UserManagementService.java`
- `frontend/src/app/(dashboard)/users/_components/user-form-dialog.tsx`

---

## 18. Özellik: SUPER_ADMIN Rolü Uygulaması

### Amaç
Platform düzeyinde bir yönetici rolü oluşturulması. SUPER_ADMIN, tüm şirketleri yönetebilen, abonelikleri kontrol edebilen ve diğer kullanıcılar tarafından atanamayan en yüksek yetki seviyesidir. Mali müşavirlerin yönettiği SaaS platformunda, platform sahibinin (sizin) şirketleri onaylaması, askıya alması ve yönetmesi için gereklidir.

### Yapılan Değişiklikler

#### Yeni Yetkiler — `Permission.java`
```java
COMPANY_CREATE,       // Şirket oluşturma (yalnızca SUPER_ADMIN)
COMPANY_DELETE,       // Şirket silme (yalnızca SUPER_ADMIN)
SUPER_ADMIN_ACCESS,   // Platform yönetim paneli erişimi
SUBSCRIPTION_MANAGE   // Abonelik yönetimi (aktif/askıya al)
```

#### Yeni Rol — `Role.java`
```java
SUPER_ADMIN(Set.of(Permission.values())) // TÜM yetkiler
```
SUPER_ADMIN, `Permission` enum'undaki tüm izinlere sahiptir. `ADMIN` rolü yalnızca şirket düzeyinde izinlere sahip kalır.

#### İlk SUPER_ADMIN Oluşturma — `SuperAdminInitializer.java` (YENİ)
Uygulama başlatıldığında ortam değişkenlerinden ilk SUPER_ADMIN hesabını oluşturur:
```bash
export SUPER_ADMIN_EMAIL=admin@yourplatform.com
export SUPER_ADMIN_PASSWORD=MySecureP@ss123!
```

- Eğer hesap zaten varsa hiçbir işlem yapmaz (idempotent).
- Şifreler hardcode değil, ortam değişkenlerinden okunur.
- SUPER_ADMIN'in `companyId`'si `null`'dır — platform düzeyinde kullanıcıdır.

**Yapılandırma — `application.yml`:**
```yaml
app:
  super-admin:
    email: ${SUPER_ADMIN_EMAIL:}
    password: ${SUPER_ADMIN_PASSWORD:}
```

#### JPA Katmanı
- `UserJpaEntity.RoleJpa` enum'una `SUPER_ADMIN` eklendi.
- `AuthenticatedUser` record'una `isSuperAdmin()` metodu eklendi.

#### Veritabanı Migration — `V46__add_super_admin_and_saas_fields.sql` (YENİ)
```sql
-- Users tablosuna SUPER_ADMIN rolünü kabul eden constraint güncellendi
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role
CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN'));
```

**Değiştirilen/Oluşturulan Dosyalar:**
- `backend/src/main/java/com/faturaocr/domain/user/valueobject/Permission.java`
- `backend/src/main/java/com/faturaocr/domain/user/valueobject/Role.java`
- `backend/src/main/java/com/faturaocr/infrastructure/common/config/SuperAdminInitializer.java` (YENİ)
- `backend/src/main/java/com/faturaocr/infrastructure/persistence/user/UserJpaEntity.java`
- `backend/src/main/java/com/faturaocr/infrastructure/security/AuthenticatedUser.java`
- `backend/src/main/resources/db/migration/V46__add_super_admin_and_saas_fields.sql` (YENİ)
- `backend/src/main/resources/application.yml`

---

## 19. Özellik: VKN (Vergi Kimlik Numarası) Doğrulama Sistemi

### Amaç
Şirket kaydı sırasında girilen 10 haneli vergi kimlik numarasının (VKN) gerçek bir numara olup olmadığının doğrulanması. Sahte şirket kayıtlarının engellenmesi.

### Yapılan Değişiklikler

#### VKN Doğrulama Algoritması — `TaxNumberValidator.java` (YENİ)
GİB (Gelir İdaresi Başkanlığı) resmi algoritmasına dayalı VKN ve TCKN doğrulama:

```java
public static boolean isValidVKN(String vkn) {
    // 10 haneli VKN kontrolü
    // Resmi GİB sağlama algoritması uygulanır
    // Son hane = (Σ dizin ağırlıklı mod işlemleri) mod 10
}

public static boolean isValidTCKN(String tckn) {
    // 11 haneli TCKN kontrolü
    // İlk hane 0 olamaz
    // 10. ve 11. haneler sağlama basamaklarıdır
}
```

#### Kayıt Sırasında Doğrulama — `AuthenticationService.java`
```java
// Regex formatı kontrolünden SONRA checksum doğrulaması
if (!TaxNumberValidator.isValidVKN(command.taxNumber())) {
    throw new DomainException("VALIDATION_ERROR", "Geçersiz vergi kimlik numarası (VKN)");
}
```

#### Veritabanı Kısıtlaması — `V46` Migration
```sql
-- VKN benzersizlik kısıtlaması (aynı VKN ile iki şirket kaydı engellensin)
ALTER TABLE companies ADD CONSTRAINT uq_companies_tax_number UNIQUE (tax_number);
```

**Değiştirilen/Oluşturulan Dosyalar:**
- `backend/src/main/java/com/faturaocr/domain/common/util/TaxNumberValidator.java` (YENİ)
- `backend/src/main/java/com/faturaocr/application/auth/service/AuthenticationService.java`
- `backend/src/main/resources/db/migration/V46__add_super_admin_and_saas_fields.sql`

---

## 20. Özellik: SaaS Trial (Deneme Sürümü) Sistemi

### Amaç
Yeni kayıt olan şirketlerin 7 gün boyunca ücretsiz deneme sürümü ile sistemi kullanabilmesi. Deneme süresinde günlük 50, toplam 350 fatura işleme hakkı verilmesi. Süre sonunda ödeme yapılmazsa hesabın askıya alınması (verilerin silinmemesi).

### Yapılan Değişiklikler

#### Domain Entity — `Company.java`
Şirket entity'sine abonelik ve kota alanları eklendi:

| Alan | Tip | Varsayılan | Açıklama |
|------|-----|-----------|----------|
| `subscriptionStatus` | String | `"TRIAL"` | TRIAL / ACTIVE / SUSPENDED |
| `trialEndsAt` | LocalDateTime | Şimdi + 7 gün | Deneme bitiş tarihi |
| `planId` | String | `"trial"` | Paket kimliği |
| `maxUsers` | int | `2` | Maksimum kullanıcı sayısı |
| `maxInvoices` | int | `350` | Toplam fatura limiti |
| `dailyInvoiceLimit` | int | `50` | Günlük fatura limiti |
| `usedInvoiceCount` | int | `0` | Kullanılan toplam fatura sayısı |
| `dailyInvoiceCount` | int | `0` | Bugün kullanılan fatura sayısı |
| `dailyCountDate` | LocalDate | Bugün | Günlük sayaç tarihi |
| `suspendedAt` | LocalDateTime | null | Askıya alınma zamanı |
| `suspensionReason` | String | null | Askıya alma sebebi |

Constructor'da varsayılan TRIAL değerleri otomatik atanır:
```java
public Company(String name, String taxNumber) {
    // ...
    this.subscriptionStatus = "TRIAL";
    this.trialEndsAt = LocalDateTime.now().plusDays(7);
    this.maxInvoices = 350;
    this.dailyInvoiceLimit = 50;
    this.maxUsers = 2;
    this.planId = "trial";
}
```

#### JPA Katmanı
- `CompanyJpaEntity.java` — 11 yeni abonelik sütunu eklendi.
- `CompanyMapper.java` — Tüm abonelik alanlarının domain ↔ JPA dönüşümü güncellendi.
- `CompanyJpaRepository.java` — `findAllBySubscriptionStatusAndTrialEndsAtBefore()` sorgusu eklendi.

#### Veritabanı Migration — `V46`
```sql
ALTER TABLE companies
    ADD COLUMN subscription_status VARCHAR(20) DEFAULT 'ACTIVE',
    ADD COLUMN trial_ends_at TIMESTAMP,
    ADD COLUMN plan_id VARCHAR(50) DEFAULT 'basic',
    ADD COLUMN max_users INTEGER DEFAULT 5,
    ADD COLUMN max_invoices INTEGER DEFAULT 1000,
    ADD COLUMN daily_invoice_limit INTEGER DEFAULT 100,
    ADD COLUMN used_invoice_count INTEGER DEFAULT 0,
    ADD COLUMN daily_invoice_count INTEGER DEFAULT 0,
    ADD COLUMN daily_count_date DATE DEFAULT CURRENT_DATE,
    ADD COLUMN suspended_at TIMESTAMP,
    ADD COLUMN suspension_reason TEXT;
```

**Değiştirilen/Oluşturulan Dosyalar:**
- `backend/src/main/java/com/faturaocr/domain/company/entity/Company.java`
- `backend/src/main/java/com/faturaocr/infrastructure/persistence/company/CompanyJpaEntity.java`
- `backend/src/main/java/com/faturaocr/infrastructure/persistence/company/CompanyMapper.java`
- `backend/src/main/java/com/faturaocr/infrastructure/persistence/company/CompanyJpaRepository.java`
- `backend/src/main/resources/db/migration/V46__add_super_admin_and_saas_fields.sql`

---

## 21. Özellik: Fatura Kota Yönetimi (QuotaService)

### Amaç
Fatura işleme ve kullanıcı ekleme işlemlerinden ÖNCE kota kontrolü yapılması. LLM API maliyetlerinin kontrol altında tutulması.

### Yapılan Değişiklikler

#### QuotaService.java (YENİ)
```java
// 1. Fatura kotası kontrolü — LLM çağrısından ÖNCE çalışır
quotaService.checkInvoiceQuota(companyId);
// Kontroller: SUSPENDED mi? Trial süresi dolmuş mu? Toplam limit? Günlük limit?

// 2. Başarılı extraction sonrası sayaç artırma
quotaService.incrementInvoiceCount(companyId);

// 3. Kullanıcı ekleme öncesi kota kontrolü
quotaService.checkUserQuota(companyId);

// 4. Frontend'e kota bilgisi gönderme
QuotaInfo info = quotaService.getQuotaInfo(companyId);
// → usedInvoices, maxInvoices, dailyUsed, dailyMax, remainingInvoices...
```

#### QuotaController.java (YENİ)
```java
@GetMapping("/api/v1/quota")
public ApiResponse<QuotaInfo> getQuotaInfo() {
    // Frontend'e kota bilgisini döndürür
}
```

#### InvoiceUploadService.java — Kota Entegrasyonu
```java
public Invoice uploadAndExtract(MultipartFile file, UUID companyId, UUID userId) {
    // 0. Check quota BEFORE ANY processing (LLM cost protection)
    quotaService.checkInvoiceQuota(companyId);

    // ... dosya doğrulama, kaydetme, extraction ...

    // Başarılı extraction sonrası sayaç artır
    quotaService.incrementInvoiceCount(companyId);
}
```

#### UserManagementService.java — Kullanıcı Kota Kontrolü
```java
public UserResponse createUser(CreateUserCommand command) {
    // SUPER_ADMIN ataması engelle
    if (command.getRole() == Role.SUPER_ADMIN) {
        throw new DomainException("SUPER_ADMIN rolü atanamaz");
    }
    // Kullanıcı kotasını kontrol et
    quotaService.checkUserQuota(companyId);
    // ... kullanıcı oluştur ...
}
```

**Değiştirilen/Oluşturulan Dosyalar:**
- `backend/src/main/java/com/faturaocr/application/company/QuotaService.java` (YENİ)
- `backend/src/main/java/com/faturaocr/interfaces/rest/quota/QuotaController.java` (YENİ)
- `backend/src/main/java/com/faturaocr/application/invoice/service/InvoiceUploadService.java`
- `backend/src/main/java/com/faturaocr/application/user/UserManagementService.java`

---

## 22. Özellik: Otomatik Abonelik Kontrolü (SubscriptionCheckScheduler)

### Amaç
Her gece saat 00:00'da çalışarak, deneme süresi dolmuş şirketleri otomatik olarak askıya alınması.

### Yapılan Değişiklikler

#### SubscriptionCheckScheduler.java (YENİ)
```java
@Scheduled(cron = "0 0 0 * * *") // Her gece 00:00
@Transactional
public void suspendExpiredTrials() {
    List<CompanyJpaEntity> expiredTrials = companyJpaRepository
        .findAllBySubscriptionStatusAndTrialEndsAtBefore("TRIAL", LocalDateTime.now());

    for (CompanyJpaEntity company : expiredTrials) {
        company.setSubscriptionStatus("SUSPENDED");
        company.setSuspendedAt(LocalDateTime.now());
        company.setSuspensionReason("Trial period expired");
        companyJpaRepository.save(company);
    }
}
```

#### @EnableScheduling — `FaturaOcrApplication.java`
```java
@SpringBootApplication
@EnableScheduling  // Scheduler'ın çalışması için gerekli
public class FaturaOcrApplication { ... }
```

**Değiştirilen/Oluşturulan Dosyalar:**
- `backend/src/main/java/com/faturaocr/infrastructure/scheduler/SubscriptionCheckScheduler.java` (YENİ)
- `backend/src/main/java/com/faturaocr/FaturaOcrApplication.java`

---

## 23. Güvenlik: Yetki Yükseltme Engeli ve Endpoint Kilitleri

### Amaç
SUPER_ADMIN rolünün hiçbir kullanıcı veya endpoint üzerinden atanamaz olması. Şirket oluşturma, silme ve abonelik yönetiminin yalnızca SUPER_ADMIN'e kilitlenmesi.

### Yapılan Değişiklikler

#### CompanyController.java — Endpoint Kilitleri
```java
// Önceki
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
public ApiResponse<CompanyResponse> createCompany(...)

// Sonraki
@PreAuthorize("hasAuthority('COMPANY_CREATE')")  // Yalnızca SUPER_ADMIN
public ApiResponse<CompanyResponse> createCompany(...)

@PreAuthorize("hasAuthority('COMPANY_DELETE')")   // Yalnızca SUPER_ADMIN
public void deleteCompany(...)

@PreAuthorize("hasAuthority('SUBSCRIPTION_MANAGE')") // Yalnızca SUPER_ADMIN
public ApiResponse<CompanyResponse> activateCompany(...)
```

#### SecurityConfig.java — Route Güvenliği
```java
// Önceki
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
.requestMatchers("/api/v1/system/**").hasRole("ADMIN")

// Sonraki
.requestMatchers("/api/v1/admin/**").hasAuthority("SUPER_ADMIN_ACCESS")
.requestMatchers("/api/v1/system/**").hasAuthority("SUPER_ADMIN_ACCESS")
```

#### UserManagementService.java — Yetki Yükseltme Engeli
```java
// SUPER_ADMIN rolü ASLA atanamaz
if (command.getRole() == Role.SUPER_ADMIN) {
    throw new DomainException("SUPER_ADMIN rolü atanamaz");
}

// SUPER_ADMIN kullanıcısının rolü DEĞİŞTİRİLEMEZ
if (user.getRole() == Role.SUPER_ADMIN) {
    throw new DomainException("SUPER_ADMIN kullanıcısının rolü değiştirilemez");
}
```

**Değiştirilen Dosyalar:**
- `backend/src/main/java/com/faturaocr/interfaces/rest/company/CompanyController.java`
- `backend/src/main/java/com/faturaocr/infrastructure/common/config/SecurityConfig.java`
- `backend/src/main/java/com/faturaocr/application/user/UserManagementService.java`

---

## 24. Güvenlik: JWT ve CompanyContextFilter SUPER_ADMIN Desteği

### Amaç
SUPER_ADMIN'in JWT token'ında tanınması ve şirket bağlamı (company context) kısıtlamasından muaf tutulması.

### Yapılan Değişiklikler

#### JwtTokenProvider.java — `isSuperAdmin` Claim
```java
claims.put("isSuperAdmin", user.getRole() == Role.SUPER_ADMIN);
```

#### AuthenticatedUser.java — `isSuperAdmin()` Metodu
```java
public boolean isSuperAdmin() {
    return "SUPER_ADMIN".equals(role);
}

public boolean isAdmin() {
    return "ADMIN".equals(role) || isSuperAdmin(); // SUPER_ADMIN da admin yetkilerine sahip
}
```

#### CompanyContextFilter.java — SUPER_ADMIN Bypass
```java
if (user.isSuperAdmin()) {
    // SUPER_ADMIN, X-Company-Id header'ı ile HERHANGİ bir şirkete erişebilir
    String headerCompanyId = request.getHeader("X-Company-Id");
    if (StringUtils.hasText(headerCompanyId)) {
        CompanyContextHolder.setCompanyId(UUID.fromString(headerCompanyId));
    }
    // Header yoksa companyId null kalır — SUPER_ADMIN'in companyId'si olmayabilir
} else {
    // Normal kullanıcılar için mevcut erişim kontrolü devam eder
}
```

**Değiştirilen Dosyalar:**
- `backend/src/main/java/com/faturaocr/infrastructure/security/JwtTokenProvider.java`
- `backend/src/main/java/com/faturaocr/infrastructure/security/AuthenticatedUser.java`
- `backend/src/main/java/com/faturaocr/infrastructure/security/CompanyContextFilter.java`

---

## 25. Güvenlik: Login'de Abonelik Durumu Kontrolü

### Amaç
Askıya alınmış (SUSPENDED) veya deneme süresi dolmuş (TRIAL expired) şirketlerin kullanıcılarının sisteme giriş yapamaması ve ödeme sayfasına yönlendirilecek bir hata mesajı alması.

### Yapılan Değişiklikler

#### AuthenticationService.java — Login Kontrolü
```java
// Şifre doğrulaması BAŞARILI olduktan SONRA:
if (user.getRole() != Role.SUPER_ADMIN && user.getCompanyId() != null) {
    Company company = companyRepository.findById(user.getCompanyId()).get();

    if (company.isSuspended()) {
        throw new DomainException("SUBSCRIPTION_SUSPENDED",
            "Aboneliğiniz askıya alınmıştır. Devam etmek için ödeme yapınız.");
    }
    if (company.isTrialExpired()) {
        company.suspend("Trial period expired");
        companyRepository.save(company);
        throw new DomainException("TRIAL_EXPIRED",
            "Deneme süreniz sona ermiştir. Devam etmek için ödeme yapınız.");
    }
}
```

**Güvenlik Notu:**
- SUPER_ADMIN bu kontrolden muaftır (platform yöneticisi her zaman giriş yapabilir).
- `companyId == null` olan kullanıcılar da muaftır (SUPER_ADMIN gibi platform düzeyinde hesaplar).
- Deneme süresi login sırasında dolmuşsa, o anda otomatik askıya alınır.

#### AuthenticationService.java — Kayıt VKN Doğrulama
```java
// Yeni şirket kaydında VKN checksum doğrulaması
if (!TaxNumberValidator.isValidVKN(command.taxNumber())) {
    throw new DomainException("VALIDATION_ERROR", "Geçersiz vergi kimlik numarası (VKN)");
}
```

**Değiştirilen Dosya:**
- `backend/src/main/java/com/faturaocr/application/auth/service/AuthenticationService.java`

---

## 26. Frontend: Kota Gösterimi ve Tip Güncellemeleri

### Amaç
- Fatura yükleme panelinde kalan fatura hakkının görsel olarak gösterilmesi.
- SUPER_ADMIN rolünün frontend tiplerinde tanımlanması.
- Abonelik ve kota ile ilgili hata mesajlarının düzgün gösterilmesi.

### Yapılan Değişiklikler

#### Kota Gösterimi — `upload/page.tsx`
Upload sayfasının sağ sütununa (bilgi paneli) yeni bir "Kota Bilgisi" kartı eklendi:

- **Günlük Kullanım** bar grafiği (ör. `12 / 50`) — 5'ten az hak kaldığında kırmızıya döner.
- **Toplam Kullanım** bar grafiği (ör. `45 / 350`) — 20'den az hak kaldığında kırmızıya döner.
- **Kullanıcı** sayısı (ör. `1 / 2`).
- **Trial Sürümü** badge'i ve bitiş tarihi (ör. "19.03.2026 tarihine kadar").

```typescript
useEffect(() => {
    apiClient.get('/api/v1/quota')
      .then(res => setQuotaInfo(res.data?.data))
      .catch(() => {/* kota bilgisi isteğe bağlı */});
}, []);
```

#### TypeScript Tip Güncellemeleri — `auth.ts`
```typescript
// Önceki
export type UserRole = 'ADMIN' | 'MANAGER' | 'ACCOUNTANT' | 'INTERN';

// Sonraki
export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'MANAGER' | 'ACCOUNTANT' | 'INTERN';

// Yeni QuotaInfo arayüzü eklendi
export interface QuotaInfo {
  usedInvoices: number;
  maxInvoices: number;
  dailyUsedInvoices: number;
  dailyMaxInvoices: number;
  usedUsers: number;
  maxUsers: number;
  subscriptionStatus: string;
  trialEndsAt: string | null;
  planId: string;
  remainingInvoices: number;
  dailyRemainingInvoices: number;
}
```

#### Hata Mesajları — `user-form-dialog.tsx`
SUPER_ADMIN ve kota hatalarını işleyen yeni koşullar eklendi (bkz. Bölüm 17).

**Değiştirilen Dosyalar:**
- `frontend/src/app/(dashboard)/invoices/upload/page.tsx`
- `frontend/src/types/auth.ts`
- `frontend/src/app/(dashboard)/users/_components/user-form-dialog.tsx`

---

## 27. Değiştirilen Dosyaların Listesi (12 Mart)

### Yeni Oluşturulan Dosyalar

| # | Dosya | Açıklama |
|---|-------|----------|
| 1 | `backend/src/main/java/com/faturaocr/domain/common/util/TaxNumberValidator.java` | GİB resmi algoritması ile VKN/TCKN doğrulama utility sınıfı |
| 2 | `backend/src/main/java/com/faturaocr/application/company/QuotaService.java` | Fatura + kullanıcı kota yönetimi servisi |
| 3 | `backend/src/main/java/com/faturaocr/infrastructure/common/config/SuperAdminInitializer.java` | İlk SUPER_ADMIN hesabını env var'dan oluşturan CommandLineRunner |
| 4 | `backend/src/main/java/com/faturaocr/infrastructure/scheduler/SubscriptionCheckScheduler.java` | Gece otomatik trial askıya alma scheduler'ı |
| 5 | `backend/src/main/java/com/faturaocr/interfaces/rest/quota/QuotaController.java` | `GET /api/v1/quota` — Frontend'e kota bilgisi döndüren endpoint |
| 6 | `backend/src/main/resources/db/migration/V46__add_super_admin_and_saas_fields.sql` | SUPER_ADMIN + SaaS abonelik + kota alanları + VKN unique constraint |

### Düzenlenen Dosyalar

| # | Dosya | Değişiklik Özeti |
|---|-------|-----------------|
| 7 | `backend/.../Permission.java` | `COMPANY_CREATE`, `COMPANY_DELETE`, `SUPER_ADMIN_ACCESS`, `SUBSCRIPTION_MANAGE` eklendi |
| 8 | `backend/.../Role.java` | `SUPER_ADMIN` rolü tüm yetkilerle eklendi |
| 9 | `backend/.../Company.java` | 11 abonelik/kota alanı, `isSuspended()`, `isTrialExpired()`, `incrementInvoiceCount()`, VKN doğrulama |
| 10 | `backend/.../CompanyJpaEntity.java` | 11 yeni sütun (subscription, kota, trial) |
| 11 | `backend/.../CompanyMapper.java` | Yeni alanların domain ↔ JPA dönüşümü |
| 12 | `backend/.../CompanyJpaRepository.java` | `findAllBySubscriptionStatusAndTrialEndsAtBefore()` sorgusu |
| 13 | `backend/.../UserJpaEntity.java` | `RoleJpa` enum'una `SUPER_ADMIN` eklendi |
| 14 | `backend/.../UserJpaRepository.java` | `countByCompanyIdAndIsActiveTrue()` sorgusu |
| 15 | `backend/.../UserRepository.java` (port) | `countActiveByCompanyId()`, `findByEmailValue()` |
| 16 | `backend/.../UserRepositoryAdapter.java` | İki yeni metod implementasyonu |
| 17 | `backend/.../AuthenticationService.java` | VKN doğrulama, TRIAL kayıt, SUSPENDED login kontrolü |
| 18 | `backend/.../SecurityConfig.java` | `/admin/**` ve `/system/**` → `SUPER_ADMIN_ACCESS` yetkisi |
| 19 | `backend/.../CompanyController.java` | create/delete/activate/deactivate → SUPER_ADMIN kilidi |
| 20 | `backend/.../UserManagementService.java` | Yetki yükseltme engeli + kullanıcı kotası + QuotaService DI |
| 21 | `backend/.../InvoiceUploadService.java` | Fatura kotası kontrolü (LLM öncesi) + sayaç artırma |
| 22 | `backend/.../JwtTokenProvider.java` | `isSuperAdmin` JWT claim |
| 23 | `backend/.../AuthenticatedUser.java` | `isSuperAdmin()` metodu |
| 24 | `backend/.../CompanyContextFilter.java` | SUPER_ADMIN company context bypass |
| 25 | `backend/.../FaturaOcrApplication.java` | `@EnableScheduling` eklendi |
| 26 | `backend/.../application.yml` | `app.super-admin.email/password` yapılandırması |
| 27 | `frontend/src/types/auth.ts` | `SUPER_ADMIN` role type + `QuotaInfo` interface |
| 28 | `frontend/.../upload/page.tsx` | Kota gösterimi kartı (günlük/toplam bar grafik, trial badge) |
| 29 | `frontend/.../user-form-dialog.tsx` | SUPER_ADMIN + kota hata mesajları |
| 30 | `frontend/.../common.json` (tr + en) | Durum etiketleri güncellendi |

---

## Alınan Dersler ve Öneriler (12 Mart)

1. **SUPER_ADMIN Güvenliği:** SUPER_ADMIN hiçbir zaman API üzerinden atanamaz; yalnızca uygulama başlangıcında ortam değişkenlerinden oluşturulur. Bu, SQL injection, privilege escalation ve API kötüye kullanımını engeller.
2. **Kota Kontrolü Zamanlaması:** LLM API çağrısı (maliyet) yapılmadan ÖNCE kota kontrolü yapılmalıdır. Kota aşıldığında dosya doğrulama bile gereksizdir.
3. **VKN Doğrulama:** Format kontrolü (`^\\d{10}$` regex) yeterli değildir; GİB checksum algoritması ile gerçek VKN olduğu doğrulanmalıdır. Bu, sahte kayıtları ve bot saldırılarını engellemede ek bir güvenlik katmanıdır.
4. **SaaS Trial Yönetimi:** Deneme süresi hem login sırasında hem de gece scheduler ile kontrol edilmelidir. Sadece scheduler'a bel bağlamak, gün içinde süre dolan kullanıcıların sistemi kullanmaya devam etmesine neden olabilir.
5. **Soft Delete Prensibi:** Askıya alınan şirketlerin verileri ASLA silinmez. `SUSPENDED` durumu verileri korur, yalnızca erişimi kısıtlar. Bu hem yasal uyumluluk (veri saklama yükümlülüğü) hem de müşteri geri kazanımı için kritiktir.
6. **Environment Variable Güvenliği:** SUPER_ADMIN şifresi asla kaynak kodunda veya yapılandırma dosyasında yer almamalıdır. `application.yml`'de yalnızca `${SUPER_ADMIN_PASSWORD:}` (boş varsayılan) bulunmalıdır.
7. **CompanyContext Null Güvenliği:** SUPER_ADMIN'in `companyId`'si `null` olabilir. Tüm şirket bağlamına bağlı servislerin bu durumu ele alması gerekir.
