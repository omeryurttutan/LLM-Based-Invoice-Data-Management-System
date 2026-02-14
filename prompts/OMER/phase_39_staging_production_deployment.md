# PHASE 39: STAGING AND PRODUCTION ENVIRONMENT PREPARATION

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM & Frontend) & Ömer Talha Yurttutan (Backend & Infrastructure)
- **Architecture**: Hybrid Microservices — Spring Boot (8080), Python FastAPI (8000), Next.js (3000)
- **Infrastructure**: PostgreSQL 15, Redis 7, RabbitMQ 3

### Current State (Phases 0-38 Completed)
All features are implemented, tested (unit + integration + E2E), and performance-optimized. The system is fully functional in the development environment (Docker Compose local setup). This phase transitions the project from a development-only setup to deployment-ready staging and production environments.

### What Has Been Built So Far
- **Backend (Spring Boot)**: Authentication (JWT), RBAC (4 roles), Invoice CRUD, Audit Log, Duplication Check, Filtering (JPA Specifications), Export (XLSX/CSV/Accounting formats), Dashboard Stats, WebSocket Notifications, Email/Push Notifications, Version History, Template & Rule Engine, KVKK Compliance (AES-256-GCM encryption), Rate Limiting, Performance Optimization (Redis caching, index tuning)
- **Frontend (Next.js)**: Layout/Routing, Auth Pages, Invoice CRUD UI, File Upload UI, Verification UI, Filtering UI, Export UI, Dashboard & Charts, Notification UI, Version History UI, Rule Engine UI, PWA Configuration, i18n (Turkish), Performance Optimization (lazy loading, code splitting)
- **Extraction Service (Python FastAPI)**: Image Preprocessing (Pillow), Gemini Flash Integration, Fallback Chain (GPT + Claude), Response Validation & Confidence Scoring, E-Invoice XML Parser, RabbitMQ Consumer, Template Learning Algorithm
- **Testing**: Backend unit tests (JUnit 5, Mockito), Python unit tests (pytest), Backend integration tests (Testcontainers), Python integration tests, Frontend E2E tests (Playwright)
- **CI/CD**: GitHub Actions pipeline (lint, test, build on push)

### Existing Docker Setup (Development)
The project already has a `docker-compose.yml` for local development created in Phase 0 with these services: postgres, redis, rabbitmq, backend, frontend, extraction-service. Each service has a basic Dockerfile created during Phase 0.

### Phase Assignment
- **Assigned To**: ÖMER (Backend & Infrastructure Developer)
- **Estimated Duration**: 2-3 days
- **Branch**: `feature/omer/faz-39-staging-production`

---

## OBJECTIVE

Prepare the application for deployment by creating production-ready Docker images, environment-specific configurations, a reverse proxy, SSL/TLS support, secret management, and a database backup strategy. The goal is to have both a staging environment (for testing before going live) and a production environment (for the actual deployment).

---

## DETAILED REQUIREMENTS

### 1. Multi-Stage Docker Builds (Production-Optimized Images)

The existing Dockerfiles from Phase 0 are development-oriented (include dev dependencies, hot reload, debug tools). Create production-optimized Dockerfiles for each service using multi-stage builds.

**1.1 Backend (Spring Boot) — `backend/Dockerfile.prod`**

Create a multi-stage Dockerfile:
- **Stage 1 (Build)**: Use a Maven/Gradle image with JDK 17. Copy source code, run the build to produce a JAR file. Skip tests during the Docker build (tests are already run in CI/CD).
- **Stage 2 (Runtime)**: Use a minimal JRE 17 image (like `eclipse-temurin:17-jre-alpine`). Copy only the JAR from Stage 1. Set appropriate JVM memory flags for production (initial heap, max heap — keep reasonable for a graduation project, e.g., 256m-512m). Expose port 8080. Run as a non-root user for security. Include a health check instruction.

**1.2 Frontend (Next.js) — `frontend/Dockerfile.prod`**

Create a multi-stage Dockerfile:
- **Stage 1 (Dependencies)**: Use Node.js 20 alpine. Install dependencies only (copy package.json and lock file first for Docker layer caching).
- **Stage 2 (Build)**: Copy source code, run `next build` to produce the production build. Set `NEXT_TELEMETRY_DISABLED=1`.
- **Stage 3 (Runtime)**: Use Node.js 20 alpine (slim). Copy only the `.next/standalone` output (if using Next.js standalone output mode) or the built files. Expose port 3000. Run as non-root user.

**1.3 Extraction Service (Python FastAPI) — `extraction-service/Dockerfile.prod`**

Create a multi-stage Dockerfile:
- **Stage 1 (Build)**: Use Python 3.11 slim. Install system dependencies needed for Pillow (image processing libraries). Install Python dependencies from requirements.txt.
- **Stage 2 (Runtime)**: Use Python 3.11 slim. Copy installed packages and source code from Stage 1. Do NOT include test files or development dependencies. Expose port 8000. Run as non-root user. Use Uvicorn with production settings (no reload, appropriate worker count).

**1.4 Image Size Targets**

Document the final image sizes. Aim for:
- Backend: under 300MB (JRE + JAR)
- Frontend: under 200MB (Node.js + built assets)
- Extraction Service: under 500MB (Python + Pillow dependencies)

If images are larger, document why (e.g., Pillow system libraries) and whether further optimization is possible.

---

### 2. Environment-Specific Configuration

**2.1 Spring Boot Profiles**

Create environment-specific configuration files:

**`application-staging.yml`:**
- Database: PostgreSQL connection pointing to staging DB host
- Redis: staging Redis host
- RabbitMQ: staging RabbitMQ host
- Logging level: INFO (not DEBUG)
- JPA: `show-sql: false`, `ddl-auto: validate` (never auto-create in staging/prod)
- Flyway: enabled (auto-run migrations)
- CORS: allowed origins set to the staging frontend URL
- Rate limiting: same as development for testing
- Swagger UI: enabled (helpful for testing in staging)

**`application-prod.yml`:**
- Database: PostgreSQL connection pointing to production DB host
- Redis: production Redis host
- RabbitMQ: production RabbitMQ host
- Logging level: WARN (only warnings and errors)
- JPA: `show-sql: false`, `ddl-auto: validate`
- Flyway: enabled (auto-run migrations on startup)
- CORS: allowed origins set to the production frontend URL only
- Rate limiting: production values (100 req/min authenticated, 20 req/min public)
- Swagger UI: disabled in production (security measure)
- Actuator: expose only `/health` and `/info` publicly, restrict `/metrics` and others to admin

**2.2 Next.js Environment Files**

Create environment configuration:

**`.env.staging`:**
- `NEXT_PUBLIC_API_URL`: Backend API URL for staging
- `NEXT_PUBLIC_WS_URL`: WebSocket URL for staging
- `NEXT_PUBLIC_VAPID_PUBLIC_KEY`: VAPID key for push notifications
- `NEXT_PUBLIC_APP_ENV`: staging

**`.env.production`:**
- `NEXT_PUBLIC_API_URL`: Backend API URL for production
- `NEXT_PUBLIC_WS_URL`: WebSocket URL for production
- `NEXT_PUBLIC_VAPID_PUBLIC_KEY`: VAPID key for push notifications
- `NEXT_PUBLIC_APP_ENV`: production

**2.3 Python FastAPI Configuration**

Update the extraction service settings to support environments:

- Use environment variables for all configuration (already partially done in Phase 13)
- Ensure LLM API keys (Gemini, OpenAI, Anthropic) are read from environment variables, never hardcoded
- RabbitMQ connection URL from environment variable
- Log level configurable via environment variable
- Uvicorn worker count configurable via environment variable

---

### 3. Docker Compose for Staging and Production

**3.1 `docker-compose.staging.yml`**

Create a staging Docker Compose file that:
- Uses the production Dockerfiles (Dockerfile.prod) for all services
- Mounts an `.env.staging` file for environment variables
- Includes PostgreSQL, Redis, and RabbitMQ with staging-appropriate settings
- PostgreSQL: persistence via a named Docker volume
- Redis: persistence via appendonly mode, limited memory (e.g., 256MB max)
- RabbitMQ: persistence via a named Docker volume
- All services on a private Docker network
- Restart policy: `unless-stopped` for all services
- Resource limits (optional but recommended): memory limits for each container

**3.2 `docker-compose.prod.yml`**

Create a production Docker Compose file that:
- Uses the production Dockerfiles (Dockerfile.prod) for all services
- Mounts an `.env.prod` file for environment variables
- Includes Nginx as a reverse proxy (see section 5)
- PostgreSQL: persistence via named volume, production-tuned settings (shared_buffers, effective_cache_size, etc.)
- Redis: persistence, password-protected, limited memory
- RabbitMQ: persistence, management plugin with authentication
- All services on a private Docker network (not exposed to host except Nginx on 80/443)
- Restart policy: `always`
- Health checks defined for all services
- Only Nginx ports (80, 443) exposed to the host — backend, frontend, extraction-service are internal only

---

### 4. Secret Management

**4.1 `.env.example` Update**

Update the existing `.env.example` file to include ALL environment variables needed for staging/production. Group them by service:

- **Database**: POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD
- **Redis**: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
- **RabbitMQ**: RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASSWORD
- **Backend (Spring Boot)**: SPRING_PROFILES_ACTIVE, JWT_SECRET, JWT_ACCESS_EXPIRATION, JWT_REFRESH_EXPIRATION, ENCRYPTION_KEY (AES-256 for KVKK)
- **LLM API Keys**: GEMINI_API_KEY, OPENAI_API_KEY, ANTHROPIC_API_KEY
- **Email/SMTP**: SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM_EMAIL
- **Push Notifications**: VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY
- **Frontend**: NEXT_PUBLIC_API_URL, NEXT_PUBLIC_WS_URL, NEXT_PUBLIC_APP_ENV
- **Extraction Service**: EXTRACTION_SERVICE_HOST, EXTRACTION_SERVICE_PORT, LOG_LEVEL, UVICORN_WORKERS

**4.2 Security Rules**

- NEVER commit `.env.staging` or `.env.prod` files to Git
- Add `.env.staging`, `.env.prod`, `.env.local` to `.gitignore` (verify they are already there; if not, add them)
- Document in README that secrets must be provided via environment variables or a `.env` file at deployment time
- For the graduation project, it is acceptable to use `.env` files on the server. In a real production environment, a secrets manager (like HashiCorp Vault, AWS Secrets Manager, or Docker Secrets) would be used. Document this as a note.

---

### 5. Nginx Reverse Proxy

**5.1 Create Nginx Configuration — `nginx/nginx.conf`**

Create an Nginx configuration that:
- Listens on port 80 (HTTP) and redirects all traffic to port 443 (HTTPS)
- Listens on port 443 (HTTPS) with SSL/TLS
- Routes requests based on path:
  - `/api/*` → proxy to backend service (port 8080)
  - `/ws/*` → proxy to backend WebSocket (port 8080) with WebSocket upgrade headers
  - `/extraction/*` → proxy to extraction service (port 8000) — only if direct access is needed; otherwise, extraction is internal only via RabbitMQ
  - `/` and all other paths → proxy to frontend service (port 3000)
- Sets proper proxy headers: `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`, `Host`
- WebSocket support: `Upgrade`, `Connection` headers for the `/ws` path
- Client max body size: at least 20MB (for invoice file uploads)
- Gzip compression enabled for text, CSS, JS, JSON responses
- Security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`
- Static file caching headers for frontend assets
- Rate limiting at Nginx level is optional (already handled by Spring Boot in Phase 32), but adding a basic connection limit is a good defense-in-depth measure

**5.2 Nginx Dockerfile — `nginx/Dockerfile`**

Create a simple Dockerfile:
- Use `nginx:alpine` base image
- Copy the custom `nginx.conf`
- Copy SSL certificate files (or mount them as volumes)
- Expose ports 80 and 443

**5.3 SSL/TLS Configuration**

For the graduation project, use self-signed certificates for staging and testing:
- Create a script (`scripts/generate-ssl-cert.sh`) that generates a self-signed SSL certificate and key using OpenSSL
- The script should output `nginx/ssl/cert.pem` and `nginx/ssl/key.pem`
- Document that for real production, Let's Encrypt (Certbot) should be used for free, trusted certificates
- Add a commented-out Certbot renewal configuration in the Nginx config as a reference

For production readiness documentation:
- Document the steps to obtain a Let's Encrypt certificate using Certbot
- Document the Certbot auto-renewal cron job or Docker container approach
- This documentation goes in `docs/deployment/ssl-setup.md`

---

### 6. Database Backup Strategy

**6.1 Backup Script — `scripts/backup-db.sh`**

Create a shell script that:
- Uses `pg_dump` to create a full database backup
- Saves the backup with a timestamp in the filename (e.g., `fatura_ocr_backup_20260212_153000.sql.gz`)
- Compresses the backup using gzip
- Stores backups in a `backups/` directory (configurable via environment variable)
- Keeps the last N backups (default: 7) and deletes older ones (rotation)
- Logs the backup result (success/failure, file size, duration) to a log file
- Returns a non-zero exit code on failure (for monitoring)

**6.2 Restore Script — `scripts/restore-db.sh`**

Create a shell script that:
- Takes a backup file path as an argument
- Warns the user that this will overwrite the current database (require confirmation)
- Drops the existing database and recreates it (or use `pg_restore` with `--clean`)
- Restores from the provided backup file
- Runs Flyway validation after restore to ensure schema consistency
- Logs the restore result

**6.3 Automated Backup Schedule**

Configure automated backups:
- Option A: Add a `backup` service to docker-compose.prod.yml that runs on a schedule (using a cron-like container, e.g., `ofelia` scheduler or a simple Alpine container with cron)
- Option B: Document a crontab entry for the host machine that runs the backup script daily

Recommended schedule:
- Daily full backup at 03:00 AM
- Keep 7 daily backups (1 week retention)

---

### 7. Health Checks and Service Dependencies

**7.1 Docker Compose Health Checks**

Add health check configurations to docker-compose.prod.yml for each service:

- **PostgreSQL**: `pg_isready` command
- **Redis**: `redis-cli ping`
- **RabbitMQ**: `rabbitmq-diagnostics -q check_running`
- **Backend**: HTTP request to `/api/v1/health` (or Spring Boot Actuator `/actuator/health`)
- **Frontend**: HTTP request to the home page or a health endpoint
- **Extraction Service**: HTTP request to `/health`

**7.2 Service Startup Order**

Configure `depends_on` with health check conditions:
- Backend depends on: PostgreSQL (healthy), Redis (healthy), RabbitMQ (healthy)
- Frontend depends on: Backend (started — not necessarily healthy, as it can retry API calls)
- Extraction Service depends on: RabbitMQ (healthy)
- Nginx depends on: Backend (healthy), Frontend (healthy)

---

### 8. Deployment Documentation

**8.1 Create `docs/deployment/deployment-guide.md`**

Write a step-by-step deployment guide that covers:

1. **Prerequisites**: What needs to be installed on the server (Docker, Docker Compose, Git)
2. **Server Preparation**: Minimum server requirements (CPU, RAM, disk — estimate for the graduation demo: 2 vCPU, 4GB RAM, 20GB disk)
3. **Clone and Configure**: How to clone the repo, create `.env.prod` from `.env.example`, fill in all secrets
4. **SSL Certificate Setup**: How to generate self-signed certs (for testing) or obtain Let's Encrypt certs (for production)
5. **Build and Deploy**: The exact commands to build images and start the stack (`docker compose -f docker-compose.prod.yml up -d --build`)
6. **Verify Deployment**: How to check that all services are running, test the health endpoints, access the application
7. **Database Migration**: How Flyway runs automatically on backend startup (just mention it; no manual steps needed)
8. **Updating the Application**: How to pull new code and redeploy (build new images, restart containers with zero-downtime strategy if possible, or accept brief downtime for a graduation project)
9. **Rollback**: How to revert to a previous version (keep the previous Docker images, revert code, restore DB backup if needed)
10. **Backup Verification**: How to manually trigger a backup and verify it can be restored

**8.2 Create `docs/deployment/environment-variables.md`**

Document every environment variable:
- Variable name
- Description
- Required or optional
- Default value (if any)
- Example value
- Which service uses it

---

### 9. CI/CD Updates for Deployment

**9.1 Update GitHub Actions (`.github/workflows/ci.yml`)**

Extend the existing CI/CD pipeline:
- Keep the existing steps: lint, test, build
- Add a new job (or step) that builds the production Docker images when merging to `main` branch
- The build step should use the `Dockerfile.prod` files
- Tag the images with the commit SHA and `latest`
- For a graduation project, pushing images to a container registry (Docker Hub, GHCR) is optional. Document it as a future improvement if not implemented.
- If implemented: push images to GitHub Container Registry (GHCR) — it is free for public repos and has a generous free tier for private repos

**9.2 Deploy Script — `scripts/deploy.sh`**

Create a deployment helper script that:
- Pulls the latest code from Git
- Builds production images
- Stops the current stack gracefully
- Starts the new stack
- Runs a basic health check to verify the deployment was successful
- If health check fails, prints an error and suggests rollback steps

---

### 10. Testing the Deployment Setup

After creating all the files, verify the setup works:

**10.1 Build Test**
- Build all production Docker images locally and verify they build successfully
- Check the image sizes and document them

**10.2 Staging Compose Test**
- Start the staging stack with `docker compose -f docker-compose.staging.yml up -d`
- Verify all services start and become healthy
- Access the application through Nginx (if included in staging) or directly
- Test the health endpoints of all services

**10.3 SSL Test**
- Generate self-signed certificates using the script
- Verify Nginx starts with SSL enabled
- Access the application via HTTPS (accept the self-signed cert warning in the browser)

**10.4 Backup/Restore Test**
- Run the backup script manually
- Verify a backup file is created and is not empty
- Test the restore process (optional in staging — at minimum verify the script runs without errors)

---

## FILE STRUCTURE

After completing this phase, the following files should be created or modified:

```
fatura-ocr-system/
├── backend/
│   ├── Dockerfile.prod                              # NEW
│   └── src/main/resources/
│       ├── application-staging.yml                  # NEW
│       └── application-prod.yml                     # NEW
├── frontend/
│   ├── Dockerfile.prod                              # NEW
│   ├── .env.staging                                 # NEW (not committed to Git)
│   └── .env.production                              # NEW (not committed to Git)
├── extraction-service/
│   └── Dockerfile.prod                              # NEW
├── nginx/
│   ├── Dockerfile                                   # NEW
│   ├── nginx.conf                                   # NEW
│   └── ssl/
│       ├── cert.pem                                 # NEW (generated, not committed)
│       └── key.pem                                  # NEW (generated, not committed)
├── scripts/
│   ├── generate-ssl-cert.sh                         # NEW
│   ├── backup-db.sh                                 # NEW
│   ├── restore-db.sh                                # NEW
│   └── deploy.sh                                    # NEW
├── docs/
│   └── deployment/
│       ├── deployment-guide.md                      # NEW
│       ├── environment-variables.md                 # NEW
│       └── ssl-setup.md                             # NEW
├── docker-compose.staging.yml                       # NEW
├── docker-compose.prod.yml                          # NEW
├── .env.example                                     # MODIFIED (updated with all variables)
├── .gitignore                                       # MODIFIED (add new ignore patterns)
└── .github/
    └── workflows/
        └── ci.yml                                   # MODIFIED (add production build step)
```

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_39_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Files created or modified (full list with paths)
3. Docker image sizes (table: service, dev image size, prod image size, reduction percentage)
4. Environment-specific configuration summary (what differs between dev/staging/prod)
5. Nginx routing rules summary
6. SSL/TLS setup details (self-signed for now, Let's Encrypt documentation)
7. Secret management approach documented
8. Backup strategy details (schedule, retention, tested or not)
9. Health check configuration summary
10. CI/CD changes (what was added to the pipeline)
11. Deployment guide summary (link to the full guide)
12. Testing results: build test, staging compose test, SSL test, backup test
13. Issues encountered and solutions
14. Next steps (Phase 40 Monitoring & Logging)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 0**: Development Docker Compose and Dockerfiles (the base to optimize)
- **Phase 1**: CI/CD Pipeline (to extend with production build)
- **Phase 4**: Authentication — JWT_SECRET must be configurable via environment variable
- **Phase 28**: Push Notifications — VAPID keys must be configurable via environment variable
- **Phase 31**: KVKK — ENCRYPTION_KEY must be configurable via environment variable
- **Phase 32**: Rate Limiting — production rate limit values
- **Phase 33**: PWA — requires HTTPS in production (this phase provides it)
- **Phase 38**: Performance — optimized application ready for deployment

### Required By
- **Phase 40**: Monitoring & Logging (needs the deployed environment to monitor)
- **Phase 42**: User Guide (references the deployed application URLs)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Backend Dockerfile.prod: multi-stage build with JRE-only final image
- [ ] Frontend Dockerfile.prod: multi-stage build with standalone Next.js output
- [ ] Extraction service Dockerfile.prod: multi-stage build with production Uvicorn
- [ ] Production image sizes documented (compare to dev images)
- [ ] application-staging.yml created with correct database, Redis, RabbitMQ settings
- [ ] application-prod.yml created with correct settings
- [ ] Frontend .env.staging and .env.production documented
- [ ] Extraction service fully driven by environment variables
- [ ] docker-compose.staging.yml: all services start with `docker compose up`
- [ ] docker-compose.prod.yml: all services start with `docker compose up`
- [ ] Nginx routes /api/* to backend:8080
- [ ] Nginx routes /ws/* to backend:8080 with WebSocket upgrade headers
- [ ] Nginx routes /* to frontend:3000
- [ ] File upload through Nginx works (client_max_body_size configured)
- [ ] SSL/TLS works with self-signed certificate
- [ ] HTTPS redirect works (HTTP → HTTPS)
- [ ] Let's Encrypt setup documented for production
- [ ] .env.example contains ALL required environment variables with descriptions
- [ ] .env.staging, .env.prod, SSL certificates in .gitignore
- [ ] LLM API keys (GEMINI, OPENAI, ANTHROPIC) configured as environment variables
- [ ] Database backup script: creates compressed .sql.gz backup
- [ ] Database restore script: restores from backup file
- [ ] Backup rotation: keeps last 7, deletes older
- [ ] Cron job configuration documented for automated backups
- [ ] Health checks configured for all services in docker-compose.prod.yml
- [ ] Service dependency order: DB → Redis → RabbitMQ → backend → extraction → frontend
- [ ] depends_on with condition: service_healthy configured
- [ ] All services run as non-root users in production images
- [ ] Deployment guide created with step-by-step instructions
- [ ] CI/CD updated with production image build step
- [ ] Deploy script (deploy.sh) works for basic deployment
- [ ] All existing tests still pass
- [ ] Result file created at docs/OMER/step_results/faz_39_result.md

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Multi-stage Dockerfile.prod created for backend with minimal JRE image
2. ✅ Multi-stage Dockerfile.prod created for frontend with standalone Next.js output
3. ✅ Multi-stage Dockerfile.prod created for extraction-service with production Uvicorn settings
4. ✅ Production images are significantly smaller than development images (document sizes)
5. ✅ application-staging.yml and application-prod.yml created with correct settings
6. ✅ Frontend environment files documented (staging/production API URLs)
7. ✅ Extraction service configuration fully driven by environment variables
8. ✅ docker-compose.staging.yml works and all services start successfully
9. ✅ docker-compose.prod.yml works with Nginx, SSL, and all services
10. ✅ Nginx correctly routes /api/* to backend, /ws/* to WebSocket, /* to frontend
11. ✅ WebSocket proxying works through Nginx (Upgrade headers configured)
12. ✅ File upload through Nginx works (client_max_body_size set)
13. ✅ SSL/TLS works with self-signed certificate
14. ✅ SSL setup documentation includes Let's Encrypt instructions
15. ✅ .env.example updated with ALL required environment variables, grouped and documented
16. ✅ Secret files (.env.staging, .env.prod, SSL certs) are in .gitignore
17. ✅ Database backup script works and creates compressed backups
18. ✅ Database restore script works
19. ✅ Backup rotation (keep last 7) works
20. ✅ Health checks configured for all services in docker-compose.prod.yml
21. ✅ Service dependency order configured correctly (depends_on with condition: service_healthy)
22. ✅ Deployment guide is comprehensive and follows a step-by-step format
23. ✅ Environment variables documentation is complete
24. ✅ CI/CD updated with production image build step
25. ✅ Deploy script works for basic deployment flow
26. ✅ All services run as non-root users in production images
27. ✅ All existing tests still pass (no regressions)
28. ✅ Result file created at docs/OMER/step_results/faz_39_result.md

---

## IMPORTANT NOTES

1. **Do NOT Break Development Setup**: The existing `docker-compose.yml` and development Dockerfiles must continue to work exactly as before. This phase creates ADDITIONAL files (Dockerfile.prod, docker-compose.staging.yml, docker-compose.prod.yml) alongside the existing development setup. Do not modify the original development Dockerfiles.

2. **This is a Graduation Project**: The deployment setup should be practical and demonstrable, not enterprise-grade. Self-signed SSL certificates are fine for the demo. A single-server Docker Compose setup is appropriate — no need for Kubernetes, Docker Swarm, or cloud-native orchestration. Document what would be different in a real production environment.

3. **Secrets in .env Files**: For the graduation project, using `.env` files for secrets is acceptable. In the deployment documentation, mention that a real production environment should use a secrets manager. Do not overcomplicate the setup.

4. **Nginx is the Single Entry Point**: In production, only Nginx's ports (80, 443) should be exposed to the outside. All other services (backend, frontend, extraction-service, PostgreSQL, Redis, RabbitMQ) should be on an internal Docker network only. This is a critical security measure.

5. **Database Migrations Run Automatically**: Flyway is configured to run migrations on Spring Boot startup. There is no need for a manual migration step in the deployment process. Just mention in the deployment guide that migrations run automatically when the backend container starts.

6. **Image Tagging Strategy**: Tag production images with both `latest` and the Git commit SHA. This allows easy rollback by running a specific version. For the graduation project, `latest` is sufficient, but documenting the SHA-based tagging is good practice.

7. **Coordinate with FURKAN**: Let Furkan know that the frontend `.env.staging` and `.env.production` need to contain the correct API URLs. If the staging/production domain names are not yet decided, use placeholder values and document what needs to be changed.

8. **PostgreSQL Production Tuning**: For the graduation demo, default PostgreSQL settings are fine. Optionally add basic tuning (shared_buffers = 256MB, effective_cache_size = 512MB) in the production docker-compose file via command arguments or a custom postgresql.conf. Do not over-tune.

9. **No Database Schema Changes**: This phase does NOT introduce any new database tables or columns. There should be no Flyway migration files created in this phase. If you find that you need a migration (unlikely), document why.

10. **Test Everything Locally**: Before claiming the phase is complete, build the production images, start the staging stack, and verify it works. The result file must include evidence that the setup was tested (command outputs, screenshots, or logs).
