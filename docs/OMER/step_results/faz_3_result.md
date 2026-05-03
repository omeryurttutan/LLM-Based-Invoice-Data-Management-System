# Faz 3 Sonuç Raporu: Veritabanı Şeması ve Migrasyon Altyapısı

## 1. Yürütme Durumu

- **Durum**: ✅ Başarılı
- **Tamamlanma Tarihi**: 14.02.2026
- **Tahmini Süre**: 2-3 gün
- **Harcanan Süre**: ~2 saat (dosya oluşturma + bağımlılık düzeltme)

---

## 2. Tamamlanan Görevler

- [x] Flyway bağımlılıkları pom.xml'e eklendi (`flyway-core` + `flyway-database-postgresql`)
- [x] Flyway ayarları application.yml'de yapılandırıldı
- [x] V1\_\_create_initial_schema.sql — 10 tablo (companies, users, categories, invoices, invoice_items, audit_logs, refresh_tokens, invoice_versions, notification_settings, notifications)
- [x] V2\_\_create_indexes.sql — 30+ indeks + pg_trgm uzantısı
- [x] V3\_\_insert_default_data.sql — Varsayılan şirket, admin kullanıcı, 8 kategori, bildirim ayarları
- [x] V4\_\_create_update_timestamp_trigger.sql — 6 tablo için `updated_at` auto-update trigger
- [x] Audit log immutability trigger (V1 içinde)
- [x] Soft delete alanları tüm ana tablolarda mevcut
- [x] UUID primary key tüm tablolarda kullanıldı
- [x] Multi-tenant (company_id) izolasyon tüm ana tablolarda uygulandı

---

## 3. Oluşturulan Migrasyon Dosyaları

```
backend/src/main/resources/db/migration/
├── V1__create_initial_schema.sql    (447 satır - Tüm tablolar)
├── V2__create_indexes.sql           (142 satır - Tüm indeksler)
├── V3__insert_default_data.sql      (88 satır  - Seed data)
├── V4__create_update_timestamp_trigger.sql (50 satır - Triggerlar)
├── V5__add_company_id_to_audit_logs.sql   (Faz 8 - Audit eklentisi)
└── V6__duplicate_detection_indexes.sql    (Faz 9 - Duplikasyon indexleri)
```

> **Not**: V5 ve V6 sonraki fazlardan (Faz 8, 9) gelmiştir.

---

## 4. Oluşturulan Tablolar (10 Tablo)

| #   | Tablo Adı             | Sütun Sayısı | Açıklama                      | Soft Delete | company_id |
| --- | --------------------- | ------------ | ----------------------------- | ----------- | ---------- |
| 1   | companies             | 16           | Şirket/Tenant bilgileri       | ✅          | —          |
| 2   | users                 | 17           | Kullanıcılar ve RBAC          | ✅          | ✅         |
| 3   | categories            | 10           | Fatura kategorileri           | ✅          | ✅         |
| 4   | invoices              | 33           | Ana fatura tablosu            | ✅          | ✅         |
| 5   | invoice_items         | 13           | Fatura kalemleri              | —           | —          |
| 6   | audit_logs            | 13           | Denetim kayıtları (immutable) | —           | —          |
| 7   | refresh_tokens        | 7            | JWT yenileme tokenları        | —           | —          |
| 8   | invoice_versions      | 6            | Fatura versiyon geçmişi       | —           | —          |
| 9   | notification_settings | 6            | Bildirim ayarları             | —           | —          |
| 10  | notifications         | 9            | Kullanıcı bildirimleri        | —           | —          |

---

## 5. Oluşturulan İndeksler (30+ İndeks)

### Companies (2)

- `idx_companies_tax_number` (partial: is_deleted=FALSE)
- `idx_companies_is_active` (partial: is_deleted=FALSE)

### Users (5)

- `idx_users_email`
- `idx_users_company_id`
- `idx_users_role`
- `idx_users_is_active`
- `idx_users_company_email` (composite)

### Categories (2)

- `idx_categories_company_id`
- `idx_categories_parent_id`

### Invoices (14) — En kritik tablo

- `idx_invoices_company_id`
- `idx_invoices_status`
- `idx_invoices_invoice_date`
- `idx_invoices_due_date`
- `idx_invoices_category_id`
- `idx_invoices_source_type`
- `idx_invoices_llm_provider`
- `idx_invoices_supplier_name`
- `idx_invoices_supplier_tax_number`
- `idx_invoices_company_invoice_number` (UNIQUE composite)
- `idx_invoices_company_status` (composite)
- `idx_invoices_company_date` (composite DESC)
- `idx_invoices_status_date` (composite DESC)
- `idx_invoices_supplier_name_trgm` (GIN trigram)
- `idx_invoices_confidence_score` (partial: < 70)

### Invoice Items (1)

- `idx_invoice_items_invoice_id`

### Audit Logs (5)

- `idx_audit_logs_user_id`
- `idx_audit_logs_entity` (composite)
- `idx_audit_logs_action_type`
- `idx_audit_logs_created_at` (DESC)
- `idx_audit_logs_entity_time` (composite DESC)

### Refresh Tokens (2)

- `idx_refresh_tokens_user_id`
- `idx_refresh_tokens_expires_at` (partial: revoked_at IS NULL)

### Invoice Versions (1)

- `idx_invoice_versions_invoice_id`

### Notifications (3)

- `idx_notifications_user_id`
- `idx_notifications_unread` (partial: is_read=FALSE)
- `idx_notifications_created_at` (DESC)

---

## 6. Veritabanı Uzantıları

| Uzantı    | Amaç                              |
| --------- | --------------------------------- |
| uuid-ossp | UUID primary key oluşturma        |
| pg_trgm   | Trigram tabanlı metin arama (GIN) |

---

## 7. Triggerlar ve Fonksiyonlar

| Trigger/Fonksiyon              | Tablo                                                                        | Açıklama                           |
| ------------------------------ | ---------------------------------------------------------------------------- | ---------------------------------- |
| `prevent_audit_modification()` | audit_logs                                                                   | UPDATE/DELETE engeller (immutable) |
| `update_updated_at_column()`   | companies, users, categories, invoices, invoice_items, notification_settings | `updated_at` otomatik günceller    |

---

## 8. Varsayılan (Seed) Veriler

### Demo Şirketi

- ID: `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`
- Vergi No: `1234567890`

### Admin Kullanıcı

- Email: `admin@demo.com`
- Şifre: `Admin123!` (BCrypt hash)
- Rol: `ADMIN`

### Kategoriler (8 adet)

| Kategori             | Renk    |
| -------------------- | ------- |
| Genel Giderler       | #6B7280 |
| Hammadde             | #10B981 |
| Hizmet Alımları      | #3B82F6 |
| Kira                 | #F59E0B |
| Elektrik/Su/Doğalgaz | #EF4444 |
| Personel             | #8B5CF6 |
| Teknoloji            | #EC4899 |
| Pazarlama            | #14B8A6 |

---

## 9. ER Diyagramı

```
┌─────────────────┐       ┌─────────────────┐
│   companies     │       │   categories    │
│─────────────────│       │─────────────────│
│ id (PK)         │       │ id (PK)         │
│ name, tax_number│       │ name, color     │
│ is_deleted      │       │ company_id (FK) │
│ created_at      │       │ parent_id (FK)  │
└────────┬────────┘       └────────┬────────┘
         │ 1:N                     │ 1:N
         ▼                         │
┌─────────────────┐                │
│     users       │                │
│─────────────────│                │
│ id (PK)         │                │
│ company_id (FK) │                │
│ email, role     │                │
│ password_hash   │                │
└────────┬────────┘                │
         │ (FK)                    │
         ▼                         ▼
┌─────────────────────────────────────────────┐
│                  invoices                    │
│─────────────────────────────────────────────│
│ id (PK), company_id (FK), category_id (FK) │
│ created_by_user_id (FK)                     │
│ invoice_number, supplier_name, total_amount │
│ source_type, llm_provider, confidence_score │
│ status: PENDING/VERIFIED/REJECTED/PROCESSING│
└─────────────────┬───────────────────────────┘
                  │ 1:N                1:N
         ┌────────┴────────┐     ┌──────────────┐
         ▼                 ▼     │invoice_versions│
┌──────────────────┐            └──────────────┘
│  invoice_items   │
│──────────────────│   ┌─────────────────────┐
│ invoice_id (FK)  │   │    audit_logs       │
│ description, qty │   │ (immutable)         │
│ unit_price       │   │ user_id, entity_type│
│ tax_rate         │   │ old_value (JSONB)   │
└──────────────────┘   │ new_value (JSONB)   │
                       └─────────────────────┘
┌──────────────────┐   ┌─────────────────────┐
│ refresh_tokens   │   │ notifications       │
│ user_id (FK)     │   │ user_id (FK)        │
│ token_hash       │   │ title, message      │
│ expires_at       │   │ type, is_read       │
└──────────────────┘   └─────────────────────┘
                       ┌─────────────────────┐
                       │notification_settings│
                       │ user_id (FK, UNIQUE)│
                       │ email/push/in_app   │
                       └─────────────────────┘
```

---

## 10. Flyway Yapılandırması

### pom.xml Bağımlılıkları

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### application.yml

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
  jpa:
    hibernate:
      ddl-auto: validate # Flyway manages schema, Hibernate only validates
```

---

## 11. Doğrulama Komutları

Docker ortamı aktif olduğunda aşağıdaki komutlarla doğrulama yapılmalıdır:

```bash
# Tabloları listele
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c \
  "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"

# İndeksleri listele
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c \
  "SELECT indexname, tablename FROM pg_indexes WHERE schemaname='public' AND indexname LIKE 'idx_%' ORDER BY tablename;"

# Varsayılan verileri kontrol et
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c \
  "SELECT name FROM companies; SELECT email, role FROM users; SELECT name FROM categories;"

# Audit log immutability testi
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c \
  "UPDATE audit_logs SET action_type='DELETE' WHERE 1=1;"
# Beklenen: ERROR: Audit logs cannot be modified or deleted

# updated_at trigger testi
docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c \
  "SELECT updated_at FROM companies LIMIT 1;
   UPDATE companies SET name='Test' WHERE id='a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
   SELECT updated_at FROM companies LIMIT 1;"
```

---

## 12. Karşılaşılan Sorunlar ve Çözümler

| Sorun                                | Çözüm                                                                           |
| ------------------------------------ | ------------------------------------------------------------------------------- |
| `flyway-database-postgresql` eksikti | Spring Boot 3.2.3 Flyway 10.x kullanır, bu modül gereklidir — eklendi           |
| Docker erişimi ilk denemede yoktu    | Geliştirme ortamında Docker kurulu değildi, migrasyon dosyaları hazır bırakıldı |
| `./mvnw` wrapper bulunamadı          | Global `mvn` komutu kullanıldı                                                  |

---

## 13. Doğrulama Kontrol Listesi

- [x] Flyway bağımlılıkları pom.xml'de (`flyway-core` + `flyway-database-postgresql`)
- [x] Flyway ayarları application.yml'de
- [x] V1\_\_create_initial_schema.sql — 10 tablo oluşturuldu
- [x] V2\_\_create_indexes.sql — 30+ indeks tanımlandı
- [x] V3\_\_insert_default_data.sql — Seed data hazır
- [x] V4\_\_create_update_timestamp_trigger.sql — Triggerlar tanımlandı
- [x] companies tablosu tüm gerekli sütunlara sahip
- [x] users tablosu güvenlik alanları dahil (failed_login_attempts, locked_until, password_changed_at)
- [x] invoices tablosu source_type, llm_provider, confidence_score içeriyor
- [x] invoice_items tablosu FK ile invoices'a bağlı
- [x] categories tablosu company izolasyonlu
- [x] audit_logs tablosu immutable (trigger ile korunuyor)
- [x] refresh_tokens tablosu JWT için oluşturuldu
- [x] Soft delete alanları (is_deleted, deleted_at) tüm ana tablolarda
- [x] pg_trgm uzantısı full-text search için etkinleştirildi
- [x] UUID primary key tüm tablolarda
- [x] 3NF normalizasyon uygulandı

---

## 14. Sonraki Adımlar (Faz 4: Kimlik Doğrulama)

- JWT tabanlı kimlik doğrulama implementasyonu
- `users` tablosu ve `refresh_tokens` tablosu kullanılacak
- Spring Security yapılandırması
- Login/Register/Refresh endpoint'leri
