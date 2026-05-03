# Fatura OCR ve Veri Yönetim Sistemi - Yönetici Kılavuzu

Bu kılavuz, sistem yöneticilerine (ADMIN) özel fonksiyonları ve sistem yönetimini açıklar.

## İçindekiler

1. [kullanıcı Yönetimi](#1-kullanıcı-yönetimi)
2. [Sistem İzleme (Monitoring)](#2-sistem-izleme-monitoring)
3. [Denetim Günlükleri (Audit Log)](#3-denetim-günlükleri-audit-log)
4. [KVKK Uyumluluğu](#4-kvkk-uyumluluğu)
5. [Otomasyon Kuralları Yönetimi](#5-otomasyon-kuralları-yönetimi)
6. [Yedekleme ve Kurtarma](#6-yedekleme-ve-kurtarma)

---

## 1. Kullanıcı Yönetimi

Yöneticiler, şirkete bağlı tüm kullanıcıları görüntüleyebilir ve yetkilerini düzenleyebilir.

### Kullanıcıları Görüntüleme

- "Ayarlar > Kullanıcı Yönetimi" sayfasına gidin.
- Burada kayıtlı tüm kullanıcıların listesini, e-posta adreslerini ve mevcut rollerini görebilirsiniz.

### Roller ve Yetkiler

| Rol            | Açıklama                                                                          |
| -------------- | --------------------------------------------------------------------------------- |
| **ADMIN**      | Tam yetki. Kullanıcı yönetimi, sistem ayarları, KVKK, izleme.                     |
| **MANAGER**    | Fatura yönetimi, kural oluşturma, dışa aktarma, denetim kaydı izleme.             |
| **ACCOUNTANT** | Fatura işlemleri, doğrulama, dışa aktarma. Admin paneline erişemez.               |
| **INTERN**     | Sadece okuma ve fatura yükleme yetkisi. Düzenleme, silme ve dışa aktarma yapamaz. |

### Rol Değiştirme ve Pasife Alma

- Bir kullanıcının yanındaki "Düzenle" butonuna tıklayarak rolünü değiştirebilirsiniz.
- İşten ayrılan bir personeli "Pasif" duruma getirerek sisteme girişini engelleyebilirsiniz.

---

## 2. Sistem İzleme (Monitoring)

Sistemin sağlık durumunu görsel olarak takip edebilirsiniz.

### Sistem Sağlık Sayfası

- Admin paneli üzerinden "Sistem Durumu" sayfasına erişin.
- **Servisler:** Backend, Python Çıkarım Servisi, Veritabanı, Redis ve RabbitMQ servislerinin durumunu (UP/DOWN) anlık olarak görebilirsiniz.
- **Metrikler:** Ortalama yanıt süreleri, önbellek kullanım oranları gibi performans verilerini inceleyebilirsiniz.

### Ne Yapmalı?

- Eğer bir servis **DOWN** (Kırmızı) görünüyorsa, teknik ekibe veya `deployment-runbook.md` dosyasına başvurun.

---

## 3. Denetim Günlükleri (Audit Log)

Güvenlik ve takip amacıyla sistemdeki kritik işlemler kayıt altına alınır.

### Günlüklere Erişim

- "Ayarlar > Denetim Günlükleri" sayfasına gidin.
- Burada kimin, ne zaman, hangi işlemi yaptığını (Örn: "Ahmet Yılmaz, Fatura #123'ü sildi") görebilirsiniz.

### Filtreleme

- Belirli bir tarih aralığı, kullanıcı veya işlem türüne (Ekleme, Silme, Güncelleme) göre filtreleme yaparak aradığınız kaydı kolayca bulabilirsiniz.

---

## 4. KVKK Uyumluluğu

Sistem, kişisel verilerin korunması kanununa (KVKK) uyumlu olarak tasarlanmıştır.

### Veri Şifreleme

TC Kimlik Numarası, Telefon, Adres gibi hassas kişisel veriler veritabanında şifreli olarak saklanır.

### Unutulma Hakkı

- Bir kullanıcı verilerinin silinmesini talep ederse, "KVKK Merkezi" sayfasından ilgili kullanıcıyı bularak "Anonimleştir" işlemini başlatabilirsiniz.
- Bu işlem kullanıcının kişisel verilerini geri döndürülemez şekilde sistemden siler, ancak fatura istatistikleri (tutarlar vb.) bozulmadan kalır.

### Açık Rıza Takibi

- Kullanıcıların kayıt olurken verdikleri onayı ve tarihini bu panelden görüntüleyebilirsiniz.

---

## 5. Otomasyon Kuralları Yönetimi

Kullanıcıların oluşturduğu kuralları denetleyebilir ve sistem genelinde geçerli kurallar tanımlayabilirsiniz.

- **Kural Hataları:** Hatalı çalışan veya sistemi yavaşlatan kuralları bu ekrandan devre dışı bırakabilirsiniz.
- **Kural Geçmişi:** Hangi kuralın hangi faturada çalıştığını ve ne sonuç ürettiğini loglardan inceleyebilirsiniz.

---

## 6. Yedekleme ve Kurtarma

Veri güvenliği için otomatik yedekleme sistemleri devrededir.

### Otomatik Yedekler

- Sistem her gece otomatik olarak veritabanı yedeği alır.

### Manuel Yedekleme ve Geri Yükleme

- Acil durumlarda manuel yedek almak veya eski bir yedeğe dönmek için teknik dokümantasyona (`docs/deployment/deployment-runbook.md`) başvurmanız gerekmektedir.
- **Dikkat:** Geri yükleme işlemi, mevcut verilerin üzerine yazar. Sadece veri kaybı durumunda yetkili teknik personel tarafından yapılmalıdır.
