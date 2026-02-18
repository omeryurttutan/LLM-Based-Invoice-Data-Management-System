#!/bin/bash
set -e

# Configuration
ENV_FILE=".env.prod"
COMPOSE_FILE="docker-compose.prod.yml"

echo "🚀 Starting Deployment Process..."

# 1. Pull latest code
echo "📦 Pulling latest code..."
git pull origin main

# 2. Check for required files
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ Error: $ENV_FILE not found! Please create it from .env.example."
    exit 1
fi

# 3. Build Production Images
echo "🏗️  Building production images..."
docker compose -f "$COMPOSE_FILE" build

# 4. Deployment
echo "🚀 Deploying stack..."
# Using --remove-orphans to clean up old containers
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

# 5. Health Check
echo "hz Waiting for services to be healthy..."
sleep 15
if docker compose -f "$COMPOSE_FILE" ps | grep "unhealthy"; then
    echo "❌ Deployment FAILED: Some services are unhealthy."
    docker compose -f "$COMPOSE_FILE" ps
    exit 1
fi

echo "✅ Deployment Successful!"
docker compose -f "$COMPOSE_FILE" ps
