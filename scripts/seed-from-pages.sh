#!/usr/bin/env bash
#
# Seed the HPS backend from parsed-providers.json
# Reads provider data extracted from escort.club HTML pages and creates:
#   1. User accounts (register)
#   2. Provider profiles
#   3. Services (mapped to HPS categories)
#   4. Weekly schedules
#
# Prerequisites:
#   - HPS backend running at localhost:8080
#   - parsed-providers.json exists (run parse-pages.py first)
#   - jq installed
#   - curl installed
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_BASE="http://localhost:8080/api/v1"
DATA_FILE="${SCRIPT_DIR}/parsed-providers.json"
PASSWORD="TestPass123!"

# ── Helpers ──────────────────────────────────────────────────────

log()  { printf "\033[1;34m[INFO]\033[0m  %s\n" "$*"; }
ok()   { printf "\033[1;32m[OK]\033[0m    %s\n" "$*"; }
warn() { printf "\033[1;33m[WARN]\033[0m  %s\n" "$*" >&2; }
err()  { printf "\033[1;31m[ERROR]\033[0m %s\n" "$*" >&2; }

# Make an API call and return the response body.  Sets $HTTP_CODE as side effect.
api_call() {
    local method="$1" url="$2" body="${3:-}" token="${4:-}"
    local headers=(-H "Content-Type: application/json" -H "Accept-Language: en")
    [[ -n "$token" ]] && headers+=(-H "Authorization: Bearer $token")

    local tmp
    tmp=$(mktemp)
    if [[ -n "$body" ]]; then
        HTTP_CODE=$(curl -s -o "$tmp" -w "%{http_code}" -X "$method" "${headers[@]}" -d "$body" "$url")
    else
        HTTP_CODE=$(curl -s -o "$tmp" -w "%{http_code}" -X "$method" "${headers[@]}" "$url")
    fi
    cat "$tmp"
    rm -f "$tmp"
}

# ── Fetch categories once ────────────────────────────────────────

log "Fetching HPS categories..."
CATEGORIES_JSON=$(api_call GET "${API_BASE}/categories")
if [[ "$HTTP_CODE" != "200" ]]; then
    err "Cannot fetch categories (HTTP $HTTP_CODE). Is the backend running?"
    echo "$CATEGORIES_JSON"
    exit 1
fi

# Build a lookup: slug -> id   (flatten parent + children)
CATEGORY_MAP=$(echo "$CATEGORIES_JSON" | jq -r '
    [.[] | {slug, id}, (.children[]? | {slug, id})] | from_entries
')

ok "Loaded categories"

# Map source service tags to HPS category slugs.
# Since the source services are escort/companion services, we remap them
# to the closest HPS beauty/wellness categories.
map_service_to_category() {
    local svc="$1"
    local lower
    lower=$(echo "$svc" | tr '[:upper:]' '[:lower:]')

    case "$lower" in
        *masaż*|*massage*|*body*to*body*|*masaz*|*relaks*)
            echo "massage" ;;
        *manicure*|*paznok*|*nails*)
            echo "nails" ;;
        *włos*|*hair*|*fryzur*)
            echo "hair" ;;
        *twarz*|*face*|*skin*|*peel*|*pielęg*)
            echo "face-skin" ;;
        *brwi*|*rzęs*|*lash*|*brow*)
            echo "eyebrows-lashes" ;;
        *depilac*|*wax*|*removal*|*ogolone*)
            echo "hair-removal" ;;
        *makijaż*|*makeup*|*make-up*)
            echo "makeup" ;;
        *)
            # Default: map to body-treatments as a catch-all for companion services
            echo "body-treatments" ;;
    esac
}

get_category_id() {
    local slug="$1"
    echo "$CATEGORY_MAP" | jq -r --arg s "$slug" '.[$s] // empty'
}

# ── Pre-check ────────────────────────────────────────────────────

if [[ ! -f "$DATA_FILE" ]]; then
    err "Data file not found: $DATA_FILE"
    err "Run parse-pages.py first."
    exit 1
fi

PROVIDER_COUNT=$(jq length "$DATA_FILE")
log "Found $PROVIDER_COUNT providers to seed"

# ── Main loop ────────────────────────────────────────────────────

jq -c '.[]' "$DATA_FILE" | while IFS= read -r provider_json; do
    NAME=$(echo "$provider_json" | jq -r '.name')
    AGE=$(echo "$provider_json" | jq -r '.age // empty')
    CITY=$(echo "$provider_json" | jq -r '.location.city // empty')
    PHONE=$(echo "$provider_json" | jq -r '.contact.phone // empty')

    log "────────────────────────────────────────"
    log "Processing: $NAME (age=$AGE, city=$CITY)"

    # ── 1. Register user ─────────────────────────────────────────

    # Generate a unique email from the name
    SLUG=$(echo "$NAME" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/_/g' | sed 's/__*/_/g' | sed 's/^_//;s/_$//')
    EMAIL="${SLUG}@hps-seed.local"

    log "  Registering user: $EMAIL"

    # Use PL description as firstName, or name itself
    FIRST_NAME="$NAME"
    LAST_NAME=""

    REG_BODY=$(jq -n \
        --arg email "$EMAIL" \
        --arg password "$PASSWORD" \
        --arg firstName "$FIRST_NAME" \
        --arg preferredLang "pl" \
        '{email: $email, password: $password, firstName: $firstName, preferredLang: $preferredLang}')

    REG_RESPONSE=$(api_call POST "${API_BASE}/auth/register" "$REG_BODY")

    if [[ "$HTTP_CODE" == "201" ]]; then
        ACCESS_TOKEN=$(echo "$REG_RESPONSE" | jq -r '.accessToken')
        ok "  Registered $EMAIL"
    elif [[ "$HTTP_CODE" == "409" ]]; then
        warn "  User already exists, attempting login..."
        LOGIN_BODY=$(jq -n --arg email "$EMAIL" --arg password "$PASSWORD" '{email: $email, password: $password}')
        REG_RESPONSE=$(api_call POST "${API_BASE}/auth/login" "$LOGIN_BODY")
        if [[ "$HTTP_CODE" == "200" ]]; then
            ACCESS_TOKEN=$(echo "$REG_RESPONSE" | jq -r '.accessToken')
            ok "  Logged in as $EMAIL"
        else
            err "  Cannot login as $EMAIL (HTTP $HTTP_CODE)"
            echo "$REG_RESPONSE" | head -5 >&2
            continue
        fi
    else
        err "  Registration failed (HTTP $HTTP_CODE)"
        echo "$REG_RESPONSE" | head -5 >&2
        continue
    fi

    # ── 2. Create provider profile ───────────────────────────────

    log "  Creating provider profile..."

    # Build description from available translations
    DESC_PL=$(echo "$provider_json" | jq -r '.descriptions.PL // empty')
    DESC_EN=$(echo "$provider_json" | jq -r '.descriptions.EN // empty')
    DESC_DE=$(echo "$provider_json" | jq -r '.descriptions.DE // empty')

    # Use EN description as the default if available, otherwise PL
    DEFAULT_DESC="${DESC_EN:-$DESC_PL}"

    # Collect category IDs for services this provider offers
    CATEGORY_IDS="[]"
    SEEN_CATS=""
    while IFS= read -r svc; do
        [[ -z "$svc" ]] && continue
        cat_slug=$(map_service_to_category "$svc")
        # Deduplicate
        if [[ "$SEEN_CATS" == *"|${cat_slug}|"* ]]; then
            continue
        fi
        SEEN_CATS="${SEEN_CATS}|${cat_slug}|"
        cat_id=$(get_category_id "$cat_slug")
        if [[ -n "$cat_id" ]]; then
            CATEGORY_IDS=$(echo "$CATEGORY_IDS" | jq --arg id "$cat_id" '. + [$id]')
        fi
    done < <(echo "$provider_json" | jq -r '.services[]')

    # Build translations array
    TRANSLATIONS="[]"
    if [[ -n "$DESC_PL" ]]; then
        TRANSLATIONS=$(echo "$TRANSLATIONS" | jq --arg d "$DESC_PL" --arg n "$NAME" \
            '. + [{"lang": "pl", "businessName": $n, "description": $d}]')
    fi
    if [[ -n "$DESC_EN" ]]; then
        TRANSLATIONS=$(echo "$TRANSLATIONS" | jq --arg d "$DESC_EN" --arg n "$NAME" \
            '. + [{"lang": "en", "businessName": $n, "description": $d}]')
    fi
    if [[ -n "$DESC_DE" ]]; then
        TRANSLATIONS=$(echo "$TRANSLATIONS" | jq --arg d "$DESC_DE" --arg n "$NAME" \
            '. + [{"lang": "de", "businessName": $n, "description": $d}]')
    fi

    PROVIDER_BODY=$(jq -n \
        --arg businessName "$NAME" \
        --arg description "$DEFAULT_DESC" \
        --arg isMobile "false" \
        --argjson categoryIds "$CATEGORY_IDS" \
        --argjson translations "$TRANSLATIONS" \
        '{
            businessName: $businessName,
            description: $description,
            isMobile: false,
            categoryIds: $categoryIds,
            translations: $translations
        }')

    PROVIDER_RESPONSE=$(api_call POST "${API_BASE}/providers/me" "$PROVIDER_BODY" "$ACCESS_TOKEN")

    if [[ "$HTTP_CODE" == "201" ]]; then
        PROVIDER_ID=$(echo "$PROVIDER_RESPONSE" | jq -r '.id')
        ok "  Created provider profile: $PROVIDER_ID"
    elif [[ "$HTTP_CODE" == "409" ]]; then
        warn "  Provider profile already exists"
        # Try to get existing provider ID from the response or by other means
        PROVIDER_ID=""
    else
        err "  Failed to create provider profile (HTTP $HTTP_CODE)"
        echo "$PROVIDER_RESPONSE" | head -5 >&2
        continue
    fi

    # ── 3. Create services ───────────────────────────────────────

    log "  Creating services..."

    # Get the 1-hour price as a baseline, fallback to base_price
    HOUR_PRICE=$(echo "$provider_json" | jq -r '.pricing["1 godz"] // .base_price_pln // 200')

    # Deduplicate: only create one service per category
    CREATED_CATS=""
    SVC_COUNT=0
    while IFS= read -r svc; do
        [[ -z "$svc" ]] && continue
        cat_slug=$(map_service_to_category "$svc")
        # Only create one service per category
        if [[ "$CREATED_CATS" == *"|${cat_slug}|"* ]]; then
            continue
        fi
        CREATED_CATS="${CREATED_CATS}|${cat_slug}|"

        cat_id=$(get_category_id "$cat_slug")
        if [[ -z "$cat_id" ]]; then
            warn "    No category ID for slug: $cat_slug"
            continue
        fi

        # Determine price: massage-related get a specific price, others get the hourly rate
        case "$cat_slug" in
            massage)      SVC_PRICE=$HOUR_PRICE; DURATION=60 ;;
            body-treatments) SVC_PRICE=$HOUR_PRICE; DURATION=60 ;;
            hair-removal) SVC_PRICE=$(( HOUR_PRICE / 3 )); DURATION=30 ;;
            *)            SVC_PRICE=$HOUR_PRICE; DURATION=60 ;;
        esac

        # Convert PLN to EUR (rough 1 EUR = 4.3 PLN)
        SVC_PRICE_EUR=$(echo "scale=0; $SVC_PRICE / 4" | bc 2>/dev/null || echo "$SVC_PRICE")

        # Build service translation
        SVC_TRANSLATIONS=$(jq -n \
            --arg title "$svc" \
            '[{"lang": "pl", "title": $title}, {"lang": "en", "title": $title}]')

        SVC_BODY=$(jq -n \
            --arg categoryId "$cat_id" \
            --arg pricingType "FIXED" \
            --argjson priceAmount "$SVC_PRICE_EUR" \
            --arg priceCurrency "PLN" \
            --argjson durationMinutes "$DURATION" \
            --argjson translations "$SVC_TRANSLATIONS" \
            '{
                categoryId: $categoryId,
                pricingType: $pricingType,
                priceAmount: $priceAmount,
                priceCurrency: $priceCurrency,
                durationMinutes: $durationMinutes,
                translations: $translations
            }')

        SVC_RESPONSE=$(api_call POST "${API_BASE}/providers/me/services" "$SVC_BODY" "$ACCESS_TOKEN")

        if [[ "$HTTP_CODE" == "201" ]]; then
            SVC_COUNT=$((SVC_COUNT + 1))
        else
            warn "    Failed to create service '$svc' (HTTP $HTTP_CODE)"
        fi
    done < <(echo "$provider_json" | jq -r '.services[]')

    ok "  Created $SVC_COUNT services"

    # ── 4. Set weekly schedule ───────────────────────────────────

    log "  Setting weekly schedule..."

    SLOTS="[]"
    for day in 1 2 3 4 5 6 7; do
        START=$(echo "$provider_json" | jq -r --arg d "$day" '.schedule[$d].start // empty')
        END=$(echo "$provider_json" | jq -r --arg d "$day" '.schedule[$d].end // empty')
        if [[ -n "$START" && -n "$END" ]]; then
            SLOTS=$(echo "$SLOTS" | jq \
                --argjson dow "$day" \
                --arg start "$START" \
                --arg end "$END" \
                '. + [{"dayOfWeek": $dow, "startTime": $start, "endTime": $end}]')
        fi
    done

    SCHEDULE_BODY=$(jq -n \
        --arg timezone "Europe/Warsaw" \
        --argjson slots "$SLOTS" \
        '{timezone: $timezone, incallGapMinutes: 15, outcallGapMinutes: 60, minLeadTimeHours: 2, slots: $slots}')

    SCHED_RESPONSE=$(api_call PUT "${API_BASE}/providers/me/schedule/weekly" "$SCHEDULE_BODY" "$ACCESS_TOKEN")

    if [[ "$HTTP_CODE" == "200" ]]; then
        SLOT_COUNT=$(echo "$SCHED_RESPONSE" | jq '.slots | length')
        ok "  Set schedule with $SLOT_COUNT time slots"
    else
        warn "  Failed to set schedule (HTTP $HTTP_CODE)"
        echo "$SCHED_RESPONSE" | head -3 >&2
    fi

    ok "Done with $NAME"
done

log "════════════════════════════════════════"
ok "Seeding complete!"
