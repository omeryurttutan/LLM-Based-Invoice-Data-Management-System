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

## Alınan Dersler ve Öneriler

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
