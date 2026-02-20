# Fatura OCR Extraction Service

LLM tabanlı fatura görüntülerinden ve e-Fatura XML dosyalarından veri çıkarım servisi.

## Teknoloji Stack

- **Framework**: FastAPI (Python 3.11)
- **LLM Providers**: Google Gemini, OpenAI GPT, Anthropic Claude
- **Image Processing**: Pillow, PyMuPDF
- **XML Parsing**: lxml (UBL-TR e-Fatura)
- **Message Queue**: RabbitMQ (pika)
- **Logging**: structlog (JSON/Console)

## Proje Yapısı

```
extraction-service/
├── app/
│   ├── main.py                  # FastAPI uygulama giriş noktası
│   ├── config/settings.py       # Pydantic Settings (env vars)
│   ├── api/routes/
│   │   ├── health.py            # Sağlık kontrol endpointleri
│   │   ├── extraction.py        # Veri çıkarım endpointleri
│   │   └── preprocessing.py     # Ön işleme endpointleri
│   ├── core/
│   │   ├── logging.py           # Yapılandırılmış loglama
│   │   ├── exceptions.py        # Özel exception sınıfları
│   │   └── middleware.py        # CORS, request logging, request ID
│   ├── models/                  # Request/Response Pydantic modelleri
│   ├── services/
│   │   ├── extraction/          # Ana çıkarım servisi
│   │   ├── llm/                 # LLM provider entegrasyonları
│   │   ├── parsers/             # XML parser (e-Fatura)
│   │   ├── preprocessing/       # Görüntü ön işleme pipeline
│   │   └── validation/          # Doğrulama katmanı
│   └── messaging/               # RabbitMQ consumer/publisher
├── tests/                       # Unit & integration testler
├── Dockerfile
├── requirements.txt
├── pyproject.toml
└── .env.example
```

## Kurulum ve Çalıştırma

### Docker ile (Önerilen)

```bash
# Proje kök dizininden
docker-compose up extraction-service
```

Servis `http://localhost:8001` adresinde çalışacaktır (host port).

### Lokal Geliştirme

```bash
cd extraction-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app/main.py # Port 8001 üzerinden çalışacaktır
```

## API Endpointleri

| Endpoint | Method | Açıklama |
|----------|--------|----------|
| `/health` | GET | Temel sağlık kontrolü |
| `/health/live` | GET | Liveness kontrolü |
| `/health/ready` | GET | Readiness kontrolü (RabbitMQ dahil) |
| `/health/dependencies` | GET | Bağımlılık durumu (Spring Boot, RabbitMQ, LLM) |
| `/health/integration` | GET | Spring Boot bağlantı testi + latency |
| `/api/v1/extraction/extract` | POST | Fatura veri çıkarımı (PDF/Image) |
| `/api/v1/extraction/extract/base64` | POST | Base64 image'dan çıkarım |
| `/api/v1/extraction/parse/xml` | POST | e-Fatura XML ayrıştırma |
| `/api/v1/extraction/validate` | POST | Fatura verisi doğrulama |
| `/api/v1/extraction/providers` | GET | LLM provider listesi |
| `/api/v1/extraction/providers/health` | GET | Provider sağlık durumu |

**Swagger UI**: `http://localhost:8000/docs`

## Ortam Değişkenleri

| Değişken | Varsayılan | Açıklama |
|----------|-----------|----------|
| `APP_NAME` | Fatura OCR Extraction Service | Uygulama adı |
| `APP_VERSION` | 1.0.0 | Versiyon |
| `DEBUG` | true | Debug modu |
| `LOG_LEVEL` | INFO | Log seviyesi |
| `ALLOWED_ORIGINS` | http://localhost:3001,http://localhost:8082 | CORS izin verilen originler |
| `SPRING_BOOT_URL` | http://localhost:8082 | Backend URL |
| `GEMINI_API_KEY` | - | Google Gemini API anahtarı |
| `OPENAI_API_KEY` | - | OpenAI API anahtarı |
| `ANTHROPIC_API_KEY` | - | Anthropic API anahtarı |
| `MAX_FILE_SIZE_MB` | 10 | Maksimum dosya boyutu (MB) |
| `SUPPORTED_FORMATS` | pdf,jpg,jpeg,png | Desteklenen formatlar |
| `REQUEST_TIMEOUT` | 60 | HTTP timeout (saniye) |
| `RABBITMQ_HOST` | localhost | RabbitMQ host |
| `RABBITMQ_PORT` | 5673 | RabbitMQ port |

## Testler

```bash
# Tüm testleri çalıştır
pytest tests/ -v --asyncio-mode=auto

# Coverage ile
pytest tests/ --cov=app --cov-report=html
```

## Mimari

```
Spring Boot Backend ──HTTP──> Python FastAPI
                    <──HTTP──

Spring Boot ──RabbitMQ──> Python Consumer
            <──RabbitMQ──  (Result Publisher)
```

Servis, Spring Boot backend ile hem HTTP hem de RabbitMQ üzerinden iletişim kurar. Fatura görüntüleri/PDF'leri LLM API'leri (Gemini → GPT → Claude fallback chain) ile analiz edilir ve yapılandırılmış JSON verisi döndürülür.
