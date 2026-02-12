# Fatura OCR ve Veri Yönetim Sistemi (Phase 0)

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

| Servis | Lokal URL | Docker Internal |
|--------|-----------|-----------------|
| Backend | http://localhost:8082 | 8080 |
| Frontend | http://localhost:3001 | 3000 |
| Extraction Service | http://localhost:8001 | 8000 |
| PostgreSQL | localhost:5436 | 5432 |
| Redis | localhost:6380 | 6379 |
| RabbitMQ (AMQP) | localhost:5673 | 5672 |
| RabbitMQ UI | http://localhost:15673 | 15672 |

## Proje Yapısı
- `backend/`: Spring Boot uygulaması
- `frontend/`: Next.js web arayüzü
- `extraction-service/`: Python tabanlı OCR/LLM servisi
- `docs/`: Proje dokümantasyonu

## Ekip
- Muhammed Furkan Akdağ (AI/LLM)
- Ömer Talha Yurttutan (Web)
