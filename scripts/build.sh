#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "==> Building application..."
./gradlew clean bootJar -x test

echo "==> Building backend Docker image..."
docker build -t hps-system:latest .

echo "==> Building frontend..."
cd "$PROJECT_DIR/frontend"
docker build -t hps-frontend:latest .

echo "==> Done. Images: hps-system:latest, hps-frontend:latest"
