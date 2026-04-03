#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "==> Building application..."
./gradlew clean bootJar -x test

echo "==> Building Docker image..."
docker build -t hps-system:latest .

echo "==> Done. Image: hps-system:latest"
