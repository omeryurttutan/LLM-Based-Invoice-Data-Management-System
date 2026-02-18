#!/bin/bash
set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-../backups}"
CONTAINER_NAME="postgres_prod"
DB_USER="fatura_user"
DB_NAME="fatura_ocr_db"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="$BACKUP_DIR/fatura_ocr_backup_$TIMESTAMP.sql.gz"
LOG_FILE="$BACKUP_DIR/backup.log"
RETENTION_DAYS=7

mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting backup for $DB_NAME..." | tee -a "$LOG_FILE"

# Run pg_dump inside the container
if docker exec "$CONTAINER_NAME" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$FILENAME"; then
    echo "[$(date)] Backup success: $FILENAME" | tee -a "$LOG_FILE"
    
    # Check size
    SIZE=$(du -h "$FILENAME" | cut -f1)
    echo "[$(date)] Backup size: $SIZE" | tee -a "$LOG_FILE"
    
    # Rotate old backups
    echo "[$(date)] Removing backups older than $RETENTION_DAYS days..." | tee -a "$LOG_FILE"
    find "$BACKUP_DIR" -name "fatura_ocr_backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete
    
else
    echo "[$(date)] Backup FAILED!" | tee -a "$LOG_FILE"
    exit 1
fi
