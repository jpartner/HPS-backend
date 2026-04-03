#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

usage() {
    echo "Usage: $0 {start|stop|restart|update|logs|reset}"
    echo ""
    echo "  start    - Build and start local test instance (persistent DB)"
    echo "  stop     - Stop containers (data is kept)"
    echo "  restart  - Restart containers without rebuilding"
    echo "  update   - Rebuild app and restart (DB data preserved)"
    echo "  logs     - Tail logs from all containers"
    echo "  reset    - Stop and destroy all data volumes"
    exit 1
}

[ $# -ge 1 ] || usage

case "$1" in
    start)
        "$SCRIPT_DIR/build.sh"
        echo "==> Starting local test environment (persistent DB)..."
        docker compose -f "$PROJECT_DIR/docker/docker-compose.local.yml" -p hps-local up -d
        echo "==> Local test running at http://localhost:8081"
        ;;
    stop)
        echo "==> Stopping local test environment (data preserved)..."
        docker compose -f "$PROJECT_DIR/docker/docker-compose.local.yml" -p hps-local stop
        ;;
    restart)
        echo "==> Restarting local test environment..."
        docker compose -f "$PROJECT_DIR/docker/docker-compose.local.yml" -p hps-local restart
        echo "==> Local test running at http://localhost:8081"
        ;;
    update)
        "$SCRIPT_DIR/build.sh"
        echo "==> Updating app container (DB data preserved)..."
        docker compose -f "$PROJECT_DIR/docker/docker-compose.local.yml" -p hps-local up -d
        echo "==> Local test running at http://localhost:8081"
        ;;
    logs)
        docker compose -f "$PROJECT_DIR/docker/docker-compose.local.yml" -p hps-local logs -f
        ;;
    reset)
        echo "==> Destroying local test environment and all data..."
        docker compose -f "$PROJECT_DIR/docker/docker-compose.local.yml" -p hps-local down -v
        echo "==> Done. All local test data removed."
        ;;
    *)
        usage
        ;;
esac
