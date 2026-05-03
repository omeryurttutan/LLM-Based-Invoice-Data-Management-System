# Deployment Guide for Fatura OCR System

This guide details the steps to deploy the "Fatura OCR ve Veri Yönetim Sistemi" to a production environment.

## 1. Prerequisites

Ensure the following are installed on your production server:
- **Docker**: v24.0+
- **Docker Compose**: v2.20+
- **Git**

### Minimum Server Requirements
- **CPU**: 2 vCPU
- **RAM**: 4GB
- **Disk**: 20GB space

## 2. Initial Setup

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/MFurkanAkdag/Fatura-OCR.git
    cd Fatura-OCR
    ```

2.  **Configure Environment Variables**
    Copy the example environment file and fill in your secrets.
    ```bash
    cp .env.example .env.prod
    nano .env.prod
    ```
    > **Note**: Refer to `environment-variables.md` for details on each variable.

3.  **Generate SSL Certificates (For Testing)**
    If you don't have a real domain/certificate yet, generate a self-signed one:
    ```bash
    cd scripts
    chmod +x generate-ssl-cert.sh
    ./generate-ssl-cert.sh
    cd ..
    ```
    > **Production**: For real production usage, see `ssl-setup.md` to use Let's Encrypt.

## 3. Build and Deployment

Use the provided deployment script for a streamlined process.

1.  **Run Deployment Script**
    ```bash
    chmod +x scripts/deploy.sh
    ./scripts/deploy.sh
    ```

    Alternatively, run manually:
    ```bash
    docker compose -f docker-compose.prod.yml build
    docker compose -f docker-compose.prod.yml up -d
    ```

2.  **Verify Deployment**
    Check the status of all containers:
    ```bash
    docker compose -f docker-compose.prod.yml ps
    ```
    All services should report `healthy`.

3.  **Access the Application**
    - **Frontend**: https://your-server-ip (or domain)
    - **Backend API**: https://your-server-ip/api/
    - **Swagger UI**: Disabled in production by default.

## 4. Maintenance

### Database Backups
Backups are configured to run via the helper script.
- **Manual Backup**:
    ```bash
    ./scripts/backup-db.sh
    ```
- **Automated Backup**:
    Add to crontab (`crontab -e`):
    ```bash
    0 3 * * * /path/to/Fatura-OCR/scripts/backup-db.sh >> /path/to/backup.log 2>&1
    ```

### Restoration
To restore from a backup file:
```bash
./scripts/restore-db.sh /path/to/backups/fatura_ocr_backup_YYYYMMDD_HHMMSS.sql.gz
```

### Updates / Rollback
- **Update**: Run `./scripts/deploy.sh` to pull latest changes and redeploy.
- **Rollback**: To roll back, checkout a previous commit and run deployment again.
    ```bash
    git checkout <previous-commit-hash>
    ./scripts/deploy.sh
    ```
