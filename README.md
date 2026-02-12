# 🧾 Fatura OCR ve Veri Yönetim Sistemi

LLM tabanlı görüntü analizi ile fatura verilerini otomatik çıkaran, kurumsal ölçekte tasarlanmış modern bir muhasebe otomasyon sistemi.

Bu proje, geleneksel OCR + regex + post-processing yaklaşımı yerine, doğrudan görüntüden yapılandırılmış veri çıkarımı yapan **LLM tabanlı hibrit bir mimari** sunar.

Sistem; fatura görsellerini (PDF, JPEG, PNG) ve GİB e-Fatura XML dosyalarını işleyerek muhasebe süreçlerindeki manuel veri girişini %80+ oranında azaltmayı hedefler.

---

## 🚀 Temel Yaklaşım

> Görüntüyü OCR’a çevir, regex ile parçala, düzelt, doğrula… ❌  
> Görüntüyü LLM’e gönder, yapılandırılmış JSON al… ✅

Bu projede fatura analizi için klasik OCR pipeline'ı yerine:

**Gemini 3 Flash → GPT-5.2 → Claude Haiku 4.5** fallback zincirine sahip  
LLM tabanlı veri çıkarım servisi kullanılmıştır.

Bu sayede:
- Farklı fatura formatlarına otomatik adaptasyon
- Çok daha düşük geliştirme maliyeti
- Daha yüksek doğruluk oranı
- Sürekli iyileştirilebilir prompt tabanlı sistem

---

## 🏗️ Sistem Mimarisi

Hibrit mimari yaklaşımı:

- **Spring Boot (Java 17)** → Ana iş mantığı (Modüler Monolit, Hexagonal Architecture)
- **Python FastAPI** → LLM tabanlı veri çıkarım mikroservisi
- **Next.js 14 PWA** → Web + Mobil uyumlu kullanıcı arayüzü
- **PostgreSQL + Redis + RabbitMQ**
- Docker container'ları ile ayrık deploy edilebilir servisler



---

## ✨ Öne Çıkan Özellikler

- 📤 Tekli ve toplu fatura yükleme (PDF, PNG, JPEG, XML)
- 🤖 LLM ile otomatik veri çıkarımı
- 🧠 Güven skoru algoritması ve manuel doğrulama kuyruğu
- 🧾 E-Fatura XML parse desteği (LLM çağrısı olmadan)
- 🗂️ Gelişmiş filtreleme, arama ve raporlama
- 📊 Dashboard ve veri görselleştirme
- 📁 XLSX, CSV ve muhasebe yazılımı formatlarında export (Logo, Mikro, Netsis, Luca)
- 🔔 Gerçek zamanlı bildirim sistemi
- 🧾 Audit log ve versiyon geçmişi
- 🔐 JWT, RBAC, KVKK uyumlu güvenlik mimarisi
- 📱 Progressive Web App (PWA) desteği

---

## 🧩 Teknoloji Yığını

| Katman | Teknolojiler |
|--------|---------------|
| Backend | Java 17, Spring Boot 3, Spring Security, JPA, PostgreSQL |
| LLM Servisi | Python 3.11, FastAPI, Gemini, GPT-5.2, Claude |
| Frontend | Next.js 14, React 19, TypeScript, Tailwind, Shadcn |
| Messaging | RabbitMQ |
| Cache | Redis |
| DevOps | Docker, GitHub Actions |

---

## 🧠 Neden LLM Tabanlı Yaklaşım?

Geleneksel OCR sistemlerinde:
- Yüzlerce satır regex
- Sürekli format bakımı
- Karmaşık doğrulama katmanları

Bu projede:
- Tek API çağrısı
- JSON schema validation
- Prompt optimizasyonu ile sürekli gelişim

---

## 🔒 Güvenlik

- JWT tabanlı stateless authentication
- RBAC yetkilendirme
- BCrypt şifreleme (strength 12)
- AES-256 veri şifreleme
- KVKK uyumlu veri saklama
- Soft delete + audit log

---

## 📦 Deployment

Tüm servisler Docker container'ları içinde çalışır.  
CI/CD GitHub Actions ile otomatikleştirilmiştir.

- Frontend: Vercel
- Backend & LLM Service: Dockerized deployment

---

## 🎯 Proje Hedefi

Muhasebe ofislerinde saatler süren manuel fatura girişini saniyelere indiren,  
ölçeklenebilir ve kurumsal düzeyde bir otomasyon altyapısı sunmak.

---

## 👨‍💻 Geliştiriciler

- Muhammed Furkan Akdağ
- Ömer Talha Yurttutan
