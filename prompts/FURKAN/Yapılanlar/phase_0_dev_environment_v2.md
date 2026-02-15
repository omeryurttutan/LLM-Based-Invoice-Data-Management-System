# PHASE 0: DEVELOPMENT ENVIRONMENT SETUP (GÜNCELLENMIŞ)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis, eliminating manual data entry for accounting offices.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - Backend: Spring Boot 3.2+ (Hexagonal Architecture)
  - Frontend: Next.js 14+ (PWA)
  - Extraction Service: Python FastAPI (LLM Integration)
- **Database**: PostgreSQL 15+
- **Message Queue**: RabbitMQ 3.x
- **Cache**: Redis 7.x

### LLM Strategy
The system uses a fallback chain for invoice data extraction:
1. **Primary**: Gemini 3 
2. **Fallback 1**: GPT-5.2
3. **Fallback 2**: Claude Haiku 4.5

### Current State
This is **Phase 0** - the very first phase. No code exists yet. We are setting up the complete development environment from scratch.

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer) — Bu faz normalde Ömer'e aittir ancak port konfigürasyonu nedeniyle Furkan tarafından yapılmaktadır.
- **Estimated Duration**: 2-3 days

---

## ÖNEMLİ: PORT KONFİGÜRASYONU

Ekip üyelerinin bilgisayarlarında başka Docker projeleri çalışmaktadır. Varsayılan portlarla çakışma yaşanmaması için bu projede aşağıdaki **özel port haritası** kullanılacaktır.

### Mevcut Port Çakışma Analizi

Ekipteki mevcut Docker container'larında kullanılan portlar:
- **5432**: tazemektup-postgres, meyvesepeti-db, meyvesepeti-postgres, s4e_postgres
- **5434**: gym-db
- **5435**: seffaf_bagis_postgres
- **6379**: seffaf_bagis_redis, meyvesepeti-redis
- **5672**: s4e_rabbitmq (AMQP)
- **15672**: s4e_rabbitmq (Management UI)
- **8080**: gym-management-backend, pgadmin4-container
- **8081**: meyvesepeti-auth
- **8089**: seffaf_bagis_backend
- **3000**: seffaf_bagis_frontend
- **8000**: s4e_api
- **5173**: gym-management-frontend, s4e_frontend

### Fatura OCR Projesi Port Haritası

Aşağıdaki portlar tüm mevcut projelerle çakışmayan, güvenli portlardır:

| Servis | Container İç Port | Host Port (Dışarıya Açılan) | docker-compose mapping |
|--------|-------------------|----------------------------|----------------------|
| PostgreSQL | 5432 | **5436** | 5436:5432 |
| Redis | 6379 | **6380** | 6380:6379 |
| RabbitMQ (AMQP) | 5672 | **5673** | 5673:5672 |
| RabbitMQ (Management UI) | 15672 | **15673** | 15673:15672 |
| Backend (Spring Boot) | 8080 | **8082** | 8082:8080 |
| Frontend (Next.js) | 3000 | **3001** | 3001:3000 |
| Extraction Service (FastAPI) | 8000 | **8001** | 8001:8000 |

**ÖNEMLİ NOTLAR:**
- Container içi portlar değişmez. Servisler arası iletişim Docker network üzerinden container iç portlarıyla gerçekleşir (örn: backend, postgres'e 5432 portundan bağlanır).
- Sadece host (bilgisayar) tarafından erişilen portlar değiştirilmiştir.
- Tüm konfigürasyon dosyalarında (application.yml, .env.example, README.md, CORS ayarları vb.) bu yeni host portları kullanılmalıdır.

---

## ÖNEMLİ: İKİ AŞAMALI KURULUM STRATEJİSİ

Bu faz iki alt aşamada gerçekleştirilecektir:

### AŞAMA A — Proje İskelet Yapısı (Docker'sız)
Önce tüm proje yapısı, kaynak kodlar, konfigürasyon dosyaları ve iskelet projeler oluşturulacaktır. Bu aşamada Docker ile ilgili hiçbir şey yapılmaz. Servisler lokal olarak (IDE üzerinden veya terminal ile) çalıştırılabilir durumda olmalıdır.

### AŞAMA B — Docker Entegrasyonu (En Son)
Tüm iskelet projeler hazır ve hatasız derlenebilir/çalışabilir olduktan sonra, en son adım olarak Docker dosyaları (Dockerfile'lar ve docker-compose.yml) oluşturulacak ve test edilecektir.

**Neden bu sıralama?** Geliştirme sürecinde sürekli Docker build/restart yapmak zaman kaybıdır. Önce projeler lokalde sağlam çalışsın, sonra Docker'a sarılsın.

---

## OBJECTIVE

Set up a complete development environment that allows all team members to work in identical conditions. The setup will be done in two stages: first create all project skeletons and configurations that work locally, then wrap everything with Docker for containerized deployment.

---

## AŞAMA A: PROJE İSKELET YAPISI (DOCKER'SIZ)

### A1. Project Root Structure

**Purpose**: Create a well-organized monorepo structure that clearly separates concerns.

**Directory Structure to Create**:

fatura-ocr-system/ dizini altında şu klasör ve dosyalar oluşturulacaktır:
- backend/ — Spring Boot uygulaması
- frontend/ — Next.js uygulaması
- extraction-service/ — Python FastAPI servisi
- docs/FURKAN/step_results/ — Furkan'ın faz sonuç dosyaları
- docs/OMER/step_results/ — Ömer'in faz sonuç dosyaları
- prompts/FURKAN/ — Furkan'ın promptları
- prompts/OMER/ — Ömer'in promptları
- docker-compose.yml — (Aşama B'de oluşturulacak)
- docker-compose.override.yml — (Aşama B'de oluşturulacak)
- .env.example
- .gitignore
- README.md

---

### A2. Backend Project Skeleton (Spring Boot)

**Purpose**: Create the initial Spring Boot project structure that compiles and runs locally.

**Directory**: backend/

#### A2.1 Project Configuration

- **Build Tool**: Maven (pom.xml)
- **Java Version**: 17
- **Spring Boot Version**: 3.2.x
- **Dependencies**: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, spring-boot-starter-security, spring-boot-starter-actuator, postgresql (runtime), flyway-core, spring-boot-starter-data-redis, spring-boot-starter-amqp, springdoc-openapi-starter-webmvc-ui (2.3.0), spring-boot-devtools (optional, runtime), lombok (optional), spring-boot-starter-test, testcontainers (postgresql, rabbitmq)

#### A2.2 Package Structure

src/main/java/com/faturaocr/ dizini altında:
- FaturaOcrApplication.java — Main class
- domain/ — Domain layer (boş)
- application/ — Application layer (boş)
- infrastructure/ — Infrastructure layer (boş)
- interfaces/rest/HealthController.java — Basic health endpoint

#### A2.3 Configuration File (application.yml)

application.yml dosyasında şu ayarlar yapılmalıdır:
- spring.application.name: fatura-ocr-backend
- spring.profiles.active: dev (environment variable'dan okunacak)
- server.port: 8080 (container iç port, değişmez)
- Dev profili altında datasource URL: jdbc:postgresql://localhost:5436/fatura_ocr_db (lokal geliştirme için host port 5436 kullanılacak)
- datasource username: fatura_user (environment variable'dan okunacak)
- datasource password: fatura_secret_2026 (environment variable'dan okunacak)
- JPA hibernate ddl-auto: validate, show-sql: true, format_sql: true
- Flyway enabled: true, locations: classpath:db/migration
- Redis host: localhost, port: 6380 (lokal geliştirme için host port)
- RabbitMQ host: localhost, port: 5673 (lokal geliştirme için host port), username: fatura_mq, password: mq_secret_2026
- Actuator endpoints: health, info, metrics exposed, health show-details: always

**NOT:** Docker environment'ta çalışırken bu değerler environment variable'lar ile override edilecektir (Aşama B'de). Lokal geliştirmede ise host portları (5436, 6380, 5673) kullanılır.

#### A2.4 Health Controller

GET /api/v1/health endpoint'i oluşturulacak. Response olarak status: "UP" ve service: "fatura-ocr-backend" döndürmeli.

**Doğrulama**: Backend'i lokal olarak IDE veya "mvn spring-boot:run" ile çalıştır (PostgreSQL, Redis, RabbitMQ henüz olmadığı için hata verebilir, bu normaldir — önemli olan projenin derlenmesi).

---

### A3. Frontend Project Skeleton (Next.js)

**Purpose**: Create the initial Next.js project with all required configurations.

**Directory**: frontend/

#### A3.1 Project Initialization
- **Framework**: Next.js 14+ (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **UI Components**: Shadcn/ui (incrementally eklenecek)

#### A3.2 Directory Structure

frontend/src/ altında:
- app/layout.tsx, app/page.tsx, app/globals.css
- components/ui/ — Shadcn bileşenleri buraya gelecek
- lib/utils.ts
- services/api.ts — API client setup
- stores/ — Zustand stores (boş)
- types/index.ts

Root'ta: tailwind.config.ts, tsconfig.json, next.config.js, package.json

#### A3.3 Key Dependencies

dependencies: next (14.x), react (^18.2.0), react-dom (^18.2.0), @tanstack/react-query (^5.x), zustand (^4.x), axios (^1.x), clsx (^2.x), tailwind-merge (^2.x)

devDependencies: typescript (^5.x), @types/node (^20.x), @types/react (^18.x), tailwindcss (^3.x), postcss (^8.x), autoprefixer (^10.x), eslint (^8.x), eslint-config-next (14.x)

#### A3.4 Initial Page Content

Ana sayfa şunları göstermeli:
- Proje adı: "Fatura OCR ve Veri Yönetim Sistemi"
- Backend API'ye bağlantı durumunu gösteren basit bir status indicator
- "Development Environment Ready" mesajı

#### A3.5 API Client Configuration

services/api.ts dosyasında API base URL olarak http://localhost:8082/api/v1 kullanılmalıdır (yeni backend host portu).

**CORS ayarı hatırlatması**: Backend'in CORS konfigürasyonunda frontend origin olarak http://localhost:3001 tanımlanmalıdır.

**Doğrulama**: "npm install && npm run dev" ile frontend'in localhost:3000'de (container iç port) sorunsuz başladığını doğrula.

---

### A4. Extraction Service Project Skeleton (Python FastAPI)

**Purpose**: Create the initial Python FastAPI project structure.

**Directory**: extraction-service/

#### A4.1 Directory Structure

extraction-service/src/ altında:
- __init__.py
- main.py — FastAPI app entry point
- api/__init__.py, api/health.py — Health check endpoint
- core/__init__.py, core/config.py — Settings/configuration
- models/__init__.py

tests/__init__.py, requirements.txt, .python-version (3.11)

#### A4.2 Requirements

fastapi==0.109.0, uvicorn[standard]==0.27.0, pydantic==2.5.3, pydantic-settings==2.1.0, python-multipart==0.0.6, pillow==10.2.0, pika==1.3.2, httpx==0.26.0, python-dotenv==1.0.0

#### A4.3 Main Application (src/main.py)

FastAPI uygulaması şu şekilde yapılandırılmalı:
- title: "Fatura OCR Extraction Service"
- description: "LLM-based invoice data extraction service"
- version: "0.1.0"
- CORS middleware: allow_origins olarak http://localhost:3001 (yeni frontend host portu) tanımlanmalı
- GET /health endpoint'i: status: "healthy", service: "extraction-service" döndürmeli

**Doğrulama**: "pip install -r requirements.txt && uvicorn src.main:app --reload --port 8000" ile servisin başladığını doğrula.

---

### A5. Environment Configuration

**Purpose**: Create environment variable templates and configuration.

#### A5.1 .env.example (Project Root)

Aşağıdaki değişkenler tanımlanmalıdır:

**Database Configuration:**
- DB_USER=fatura_user
- DB_PASSWORD=fatura_secret_2026
- DB_NAME=fatura_ocr_db
- DB_HOST_PORT=5436

**Redis Configuration:**
- REDIS_HOST=localhost
- REDIS_HOST_PORT=6380

**RabbitMQ Configuration:**
- RABBITMQ_USER=fatura_mq
- RABBITMQ_PASSWORD=mq_secret_2026
- RABBITMQ_HOST=localhost
- RABBITMQ_HOST_PORT=5673
- RABBITMQ_MGMT_HOST_PORT=15673

**Backend Configuration:**
- SPRING_PROFILES_ACTIVE=dev
- BACKEND_HOST_PORT=8082

**Frontend Configuration:**
- NEXT_PUBLIC_API_URL=http://localhost:8082/api/v1
- FRONTEND_HOST_PORT=3001

**Extraction Service Configuration:**
- EXTRACTION_HOST_PORT=8001

**LLM API Keys (DO NOT COMMIT REAL VALUES):**
- GEMINI_API_KEY=your_gemini_api_key_here
- OPENAI_API_KEY=your_openai_api_key_here
- ANTHROPIC_API_KEY=your_anthropic_api_key_here

**JWT Configuration (for later phases):**
- JWT_SECRET=your_super_secret_jwt_key_min_32_chars
- JWT_ACCESS_TOKEN_EXPIRY=900
- JWT_REFRESH_TOKEN_EXPIRY=604800

#### A5.2 .gitignore (Project Root)

Şu dosya/klasörler ignore edilmeli:

Environment: .env, .env.local, .env.*.local
IDE: .idea/, .vscode/, *.swp, *.swo
OS: .DS_Store, Thumbs.db
Backend: backend/target/, backend/*.jar, backend/.mvn/
Frontend: frontend/node_modules/, frontend/.next/, frontend/out/, frontend/.pnp.*
Extraction Service: extraction-service/__pycache__/, extraction-service/*.pyc, extraction-service/.pytest_cache/, extraction-service/venv/, extraction-service/.venv/
Docker: *.log
Coverage: coverage/, .coverage, htmlcov/
Build outputs: dist/, build/

---

### A6. Git Repository Setup

**Purpose**: Initialize Git repository with proper branch strategy.

- Git repo'yu başlat (git init)
- İlk commit: "[Phase-0] Initial project setup - project skeletons"
- Branch stratejisi: main (production-ready), develop (integration), feature/* (feature branches)
- develop branch'ini oluştur ve geç

---

### A7. README.md Documentation

**Purpose**: Create comprehensive project documentation.

README.md şunları içermeli:
1. Proje adı ve açıklaması
2. Teknoloji stack'i özeti
3. Gereksinimler (Java 17, Node.js 20, Python 3.11, Docker, Docker Compose, Git)
4. Hızlı başlangıç talimatları (hem lokal hem Docker)
5. Geliştirme ortamı kurulum adımları
6. Servis URL'leri tablosu (yeni portlarla)
7. Proje yapısı özeti
8. Ekip üyeleri

**Servis URL Tablosu (README'de kullanılacak):**

| Servis | Lokal URL | Docker URL |
|--------|-----------|------------|
| Backend | http://localhost:8082 | http://localhost:8082 |
| Frontend | http://localhost:3001 | http://localhost:3001 |
| Extraction Service | http://localhost:8001 | http://localhost:8001 |
| PostgreSQL | localhost:5436 | localhost:5436 |
| Redis | localhost:6380 | localhost:6380 |
| RabbitMQ AMQP | localhost:5673 | localhost:5673 |
| RabbitMQ Management UI | http://localhost:15673 | http://localhost:15673 |

---

## AŞAMA B: DOCKER ENTEGRASYONU (EN SON YAPILACAK)

**ÖN KOŞUL:** Aşama A tamamen tamamlanmış olmalıdır. Backend derlenmeli, frontend build olmalı, extraction service çalışmalıdır.

### B1. Backend Dockerfile (backend/Dockerfile)

İki aşamalı (multi-stage) build:
- **Build stage**: eclipse-temurin:17-jdk-alpine image'ı üzerinde Maven ile projeyi derle (testleri atla)
- **Runtime stage**: eclipse-temurin:17-jre-alpine image'ı üzerinde sadece jar dosyasını kopyala
- Container iç port: 8080
- Entrypoint: java -jar app.jar

### B2. Frontend Dockerfile (frontend/Dockerfile)

- Base image: node:20-alpine
- Working directory: /app
- package*.json kopyala, npm install çalıştır
- Tüm dosyaları kopyala
- Container iç port: 3000
- Command: npm run dev

### B3. Extraction Service Dockerfile (extraction-service/Dockerfile)

- Base image: python:3.11-slim
- Working directory: /app
- requirements.txt kopyala, pip install çalıştır
- Tüm dosyaları kopyala
- Container iç port: 8000
- Command: uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload

### B4. Docker Compose Configuration (docker-compose.yml)

**Purpose**: Define all infrastructure services and application containers with the custom port mappings.

#### B4.1 PostgreSQL Database
- Image: postgres:15-alpine
- Container Name: fatura-ocr-postgres
- **Port Mapping: 5436:5432**
- Environment: POSTGRES_DB=fatura_ocr_db, POSTGRES_USER=${DB_USER:-fatura_user}, POSTGRES_PASSWORD=${DB_PASSWORD:-fatura_secret_2026}
- Volume: postgres_data:/var/lib/postgresql/data
- Health Check: pg_isready -U ${DB_USER:-fatura_user} -d fatura_ocr_db

#### B4.2 Redis Cache
- Image: redis:7-alpine
- Container Name: fatura-ocr-redis
- **Port Mapping: 6380:6379**
- Command: redis-server --appendonly yes
- Volume: redis_data:/data
- Health Check: redis-cli ping

#### B4.3 RabbitMQ Message Queue
- Image: rabbitmq:3-management-alpine
- Container Name: fatura-ocr-rabbitmq
- **Port Mappings: 5673:5672 (AMQP), 15673:15672 (Management UI)**
- Environment: RABBITMQ_DEFAULT_USER=${RABBITMQ_USER:-fatura_mq}, RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD:-mq_secret_2026}
- Volume: rabbitmq_data:/var/lib/rabbitmq
- Health Check: rabbitmq-diagnostics -q ping

#### B4.4 Backend Service (Spring Boot)
- Build Context: ./backend
- Container Name: fatura-ocr-backend
- **Port Mapping: 8082:8080**
- Environment Variables:
  - SPRING_PROFILES_ACTIVE=dev
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fatura_ocr_db (container iç port, Docker network üzerinden)
  - SPRING_DATASOURCE_USERNAME=${DB_USER:-fatura_user}
  - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-fatura_secret_2026}
  - SPRING_REDIS_HOST=redis (container adı)
  - SPRING_REDIS_PORT=6379 (container iç port)
  - SPRING_RABBITMQ_HOST=rabbitmq (container adı)
  - SPRING_RABBITMQ_PORT=5672 (container iç port)
  - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USER:-fatura_mq}
  - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-mq_secret_2026}
- Depends On: postgres, redis, rabbitmq (with health checks)
- Volume: ./backend:/app (for hot reload in dev)

**ÖNEMLİ:** Docker environment'ta servisler arası iletişim container adları ve iç portlar üzerinden yapılır. Host portları (5436, 6380 vb.) sadece bilgisayarınızdan erişim içindir.

#### B4.5 Frontend Service (Next.js)
- Build Context: ./frontend
- Container Name: fatura-ocr-frontend
- **Port Mapping: 3001:3000**
- Environment Variables:
  - NEXT_PUBLIC_API_URL=http://localhost:8082/api/v1 (tarayıcıdan erişim için host port)
  - NODE_ENV=development
- Depends On: backend
- Volumes: ./frontend:/app (hot reload), /app/node_modules (anonymous volume)

#### B4.6 Extraction Service (Python FastAPI)
- Build Context: ./extraction-service
- Container Name: fatura-ocr-extraction
- **Port Mapping: 8001:8000**
- Environment Variables:
  - PYTHONUNBUFFERED=1
  - ENVIRONMENT=development
  - RABBITMQ_HOST=rabbitmq (container adı)
  - RABBITMQ_PORT=5672 (container iç port)
  - RABBITMQ_USER=${RABBITMQ_USER:-fatura_mq}
  - RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-mq_secret_2026}
  - GEMINI_API_KEY=${GEMINI_API_KEY}
  - OPENAI_API_KEY=${OPENAI_API_KEY}
  - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
- Depends On: rabbitmq (with health check)
- Volume: ./extraction-service:/app (hot reload)

#### B4.7 Named Volumes
- postgres_data
- redis_data
- rabbitmq_data

#### B4.8 Network
- fatura-network (bridge driver)

---

## TECHNICAL SPECIFICATIONS

### Service Ports Summary (Güncellenmiş)

| Service | Container İç Port | Host Port | Lokal Erişim URL |
|---------|-------------------|-----------|-----------------|
| Backend (Spring Boot) | 8080 | 8082 | http://localhost:8082 |
| Frontend (Next.js) | 3000 | 3001 | http://localhost:3001 |
| Extraction Service (FastAPI) | 8000 | 8001 | http://localhost:8001 |
| PostgreSQL | 5432 | 5436 | localhost:5436 |
| Redis | 6379 | 6380 | localhost:6380 |
| RabbitMQ (AMQP) | 5672 | 5673 | localhost:5673 |
| RabbitMQ (Management UI) | 15672 | 15673 | http://localhost:15673 |

### Health Check Endpoints

| Service | Endpoint | Expected Response |
|---------|----------|-------------------|
| Backend | GET http://localhost:8082/api/v1/health | {"status": "UP", "service": "fatura-ocr-backend"} |
| Frontend | GET http://localhost:3001 | 200 OK (HTML page) |
| Extraction | GET http://localhost:8001/health | {"status": "healthy", "service": "extraction-service"} |

---

## TESTING REQUIREMENTS

### Aşama A Testleri (Docker'sız)

#### Test A1: Backend Derleme
- Maven ile projeyi derle: mvn clean compile
- Beklenen: BUILD SUCCESS (runtime bağımlılıklar olmadan derleme başarılı olmalı)

#### Test A2: Frontend Build
- npm install && npm run build
- Beklenen: Build başarılı, hata yok

#### Test A3: Extraction Service
- pip install -r requirements.txt çalıştır
- uvicorn src.main:app --port 8000 ile başlat
- GET /health endpoint'ini test et
- Beklenen: {"status": "healthy", "service": "extraction-service"}

### Aşama B Testleri (Docker ile)

#### Test B1: Infrastructure Services
- docker-compose up -d postgres redis rabbitmq ile sadece altyapıyı başlat
- 30 saniye bekle
- PostgreSQL testi: docker exec -it fatura-ocr-postgres psql -U fatura_user -d fatura_ocr_db -c "SELECT 1;"
- Redis testi: docker exec -it fatura-ocr-redis redis-cli ping → Beklenen: PONG
- RabbitMQ testi: curl -u fatura_mq:mq_secret_2026 http://localhost:15673/api/overview → Beklenen: JSON response

#### Test B2: Application Services
- docker-compose up -d ile tüm servisleri başlat
- 60 saniye bekle
- Backend testi: curl http://localhost:8082/api/v1/health → Beklenen: {"status": "UP"}
- Frontend testi: curl -I http://localhost:3001 → Beklenen: HTTP/1.1 200 OK
- Extraction testi: curl http://localhost:8001/health → Beklenen: {"status": "healthy"}

#### Test B3: Service Logs
- docker-compose logs backend, frontend, extraction-service loglarında kritik hata olmamalı

#### Test B4: Hot Reload
1. backend/src/ altında bir dosya değiştir → Backend otomatik restart etmeli
2. frontend/src/ altında bir dosya değiştir → Frontend otomatik refresh etmeli
3. extraction-service/src/ altında bir dosya değiştir → Extraction service otomatik reload etmeli

---

## VERIFICATION CHECKLIST

### Aşama A Checklist
- [ ] Proje root dizin yapısı oluşturuldu
- [ ] Backend Spring Boot projesi hatasız derleniyor
- [ ] Frontend Next.js projesi hatasız build oluyor
- [ ] Extraction Service FastAPI hatasız başlıyor
- [ ] .env.example tüm gerekli değişkenleri içeriyor (yeni portlarla)
- [ ] .gitignore tüm gerekli pattern'ları kapsıyor
- [ ] README.md kapsamlı ve doğru (yeni portlarla)
- [ ] Git repository develop branch ile başlatıldı

### Aşama B Checklist
- [ ] Backend Dockerfile oluşturuldu ve build oluyor
- [ ] Frontend Dockerfile oluşturuldu ve build oluyor
- [ ] Extraction Service Dockerfile oluşturuldu ve build oluyor
- [ ] docker-compose.yml geçerli ve doğru yapılandırılmış (yeni portlarla)
- [ ] 6 servis tanımlı (postgres, redis, rabbitmq, backend, frontend, extraction)
- [ ] docker-compose up tüm servisleri başarıyla başlatıyor
- [ ] PostgreSQL erişilebilir ve veritabanı oluşturulmuş
- [ ] Redis ping'e yanıt veriyor
- [ ] RabbitMQ Management UI http://localhost:15673 adresinde erişilebilir
- [ ] Backend health endpoint 200 döndürüyor (http://localhost:8082/api/v1/health)
- [ ] Frontend ana sayfası yükleniyor (http://localhost:3001)
- [ ] Extraction service health endpoint 200 döndürüyor (http://localhost:8001/health)
- [ ] Tüm servisler Docker network içinde iletişim kurabiliyor
- [ ] Hot reload geliştirme için çalışıyor

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
docs/FURKAN/step_results/faz_0_result.md

(NOT: Normalde bu faz Ömer'e aittir ancak port konfigürasyonu nedeniyle Furkan tarafından yapıldığı için result dosyası FURKAN klasöründe oluşturulacaktır.)

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (2-3 days)

### 2. Completed Tasks
Her görev için checkbox ile durum:
- [x] Tamamlanan görev
- [ ] Tamamlanmayan görev (nedenle birlikte)

### 3. Files Created
Oluşturulan tüm dosyaların tam yol listesi

### 4. Test Results — Aşama A
Her test için gerçek çıktı

### 5. Test Results — Aşama B
Her Docker testi için gerçek çıktı

### 6. Docker Status
docker-compose ps çıktısı

### 7. Issues Encountered
Karşılaşılan sorunlar ve çözümleri

### 8. Port Konfigürasyonu Doğrulama
Tüm portların doğru çalıştığının kanıtı (curl çıktıları)

### 9. Screenshots
- Docker Desktop'ta çalışan container'lar
- Frontend ana sayfası tarayıcıda
- RabbitMQ Management UI (http://localhost:15673)

### 10. Next Steps
Phase 1 (CI/CD Pipeline) için yapılması gerekenler

### 11. Notes for Team
Ömer için önemli bilgiler (özellikle port değişiklikleri)

---

## DEPENDENCIES

### Requires (must be completed first)
- None (This is Phase 0)

### Required By (blocks these phases)
- **Phase 1**: CI/CD Pipeline Setup (ÖMER)
- **Phase 2**: Hexagonal Architecture (ÖMER)
- **Phase 3**: Database Schema (ÖMER)
- **Phase 10**: Frontend Layout (ÖMER)
- **Phase 13**: FastAPI Setup (FURKAN) - needs extraction-service skeleton

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

### Aşama A Başarı Kriterleri:
1. ✅ Backend Spring Boot projesi hatasız derleniyor
2. ✅ Frontend Next.js projesi hatasız build oluyor
3. ✅ Extraction Service FastAPI hatasız başlıyor
4. ✅ Tüm konfigürasyon dosyaları yeni portlarla yapılandırılmış
5. ✅ Git repository hazır

### Aşama B Başarı Kriterleri:
6. ✅ docker-compose up tüm 6 servisi hatasız başlatıyor
7. ✅ PostgreSQL veritabanı fatura_ocr_db erişilebilir (localhost:5436)
8. ✅ Redis ping komutuna yanıt veriyor (localhost:6380)
9. ✅ RabbitMQ Management UI erişilebilir (http://localhost:15673)
10. ✅ Backend health endpoint HTTP 200 döndürüyor (http://localhost:8082)
11. ✅ Frontend sayfası yükleniyor (http://localhost:3001)
12. ✅ Extraction service health endpoint HTTP 200 döndürüyor (http://localhost:8001)
13. ✅ Tüm servisler Docker network içinde iletişim kurabiliyor
14. ✅ Development hot reload tüm servisler için çalışıyor
15. ✅ Result dosyası oluşturulmuş

---

## IMPORTANT NOTES

1. **Do NOT commit real API keys** - Use .env.example as template, create local .env
2. **Port çakışmaları çözüldü** - Bu proje için özel port haritası kullanılıyor (5436, 6380, 5673, 15673, 8082, 3001, 8001). Standart portlar KULLANILMAMALIDIR.
3. **Docker resources** - Allocate at least 4GB RAM to Docker
4. **İki aşamalı kurulum** - Önce Aşama A (iskelet projeler), sonra Aşama B (Docker entegrasyonu)
5. **Database persistence** - Data is persisted in Docker volumes
6. **Hot reload** - Volume mounts enable development without rebuilding
7. **Network isolation** - All services communicate via fatura-network
8. **Container iç port vs Host port** - Servisler arası iletişim container iç portlarıyla (5432, 6379, 5672, 8080, 3000, 8000), bilgisayardan erişim host portlarıyla (5436, 6380, 5673, 8082, 3001, 8001)

---

## TROUBLESHOOTING

### Common Issues

**Issue**: Port already in use
- lsof -i :PORT komutuyla hangi process'in portu kullandığını bul
- Eğer başka bir Docker container ise: docker stop CONTAINER_NAME

**Issue**: Container exits immediately
- docker-compose logs SERVICE_NAME ile logları kontrol et

**Issue**: Database connection refused
- PostgreSQL container'ının healthy olduğunu doğrula: docker-compose ps
- Lokal erişimde doğru host portunu kullandığından emin ol (5436, 5432 DEĞİL)

**Issue**: Frontend cannot reach backend
- NEXT_PUBLIC_API_URL'in http://localhost:8082/api/v1 olduğunu doğrula
- Backend'in CORS'ta http://localhost:3001 origin'ine izin verdiğini doğrula

**Issue**: Permission denied on Linux
- sudo usermod -aG docker $USER ile kullanıcıyı docker grubuna ekle, logout/login yap

---

## ROLLBACK PLAN

If this phase needs to be reverted:

1. Docker container'larını durdur ve volume'ları sil: docker-compose down -v
2. Oluşturulan dizinleri sil: rm -rf backend frontend extraction-service
3. Git'i sıfırla: git reset --hard HEAD~1

---

## COMMANDS REFERENCE

### Aşama A (Lokal Geliştirme)
- Backend derle: cd backend && mvn clean compile
- Frontend başlat: cd frontend && npm run dev
- Extraction service başlat: cd extraction-service && uvicorn src.main:app --reload --port 8000

### Aşama B (Docker)
- Tüm servisleri başlat: docker-compose up -d
- Build ile başlat: docker-compose up -d --build
- Servisleri durdur: docker-compose down
- Volume'larla birlikte durdur (DİKKAT: verileri siler): docker-compose down -v
- Logları izle: docker-compose logs -f
- Belirli servis logları: docker-compose logs -f backend
- Servis rebuild: docker-compose build backend
- Container'a bağlan: docker exec -it fatura-ocr-backend sh
- Servis durumu: docker-compose ps

---

## DİĞER FAZLARA ETKİSİ

Bu fazdaki port değişiklikleri sonraki fazları da etkiler. Aşağıdaki fazlarda port referansları yeni değerlerle kullanılmalıdır:

- **Phase 1 (CI/CD)**: Test komutlarında port referansları
- **Phase 4 (Authentication)**: JWT token endpoint URL'leri
- **Phase 10 (Frontend Layout)**: API base URL konfigürasyonu
- **Phase 11 (Frontend Auth)**: Auth endpoint URL'leri
- **Phase 13 (FastAPI Setup)**: RabbitMQ bağlantı portu
- **Phase 19 (RabbitMQ)**: RabbitMQ bağlantı ayarları
- **Phase 39 (Staging/Production)**: Production docker-compose portları
- **Phase 41 (API Documentation)**: Swagger/OpenAPI URL'leri

**NOT:** Container iç portlar hiçbir fazda değişmez. Sadece host portları (lokal erişim ve docker-compose port mapping) yeni değerleri kullanır.

---

**Phase 0 Completion Target**: All infrastructure ready for Phase 1-13 development, with custom port configuration to avoid conflicts with existing Docker projects.
