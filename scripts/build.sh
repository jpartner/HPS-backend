#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

API_URL="${1:-http://localhost:8080}"
TAG="${2:-latest}"

cd "$PROJECT_DIR"

echo "==> Building application..."
./gradlew clean bootJar -x test

echo "==> Building backend Docker image..."
docker build -t hps-system:$TAG .

echo "==> Building frontend (API_URL=$API_URL)..."
cd "$PROJECT_DIR/frontend"
docker build --build-arg NEXT_PUBLIC_API_URL="$API_URL" -t hps-frontend:$TAG .

echo "==> Building admin (API_URL=$API_URL)..."
cd "$PROJECT_DIR/admin"
docker build --build-arg NEXT_PUBLIC_API_URL="$API_URL" -t hps-admin:$TAG .

echo "==> Done. Images tagged ':$TAG' with API_URL=$API_URL"
