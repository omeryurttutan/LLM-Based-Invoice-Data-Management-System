# Environment Variables Reference

This document lists all environment variables required for configuring the Fatura OCR System in staging and production environments.

## Database Configuration
| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `POSTGRES_USER` | Database username | Yes | `fatura_user` |
| `POSTGRES_PASSWORD` | Database password | Yes | `secret123` |
| `POSTGRES_DB` | Database name | Yes | `fatura_ocr_db` |
| `POSTGRES_HOST` | Database host (docker service name) | Yes | `postgres` |

## Redis Configuration
| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `REDIS_HOST` | Redis host | Yes | `redis` |
| `REDIS_PASSWORD` | Redis password | Yes | `redis_secret` |

## RabbitMQ Configuration
| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `RABBITMQ_USER` | RabbitMQ username | Yes | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | Yes | `guest` |
| `RABBITMQ_HOST` | RabbitMQ host | Yes | `rabbitmq` |

## Backend (Spring Boot)
| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `SPRING_PROFILES_ACTIVE`| Active profile (`dev`, `staging`, `prod`) | Yes | `prod` |
| `JWT_SECRET` | Secret key for JWT signing (min 32 chars) | Yes | `very_long_secret_key...` |
| `ENCRYPTION_KEY` | AES-256 Key for KVKK encryption (Base64) | Yes | `...=` |
| `SMTP_HOST` | SMTP server for emails | Yes | `smtp.gmail.com` |

## Frontend (Next.js)
| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `NEXT_PUBLIC_API_URL` | Public URL of the Backend API | Yes | `https://fatura-ocr.com/api/v1` |
| `NEXT_PUBLIC_WS_URL` | Public URL of the WebSocket | Yes | `wss://fatura-ocr.com/ws` |
| `NEXT_PUBLIC_VAPID_PUBLIC_KEY`| VAPID Public Key for Push Notifications | Yes | `...` |

## Extraction Service
| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `GEMINI_API_KEY` | Google Gemini API Key | Yes | `AIza...` |
| `OPENAI_API_KEY` | OpenAI API Key | Yes | `sk-...` |
| `ANTHROPIC_API_KEY` | Anthropic API Key | Yes | `sk-ant...` |
| `UVICORN_WORKERS` | Number of Uvicorn workers | No | `4` |
