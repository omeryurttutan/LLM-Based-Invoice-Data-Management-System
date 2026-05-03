# Fatura OCR System - Deployment Runbook

This document provides step-by-step instructions for deploying, updating, and maintaining the Fatura OCR system in production.

## 1. Prerequisites

### Hardware Requirements

- **CPU:** Minimum 2 vCPUs (4 recommended for production)
- **RAM:** Minimum 4GB (8GB recommended)
- **Disk:** 40GB SSD
- **OS:** Linux (Ubuntu 22.04 LTS recommended)

### Software Requirements

- **Docker:** Version 24+
- **Docker Compose:** Version v2+
- **Git:** Latest version

### External Services

- **LLM API Keys:**
  - Google Gemini API Key
  - OpenAI API Key (Optional fallback)
  - Anthropic API Key (Optional fallback)
- **SMTP Server:** For sending email notifications
- **Domain Name:** Pointed to the server IP (A Record)

---

## 2. Initial Deployment Procedure

Follow these steps to deploy the application from scratch.

### 2.1 Clone the Repository

```bash
git clone https://github.com/your-org/fatura-ocr-system.git
cd fatura-ocr-system
```

### 2.2 Configure Environment Variables

Copy the example environment file and fill in the required values.

```bash
cp .env.example .env.prod
nano .env.prod
```

**Critical Variables:**

- `POSTGRES_PASSWORD`: Set a strong password.
- `JWT_SECRET`: Generate a secure random string.
- `GEMINI_API_KEY`: Your Google AI API key.
- `VAPID_PRIVATE_KEY`: For push notifications.

### 2.3 Generate SSL Certificates

For the first run, generate self-signed certificates or use Let's Encrypt script (if available).

```bash
./scripts/generate-ssl.sh
```

### 2.4 Build and Start

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

### 2.5 Verify Deployment

Check if all containers are running and healthy.

```bash
docker compose -f docker-compose.prod.yml ps
```

Visit `https://your-domain.com` and login.

---

## 3. Updating the Application

To deploy a new version of the application without downtime (or minimal downtime).

1. **Pull Latest Code:**

   ```bash
   git pull origin main
   ```

2. **Build New Images:**

   ```bash
   docker compose -f docker-compose.prod.yml build
   ```

3. **Restart Services:**

   ```bash
   docker compose -f docker-compose.prod.yml up -d
   ```

   _Docker Compose will recreate only the containers that have changed._

4. **Verify:**
   Check logs for any startup errors.
   ```bash
   docker compose -f docker-compose.prod.yml logs -f --tail=100
   ```

---

## 4. Rollback Procedure

If a new deployment fails, follow these steps to revert to the previous working version.

1. **Identify Previous Version:**
   Find the commit hash of the last stable version.

   ```bash
   git log --oneline
   ```

2. **Checkout Previous Version:**

   ```bash
   git checkout <COMMIT_HASH>
   ```

3. **Restore Database (If Needed):**
   If the failed update included destructive database migrations, restore from the nightly backup.
   _(See Database Operations section)_

4. **Redeploy Previous Version:**
   ```bash
   docker compose -f docker-compose.prod.yml up --build -d --force-recreate
   ```

---

## 5. Database Operations

### 5.1 Manual Backup

To create an immediate backup of the PostgreSQL database:

```bash
docker exec -t fatura-db pg_dumpall -c -U postgres > dump_$(date +%Y-%m-%d).sql
```

### 5.2 Restore from Backup

**WARNING:** This will overwrite the current database.

```bash
cat dump_2023-XX-XX.sql | docker exec -i fatura-db psql -U postgres
```

### 5.3 Database Shell

To access the database directly:

```bash
docker exec -it fatura-db psql -U postgres -d fatura_db
```

---

## 6. Log Management

Logs are essential for troubleshooting.

- **View Real-time Logs:**

  ```bash
  docker compose -f docker-compose.prod.yml logs -f
  ```

- **View Specific Service Logs:**

  ```bash
  docker compose -f docker-compose.prod.yml logs -f backend
  docker compose -f docker-compose.prod.yml logs -f extraction-service
  ```

- **Log Rotation:**
  Docker logging driver is configured to rotate logs automatically to prevent disk fill-up (max-size: 10m, max-file: 3).

---

## 7. Common Maintenance Tasks

### 7.1 Clear Redis Cache

If you need to invalidate all active sessions and cached data:

```bash
docker exec -it fatura-redis redis-cli FLUSHALL
```

### 7.2 Purge RabbitMQ Queues

If queues are stuck with bad messages:

1. Access RabbitMQ Management UI at `http://your-server-ip:15672`
2. Login (default: guest/guest or from .env)
3. Go to **Queues** tab -> Select queue -> **Purge Messages**

### 7.3 Prune Docker System

To free up disk space by removing unused images and volumes:

```bash
docker system prune -a --volumes
```

_Run this only when the system is stopped or you are sure about unused resources._
