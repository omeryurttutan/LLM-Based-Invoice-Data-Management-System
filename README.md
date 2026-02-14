# Fatura OCR ve Veri Yönetim Sistemi

![CI Pipeline](https://github.com/MFurkanAkdag/Fatura-OCR/actions/workflows/ci.yml/badge.svg)
![Backend](https://github.com/MFurkanAkdag/Fatura-OCR/actions/workflows/ci.yml/badge.svg?branch=main&event=push&label=Backend)

## Proje Hakkında

Fatura verilerinin OCR ve LLM teknolojileri kullanılarak otomatik işlenmesini sağlayan, muhasebe süreçlerini hızlandıran modern bir web uygulamasıdır.

## Teknoloji Stack'i

- **Backend**: Spring Boot 3.2 (Java 17)
- **Frontend**: Next.js 14 (TypeScript)
- **Extraction**: FastAPI (Python 3.11)
- **Data**: PostgreSQL 15, Redis 7
- **Queue**: RabbitMQ 3

## Geliştirme Ortamı Kurulumu

### Gereksinimler

- Java 17
- Node.js 20+
- Python 3.11+
- Docker & Docker Compose

### Hızlı Başlangıç (Docker ile)

1. `.env.example` dosyasını `.env` olarak kopyalayın
2. `docker-compose up -d --build` komutunu çalıştırın
3. Servislerin başlamasını bekleyin

### Servis URL Tablosu

Aşağıdaki host portları, mevcut diğer projelerle çakışmayı önlemek için özel olarak ayarlanmıştır.

| Servis             | Lokal URL              | Docker Internal |
| ------------------ | ---------------------- | --------------- |
| Backend            | http://localhost:8082  | 8080            |
| Frontend           | http://localhost:3001  | 3000            |
| Extraction Service | http://localhost:8001  | 8000            |
| PostgreSQL         | localhost:5436         | 5432            |
| Redis              | localhost:6380         | 6379            |
| RabbitMQ (AMQP)    | localhost:5673         | 5672            |
| RabbitMQ UI        | http://localhost:15673 | 15672           |

## Proje Yapısı

- `backend/`: Spring Boot uygulaması
- `frontend/`: Next.js web arayüzü
- `extraction-service/`: Python tabanlı OCR/LLM servisi
- `docs/`: Proje dokümantasyonu

## Ekip

- Muhammed Furkan Akdağ (AI/LLM)
- Ömer Talha Yurttutan (Web)

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

| Katman      | Teknolojiler                                             |
| ----------- | -------------------------------------------------------- |
| Backend     | Java 17, Spring Boot 3, Spring Security, JPA, PostgreSQL |
| LLM Servisi | Python 3.11, FastAPI, Gemini, GPT-5.2, Claude            |
| Frontend    | Next.js 14, React 19, TypeScript, Tailwind, Shadcn       |
| Messaging   | RabbitMQ                                                 |
| Cache       | Redis                                                    |
| DevOps      | Docker, GitHub Actions                                   |

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
