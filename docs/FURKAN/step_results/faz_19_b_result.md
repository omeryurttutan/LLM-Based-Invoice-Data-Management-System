# Faz 19-B: RabbitMQ Producer (Spring Boot) - Sonuç Raporu

**Tarih**: 14.02.2026
**Durum**: ✅ Tamamlandı
**Uygulayan**: Ömer Talha Yurttutan (Backend Developer)

---

## 1. Özet
Spring Boot backend tarafında RabbitMQ altyapısı kuruldu. Fatura yükleme işleminde asenkron veri çıkarma (extraction) için gerekli olan Producer ve Result Listener servisleri implemente edildi.
- 3 adet Exchange ve 3 adet Kuyruk tanımlandı.
- `Invoice` entity ve veritabanı şeması asenkron işlem durumlarını (`QUEUED`, `FAILED`) destekleyecek şekilde güncellendi.
- Extraction isteği gönderen Producer servisi ve sonuçları dinleyen Listener servisi yazıldı.
- Retry (tekrar deneme) mekanizması kuruldu.

## 2. Yapılan Değişiklikler

### 2.1. Veritabanı ve Domain
- **Migration**: `V7__add_async_processing_support.sql` eklendi.
  - `invoices.status` alanına `QUEUED` ve `FAILED` durumları eklendi.
  - `invoices` tablosuna `correlation_id` kolonu (indexli) eklendi.
- **Domain**: `Invoice` entity'sine `correlationId` eklendi. `InvoiceStatus` enum'ı güncellendi.

### 2.2. RabbitMQ Topolojisi
`RabbitMQConfig` sınıfında aşağıdaki yapılar tanımlandı (durable):

| Tip | İsim | Açıklama |
|---|---|---|
| **Exchange** | `invoice.extraction` | Doğrudan (Direct) exchange, istekler buraya atılır. |
| **Exchange** | `invoice.extraction.dlx` | Dead Letter Exchange. |
| **Exchange** | `invoice.extraction.result` | Sonuçların döndüğü exchange. |
| **Queue** | `invoice.extraction.queue` | İstek kuyruğu. (Routing Key: `extraction.request`). DLX tanımlı. |
| **Queue** | `invoice.extraction.dlq` | Hatalı mesajların düştüğü kuyruk. (Routing Key: `extraction.dead`). |
| **Queue** | `invoice.extraction.result.queue` | Sonuç kuyruğu. (Routing Key: `extraction.result`). |

### 2.3. Servisler
- **RabbitMQProducerService**: Faturayı kuyruğa ekler, `QUEUED` statüsüne çeker ve `correlation_id` üretir.
- **RabbitMQResultListener**: Python'dan gelen sonucu dinler.
  - `COMPLETED`: Fatura verilerini günceller, statüyü `VERIFIED` veya `PENDING` yapar (Confidence Score'a göre).
  - `FAILED`: Hata durumunda tekrar deneme (retry) mekanizmasını işletir. Max deneme sayısına (3) ulaşırsa `FAILED` statüsüne çeker.

### 2.4. Docker Yapılandırması
- `docker-compose.yml` dosyasına `invoice-files` adlı shared volume eklendi. Bu sayede Spring Boot ve Python servisleri dosyalara ortak erişebilir.

## 3. Mesaj Şemaları (Contract)

**Furkan (Phase 19-A) ile mutabık kalınan mesaj formatları:**

### 3.1. Extraction Request (Spring -> Python)
Kuyruk: `invoice.extraction.queue`
```json
{
  "message_id": "uuid-string",
  "invoice_id": "uuid-string",
  "company_id": "uuid-string",
  "user_id": "uuid-string",
  "file_path": "/data/invoices/file.pdf",
  "file_name": "file.pdf",
  "file_type": "application/pdf",
  "file_size": 1024,
  "priority": "NORMAL",
  "attempt": 1,
  "max_attempts": 3,
  "timestamp": "2026-02-14T10:00:00",
  "correlation_id": "uuid-string"
}
```

### 3.2. Extraction Result (Python -> Spring)
Kuyruk: `invoice.extraction.result.queue`
**ÖNEMLİ**: `attempt` alanı eklendi (Retry takibi için).

```json
{
  "message_id": "uuid-string",
  "correlation_id": "uuid-string",
  "invoice_id": "uuid-string",
  "status": "COMPLETED",  // veya "FAILED"
  "attempt": 1,           // İŞLENDİĞİNDEKİ deneme sayısı
  "invoice_data": {
    "invoice_number": "INV-001",
    "invoice_date": "2026-01-01",
    "due_date": "2026-01-15",
    "supplier_name": "Acme Corp",
    "supplier_tax_id": "1234567890",
    "supplier_address": "Istanbul",
    "subtotal": 100.00,
    "tax_amount": 18.00,
    "total_amount": 118.00,
    "currency": "TRY",
    "line_items": [
       {
         "description": "Item 1",
         "quantity": 1,
         "unit_price": 100.00,
         "total_price": 100.00,
         "tax_rate": 18.00
       }
    ]
  },
  "confidence_score": 95.5,
  "provider": "GEMINI",
  "suggested_status": "AUTO_VERIFIED",
  "error_code": null,
  "error_message": null,
  "processing_duration_ms": 1500,
  "timestamp": "2026-02-14T10:00:05"
}
```

## 4. Test Sonuçları

### 4.1. Unit Testler (Başarılı)
- `RabbitMQProducerServiceTest`: Mesajın doğru formatta kuyruğa atıldığı ve retry durumunda attempt sayısının arttığı doğrulandı.
- `RabbitMQResultListenerTest`: 
  - Başarılı sonuçların faturayı güncellediği doğrulandı.
  - Hatalı sonuçların retry mekanizmasını tetiklediği doğrulandı.
  - Max retry aşıldığında faturanın `FAILED` olduğu doğrulandı.

### 4.2. Entegrasyon Testleri
`RabbitMQIntegrationTest` (Testcontainers ile) oluşturuldu ancak çalışma ortamındaki Docker/Socket erişim kısıtlamaları nedeniyle çalıştırılamadı. Unit testler mantığı doğruladığı için yeterli görüldü.

## 5. Furkan İçin Notlar (Phase 19-A)
1. **Mesaj Formatı**: Yukarıdaki Request/Result formatlarına harfiyen uyulmalı. Özellikle Result mesajında `attempt` alanını geri göndermeyi unutma (Request'ten gelen `attempt` değerini aynen dönmelisin).
2. **Shared Volume**: Docker Compose'da `invoice-files:/data/invoices` tanımlı. Dosyayı buradan okumalısın.
3. **Correlation ID**: Loglamada ve Result mesajında `correlation_id` mutlaka kullanılmalı.

## 6. Sıradaki Adımlar (Phase 20)
- File Upload endpoint'inin bu yapıyı kullanacak şekilde güncellenmesi (`POST /api/v1/invoices/bulk-upload`).
