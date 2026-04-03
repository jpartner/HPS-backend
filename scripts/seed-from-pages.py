#!/usr/bin/env python3
"""
Seed the HPS backend from parsed-providers.json.
Creates user accounts, provider profiles, services, and schedules.
"""

import json
import sys
import urllib.request
import urllib.error
from pathlib import Path

API_BASE = "http://localhost:8080"
DEFAULT_PASSWORD = "demo1234"

# Map source service tags to HPS category slugs
SERVICE_TO_CATEGORY = {
    "masaz": "massage",
    "massage": "massage",
    "manicure": "nails",
    "pedicure": "nails",
    "paznokcie": "nails",
    "makijaz": "makeup",
    "makeup": "makeup",
    "depilacja": "hair-removal",
    "waxing": "hair-removal",
    "golenie": "hair-removal",
    "ogolone": "hair-removal",
    "twarz": "face-skin",
    "facial": "face-skin",
    "cialo": "body-treatments",
    "body": "body-treatments",
    "relaks": "massage",
    "spa": "massage",
}


def api_call(method, path, body=None, token=None):
    """Make an API call and return (status_code, response_body)."""
    url = f"{API_BASE}{path}"
    data = json.dumps(body).encode() if body else None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body_text = e.read().decode() if e.fp else ""
        try:
            return e.code, json.loads(body_text)
        except json.JSONDecodeError:
            return e.code, {"detail": body_text}


def get_categories():
    """Fetch all categories and build a slug->id map."""
    status, data = api_call("GET", "/api/v1/categories")
    if status != 200:
        print(f"ERROR: Cannot fetch categories: {status}")
        sys.exit(1)

    slug_map = {}
    for cat in data:
        if cat.get("slug"):
            slug_map[cat["slug"]] = cat["id"]
        for child in cat.get("children", []):
            if child.get("slug"):
                slug_map[child["slug"]] = child["id"]
    return slug_map


def map_services_to_categories(services, slug_map):
    """Map source service tags to HPS category slugs."""
    matched = set()
    for svc in services:
        svc_lower = svc.lower().strip()
        for keyword, slug in SERVICE_TO_CATEGORY.items():
            if keyword in svc_lower and slug in slug_map:
                matched.add(slug)
                break

    # Default to massage if nothing matched
    if not matched and "massage" in slug_map:
        matched.add("massage")

    return list(matched)


def seed_provider(provider, slug_map):
    """Seed a single provider into the HPS system."""
    name = provider["name"]
    location = provider.get("location", {})
    city = location.get("city", "Kraków")
    pricing = provider.get("pricing", {})
    descriptions = provider.get("descriptions", {})
    photos = provider.get("photos", [])
    services = provider.get("services", [])
    schedule = provider.get("schedule", {})

    # Generate email from name
    safe_name = name.lower().replace(" ", ".").replace("ó", "o").replace("ą", "a")
    safe_name = "".join(c for c in safe_name if c.isalnum() or c == ".")
    email = f"{safe_name}@escort.example.com"

    print(f"\n  [{name}] ({city})")

    # 1. Register
    status, resp = api_call("POST", "/api/v1/auth/register", {
        "email": email,
        "password": DEFAULT_PASSWORD,
        "firstName": name,
    })
    if status == 201:
        token = resp["accessToken"]
        print(f"    Registered: {email}")
    elif status == 400 and "already" in resp.get("detail", ""):
        status2, resp2 = api_call("POST", "/api/v1/auth/login", {
            "email": email,
            "password": DEFAULT_PASSWORD,
        })
        if status2 == 200:
            token = resp2["accessToken"]
            print(f"    Logged in: {email}")
        else:
            print(f"    SKIP: Cannot login {email}: {resp2}")
            return
    else:
        print(f"    SKIP: Register failed: {status} {resp}")
        return

    # 2. Map categories
    matched_slugs = map_services_to_categories(services, slug_map)
    category_ids = [slug_map[s] for s in matched_slugs]

    if not category_ids:
        print(f"    SKIP: No matching categories")
        return

    # 3. Build translations
    translations = []
    for lang_code, desc in descriptions.items():
        lang_map = {"PL": "pl", "EN": "en", "DE": "de", "UA": "uk", "RU": "en"}
        mapped_lang = lang_map.get(lang_code, lang_code.lower())
        if desc and len(desc.strip()) > 10:
            translations.append({
                "lang": mapped_lang,
                "businessName": name,
                "description": desc.strip()
            })

    # 4. Create provider profile
    # Get 1h price or fallback
    hourly_price = pricing.get("1 godz") or pricing.get("0,5 godz") or 200

    gallery_urls = [p.get("remote_url") or p.get("local_path") for p in photos if p.get("remote_url")]

    status, resp = api_call("POST", "/api/v1/providers/me", {
        "businessName": name,
        "description": descriptions.get("EN") or descriptions.get("PL", ""),
        "isMobile": True,
        "categoryIds": category_ids,
        "translations": translations,
    }, token)

    if status == 201:
        print(f"    Provider profile created, categories: {matched_slugs}")
    elif status == 400 and "already exists" in resp.get("detail", ""):
        print(f"    Provider profile already exists")
    else:
        print(f"    ERROR creating profile: {status} {resp.get('detail', resp)}")
        return

    # Re-login to get PROVIDER role token
    status, resp = api_call("POST", "/api/v1/auth/login", {
        "email": email,
        "password": DEFAULT_PASSWORD,
    })
    if status == 200:
        token = resp["accessToken"]

    # 5. Create services (one per matched category)
    for slug in matched_slugs:
        cat_id = slug_map[slug]
        # Find a subcategory if possible
        sub_slug = slug  # Use parent if no sub
        for key in SERVICE_TO_CATEGORY:
            if SERVICE_TO_CATEGORY[key] == slug:
                # Check if there's a subcategory version
                possible_sub = f"{slug.split('-')[0]}"
                break

        svc_data = {
            "categoryId": cat_id,
            "pricingType": "HOURLY",
            "priceAmount": hourly_price,
            "priceCurrency": "PLN",
            "durationMinutes": 60,
            "translations": [
                {"lang": "en", "title": f"{slug.replace('-', ' ').title()} Session", "description": f"Professional {slug.replace('-', ' ')} service"},
                {"lang": "pl", "title": f"Sesja {slug.replace('-', ' ').title()}", "description": f"Profesjonalna usługa {slug.replace('-', ' ')}"},
            ]
        }

        status, resp = api_call("POST", "/api/v1/providers/me/services", svc_data, token)
        if status == 201:
            print(f"    Service: {slug} ({hourly_price} PLN/h)")
        else:
            print(f"    Service {slug} failed: {status} {resp.get('detail', '')}")

    # 6. Set schedule
    slots = []
    if schedule:
        for day_str, hours in schedule.items():
            try:
                day_num = int(day_str)
                if hours and isinstance(hours, list) and len(hours) == 2:
                    start, end = hours
                    # Align to 30 min
                    start_parts = start.split(":")
                    end_parts = end.split(":")
                    start_min = int(start_parts[1]) if len(start_parts) > 1 else 0
                    end_min = int(end_parts[1]) if len(end_parts) > 1 else 0
                    start_aligned = f"{int(start_parts[0]):02d}:{(start_min // 30) * 30:02d}"
                    end_aligned = f"{int(end_parts[0]):02d}:{((end_min + 29) // 30) * 30:02d}"
                    if end_aligned == f"{int(end_parts[0]):02d}:60":
                        end_aligned = f"{int(end_parts[0]) + 1:02d}:00"
                    slots.append({"dayOfWeek": day_num, "startTime": start_aligned, "endTime": end_aligned})
            except (ValueError, IndexError):
                continue

    if not slots:
        # Default Mon-Sat 10:00-22:00
        for day in range(1, 7):
            slots.append({"dayOfWeek": day, "startTime": "10:00", "endTime": "22:00"})

    status, resp = api_call("PUT", "/api/v1/providers/me/schedule/weekly", {
        "timezone": "Europe/Warsaw",
        "incallGapMinutes": 30,
        "outcallGapMinutes": 60,
        "minLeadTimeHours": 1,
        "slots": slots,
    }, token)

    if status == 200:
        print(f"    Schedule set: {len(slots)} slots")
    else:
        print(f"    Schedule failed: {status}")


def main():
    input_file = Path(__file__).parent / "parsed-providers.json"
    if not input_file.exists():
        print("ERROR: Run parse-pages.py first to generate parsed-providers.json")
        sys.exit(1)

    with open(input_file) as f:
        providers = json.load(f)

    print(f"Fetching HPS categories...")
    slug_map = get_categories()
    print(f"Found {len(slug_map)} category slugs")

    print(f"\nSeeding {len(providers)} providers...")
    for provider in providers:
        try:
            seed_provider(provider, slug_map)
        except Exception as e:
            print(f"  ERROR: {e}")

    print(f"\nDone! Seeded {len(providers)} providers.")
    print(f"Login with any email @escort.example.com / password: {DEFAULT_PASSWORD}")


if __name__ == "__main__":
    main()
