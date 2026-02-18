#!/bin/bash
set -e

if [ -z "$1" ]; then
    echo "Usage: ./restore-db.sh <backup_file_path>"
    exit 1
fi

BACKUP_FILE="$1"
CONTAINER_NAME="postgres_prod"
DB_USER="fatura_user"
DB_NAME="fatura_ocr_db"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "⚠️  WARNING: This will OVERWRITE the current database '$DB_NAME'."
read -p "Are you sure you want to proceed? (y/n): " confirm
if [[ "$confirm" != "y" ]]; then
    echo "Restore cancelled."
    exit 0
fi

echo "Starting restore from $BACKUP_FILE..."

# Stop backend connections (optional, might fail if active)
# docker stop backend_prod || true

# Drop and Recreate DB
echo "Recreating database..."
docker exec "$CONTAINER_NAME" dropdb -U "$DB_USER" --if-exists "$DB_NAME"
docker exec "$CONTAINER_NAME" createdb -U "$DB_USER" "$DB_NAME"

# Restore
echo "Restoring data..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"

echo "✅ Restore completed successfully."
# docker start backend_prod || true
