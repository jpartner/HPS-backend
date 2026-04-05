#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

usage() {
    echo "Usage: $0 <api-url> <config-file>"
    echo ""
    echo "  Import a tenant config into a running HPS instance."
    echo "  Requires SUPER_ADMIN credentials in env vars:"
    echo "    HPS_ADMIN_EMAIL    (default: admin@hps.local)"
    echo "    HPS_ADMIN_PASSWORD (default: admin1234)"
    echo ""
    echo "  Examples:"
    echo "    $0 http://localhost:8080 config/default-tenant.json"
    echo "    $0 http://localhost:8081 config/default-tenant.json"
    exit 1
}

[ $# -ge 2 ] || usage

API_URL="$1"
CONFIG_FILE="$2"

if [ ! -f "$CONFIG_FILE" ]; then
    # Try relative to project dir
    CONFIG_FILE="$PROJECT_DIR/$CONFIG_FILE"
fi

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Config file not found: $2"
    exit 1
fi

EMAIL="${HPS_ADMIN_EMAIL:-admin@hps.local}"
PASSWORD="${HPS_ADMIN_PASSWORD:-admin1234}"

echo "==> Logging in as $EMAIL at $API_URL..."
TOKEN=$(curl -sf "$API_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "==> Importing tenant config from $CONFIG_FILE..."
RESULT=$(curl -sf "$API_URL/api/v1/admin/tenants/import" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $TOKEN" \
    -d @"$CONFIG_FILE")

echo "$RESULT" | python3 -m json.tool
echo "==> Done."
