# Faz 29: Fatura Versiyon Geçmişi ve Geri Alma (Backend)

## Tamamlanan Maddeler

### 1. Veritabanı Şeması
- `invoice_versions` tablosu oluşturuldu.
- `invoice_id` foreign key `invoices(id)` ile ilişkilendirildi (UUID kullanılarak).
- `snapshot_data` ve `items_snapshot` kolonları JSONB olarak tanımlandı.
- `change_source`, `change_summary`, `changed_fields` kolonları eklendi.
- `changed_by_user_id` foreign key `users(id)` ile ilişkilendirildi.
- Performans için gerekli indeksler eklendi (`idx_invoice_versions_invoice`, `idx_invoice_versions_invoice_version`).

### 2. Domain Entity ve Repository
- `InvoiceVersion` entity sınıfı oluşturuldu.
- `InvoiceVersionRepository` arayüzü oluşturuldu ve gerekli sorgu metotları eklendi (`findMaxVersionNumberByInvoiceId`, `findVersionsByInvoiceId`).

### 3. Servis Katmanı
- `InvoiceVersionService` arayüzü ve `InvoiceVersionServiceImpl` sınıfı geliştirildi.
- **Snapshot Oluşturma (`createSnapshot`)**: Fatura her güncellendiğinde mevcut halini JSON olarak saklayan yapı kuruldu.
- **Versiyon Temizleme (`cleanupOldVersions`)**: Fatura başına maksimum 50 versiyon sınırı getirildi, eskiler otomatik siliniyor.
- **Versiyon Karşılaştırma (`compareVersions`)**: İki versiyon arasındaki farkları (değişen alanlar) hesaplayan mantık eklendi.
- **Geri Alma (`revertToVersion`)**: Geçmiş bir versiyondaki veriyi tekrar `Invoice` nesnesine dönüştüren yapı kuruldu.

### 4. Entegrasyon
- `InvoiceService` içerisine `createSnapshot` çağrıları eklendi:
    - Manuel güncelleme öncesi (`updateInvoice`)
    - Doğrulama öncesi (`verifyInvoice`)
    - Reddetme öncesi (`rejectInvoice`)
- `InvoiceService` içine `revertInvoice` metodu eklendi. Bu metod:
    - Mevcut durumun snapshot'ını alır (Geri alma işlemi olarak).
    - Hedef versiyondaki verileri mevcut faturaya uygular.
    - Veritabanına kaydeder.

### 5. API Endpointleri
- `InvoiceVersionController` oluşturuldu.
- `GET /api/v1/invoices/{id}/versions`: Versiyon listesi.
- `GET /api/v1/invoices/{id}/versions/{versionNumber}`: Versiyon detayı.
- `GET /api/v1/invoices/{id}/versions/diff`: Versiyon karşılaştırma.
- `POST /api/v1/invoices/{id}/revert/{versionNumber}`: Versiyona geri dönme (ADMIN/MANAGER yetkisi ile).

## Testler
- `InvoiceVersionServiceImplTest` birim testleri oluşturuldu.
- Snapshot oluşturma, versiyon listeleme ve eski versiyon temizleme fonksiyonları test edildi ve başarıyla geçti.

## Notlar
- Veritabanı migrasyonunda `UUID` type mismatch sorunu düzeltildi.
- `Invoice` entity'sinde `companyId` kullanımı ile `InvoiceVersion` uyumlu hale getirildi.
- `User` entity referansı `userRepository.findById` ile güvenli şekilde sağlandı.
