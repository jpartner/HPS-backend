#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

usage() {
    echo "Usage: $0 {start|stop|restart|logs}"
    echo ""
    echo "  start    - Build and start dev instance (empty DB every time)"
    echo "  stop     - Stop and remove dev containers"
    echo "  restart  - Rebuild and restart"
    echo "  logs     - Tail logs from all containers"
    exit 1
}

[ $# -ge 1 ] || usage

case "$1" in
    start)
        "$SCRIPT_DIR/build.sh"
        echo "==> Starting dev environment (ephemeral DB)..."
        docker compose -f "$PROJECT_DIR/docker/docker-compose.dev.yml" -p hps-dev up -d
        echo "==> Dev running: API http://localhost:8080 | Frontend http://localhost:3000 | Admin http://localhost:3002"
        ;;
    stop)
        echo "==> Stopping dev environment..."
        docker compose -f "$PROJECT_DIR/docker/docker-compose.dev.yml" -p hps-dev down
        ;;
    restart)
        docker compose -f "$PROJECT_DIR/docker/docker-compose.dev.yml" -p hps-dev down
        "$SCRIPT_DIR/build.sh"
        docker compose -f "$PROJECT_DIR/docker/docker-compose.dev.yml" -p hps-dev up -d
        echo "==> Dev running: API http://localhost:8080 | Frontend http://localhost:3000 | Admin http://localhost:3002"
        ;;
    logs)
        docker compose -f "$PROJECT_DIR/docker/docker-compose.dev.yml" -p hps-dev logs -f
        ;;
    *)
        usage
        ;;
esac
