# Fatura OCR ve Veri Yönetim Sistemi - Kullanıcı Kılavuzu

Bu kılavuz, Fatura OCR sistemini günlük işlerinizde verimli bir şekilde kullanmanız için hazırlanmıştır.

## İçindekiler

1. [Giriş ve Başlarken](#1-giriş-ve-başlarken)
2. [Pano (Dashboard)](#2-pano-dashboard)
3. [Fatura Yönetimi](#3-fatura-yönetimi)
4. [Dosya Yükleme ve Akıllı Çıkarım](#4-dosya-yükleme-ve-akıllı-çıkarım)
5. [Filtreleme ve Arama](#5-filtreleme-ve-arama)
6. [Dışa Aktarma (Export)](#6-dışa-aktarma-export)
7. [Bildirimler](#7-bildirimler)
8. [Versiyon Geçmişi](#8-versiyon-geçmişi)
9. [Otomasyon Kuralları](#9-otomasyon-kuralları)
10. [Tedarikçi Şablonları](#10-tedarikçi-şablonları)

---

## 1. Giriş ve Başlarken

### Sistem Hakkında

Fatura OCR ve Veri Yönetim Sistemi, işletmenizin fatura işleme süreçlerini otomatize eden yapay zeka destekli bir platformdur. Fatura görsellerinden (JPEG, PNG, PDF) otomatik veri çıkarımı yapar, hataları doğrulamanıza olanak tanır ve muhasebe programlarınıza (Logo, Mikro, Netsis, Luca) uyumlu çıktılar üretir.

### Kimler İçin?

Bu sistem, muhasebe departmanları, finans ekipleri ve işletme yöneticileri içindir. Manuel veri girişini azaltarak zamandan tasarruf etmenizi ve hataları en aza indirmenizi sağlar.

### Gereksinimler

- **Tarayıcı:** Google Chrome, Mozilla Firefox veya Microsoft Edge'in güncel sürümleri önerilir.
- **Mobil Erişim:** Sisteme mobil cihazlarınızdan da erişebilirsiniz. Ana ekrana ekleyerek uygulama gibi kullanabilirsiniz.

### Hesap Oluşturma ve Giriş

1. **Kayıt Ol:** Giriş ekranındaki "Kayıt Ol" butonuna tıklayın. Ad, soyad, e-posta ve şifrenizi girin.
2. **Şirket Seçimi:** Kayıt olduktan sonra mevcut bir şirkete katılabilir veya yeni bir şirket oluşturabilirsiniz.
3. **Giriş Yap:** E-posta ve şifrenizle sisteme giriş yapın.

---

## 2. Pano (Dashboard)

Sisteme giriş yaptığınızda sizi karşılayan ana ekrandır. İşletmenizin durumunu bir bakışta görmenizi sağlar.

- **Özet Kartları:**
  - **Toplam Fatura:** Sisteme yüklenen tüm faturaların sayısı.
  - **Toplam Tutar:** Tüm faturaların toplam parasal değeri.
  - **Bekleyen İşlem:** Doğrulama veya onay bekleyen fatura sayısı (En önemlisi burasıdır).
  - **Doğrulanan:** İşlemi tamamlanmış fatura sayısı.

- **Grafikler:**
  - **Kategori Dağılımı:** Harcamalarınızın hangi kategorilerde (Gıda, Teknoloji, Ulaşım vb.) yoğunlaştığını gösteren pasta grafik.
  - **Aylık Trend:** Fatura hacminin aylara göre değişimini gösteren çizgi grafik.
  - **Top Tedarikçiler:** En çok iş yaptığınız tedarikçileri gösteren sütun grafik.

- **Bekleyen İşlemler Listesi:** Onayınızı bekleyen faturaların kısa bir listesidir. Buradan hızlıca işlem yapabilirsiniz.

---

## 3. Fatura Yönetimi

Faturalarınızın listelendiği, düzenlendiği ve yönetildiği ana bölümdür.

### Fatura Listesi

- **Sıralama:** Tablo başlıklarına tıklayarak (Tarih, Tutar, Tedarikçi) sıralama yapabilirsiniz.
- **Durum Rozetleri:**
  - `Onay Bekliyor` (Sarı): Fatura işlendi ancak henüz doğrulanmadı. Gözden geçirmeniz gerekiyor.
  - **Doğrulandı** (Yeşil): Fatura onaylandı ve muhasebeleşmeye hazır.
  - **Reddedildi** (Kırmızı): Fatura hatalı veya geçersiz olarak işaretlendi.

### Manuel Fatura Ekleme

Eğer elinizde dijital bir görsel yoksa, "Yeni Fatura" butonuna tıklayıp "Manuel Giriş" seçeneği ile faturayı elle sisteme girebilirsiniz.

### Fatura Detayı ve Düzenleme

Bir faturaya tıkladığınızda detay sayfasına gidersiniz. Burada faturanın tüm bilgilerini görebilir, eksik veya hatalı alanları düzeltebilirsiniz.

- **Düzenleme:** Kalem ikonuna tıklayarak veya doğrudan alanın üzerine tıklayarak düzenleme yapabilirsiniz.
- **Silme:** Yetkiniz varsa, faturayı silebilirsiniz. Silinen faturalar "Çöp Kutusu"na gider, tamamen kaybolmaz (Admin kurtarabilir).

---

## 4. Dosya Yükleme ve Akıllı Çıkarım

Sistemin en güçlü özelliği olan yapay zeka ile veri çıkarımıdır.

### Tekli ve Çoklu Yükleme

- **Desteklenen Dosyalar:** JPEG, PNG, PDF ve e-Fatura XML.
- **Nasıl Yüklenir:** "Fatura Yükle" butonuna tıklayın veya dosyaları sürükleyip bırakın. Aynı anda 20 dosyaya kadar yükleme yapabilirsiniz.
- **İşlem Süreci:** Dosyalar yüklendikten sonra sistem arka planda çalışır. İşlem bittiğinde size bildirim gelir.

### Çıkarım Sonuçlarını Anlama

Fatura detay sayfasında ekran ikiye bölünür:

- **Sol Taraf:** Yüklediğiniz faturanın orijinal görüntüsü.
- **Sağ Taraf:** Yapay zekanın okuduğu veriler.
- **Güven Skoru:** Her alanın yanında bir renk görürsünüz.
  - 🟢 **Yeşil:** Sistem çok emin (%90+).
  - 🟡 **Sarı:** Sistem emin değil, kontrol etmelisiniz.
  - 🔴 **Kırmızı:** Sistem okuyamadı veya düşük güvenle okudu.

### Doğrulama (Verifikasyon)

Yapay zeka %100 hatasız değildir. Lütfen sarı ve kırmızı alanları kontrol edin.

1. Fatura görseli ile sağdaki verileri karşılaştırın.
2. Hatalı bir yer varsa tıklayıp düzeltin.
3. Her şey doğruysa "Doğrula" butonuna basın. Bu işlem faturayı "Doğrulanmış" statüsüne alır ve muhasebe ihracatına hazır hale getirir.

---

## 5. Filtreleme ve Arama

Binlerce fatura arasından aradığınızı bulmak için güçlü filtreleme özelliklerini kullanın.

- **Filtre Paneli:** Listenin sağ üst köşesindeki filtre ikonuna tıklayarak paneli açın.
- **Filtre Seçenekleri:**
  - **Tarih Aralığı:** Belirli iki tarih arasını seçin.
  - **Durum:** Sadece "Onay Bekleyenler"i veya "Doğrulananlar"ı görebilirsiniz.
  - **Tedarikçi:** Tedarikçi adına göre arama.
  - **Kategori:** Harcama kategorisine göre filtreleme.
  - **Tutar:** Min ve Max tutar girerek aralık belirleme.
- **Arama Çubuğu:** Fatura numarası, vergi numarası veya tedarikçi adını yazarak hızlı arama yapabilirsiniz.

---

## 6. Dışa Aktarma (Export)

Verilerinizi başka sistemlerde kullanmak için dışa aktarabilirsiniz. Dışa aktarma işlemi genellikle sadece **Doğrulanmış** faturalar için yapılır.

1. "Dışa Aktar" butonuna tıklayın.
2. Format seçin:
   - **Excel (XLSX):** Genel raporlama ve analiz için.
   - **CSV:** Başka yazılımlara veri aktarmak için.
   - **Muhasebe Formatları:**
     - **Logo:** Logo muhasebe programı için XML/Excel formatı.
     - **Mikro:** Mikro yazılımı için uygun format.
     - **Netsis:** Netsis entegrasyonu için.
     - **Luca:** Luca mali müşavir programı için.

---

## 7. Bildirimler

Sistemdeki önemli olaylardan haberdar olursunuz.

- **Zil İkonu:** Sağ üstteki zil ikonunda kırmızı bir sayı varsa okunmamış bildiriminiz var demektir.
- **Bildirim Türleri:**
  - Fatura işleme tamamlandı.
  - İşlem başarısız oldu (dosya bozuk vb.).
  - Düşük güven skoru (dikkat etmeniz gereken fatura).
  - Başka bir kullanıcı bir faturayı doğruladı.
- **Ayarlar:** Profil menüsünden "Ayarlar"a giderek hangi bildirimleri e-posta veya uygulama içi almak istediğinizi seçebilirsiniz.

---

## 8. Versiyon Geçmişi

Her faturanın bir tarihçesi tutulur. Kimin, ne zaman, hangi değişikliği yaptığını görebilirsiniz.

- Fatura detay sayfasında "Geçmiş" sekmesine tıklayın.
- Yapılan değişiklikleri (Eski değer -> Yeni değer) detaylıca inceleyebilirsiniz.
- Hatalı bir değişiklik yapıldıysa (yetkiniz varsa) eski bir versiyona geri dönebilirsiniz.

---

## 9. Otomasyon Kuralları (Yöneticiler İçin)

Tekrar eden işleri sisteme yaptırabilirsiniz.

- **Örnek Kural:** "Eğer tedarikçi 'Turkcell' ise, kategoriyi 'İletişim' yap."
- **Örnek Kural:** "Tutar 50.000 TL'den büyükse, 'Yönetici Onayı' etiketini ekle."
- Kurallar "Ayarlar > Otomasyon Kuralları" menüsünden yönetilir.

---

## 10. Tedarikçi Şablonları

Siz faturaları doğruladıkça sistem öğrenir.

- Örneğin, 'Migros' faturalarını sürekli 'Gıda' olarak işaretliyorsanız, sistem bunu öğrenir ve bir sonraki 'Migros' faturasında kategoriyi otomatik olarak 'Gıda' seçer.
- Bu öğrenilen bilgileri "Tedarikçi Şablonları" sayfasından görüntüleyebilir ve düzenleyebilirsiniz.
