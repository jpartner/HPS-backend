#!/usr/bin/env python3
"""
Seed the HPS backend from parsed-providers.json.
Creates a new category domain, user accounts, provider profiles,
individual services with pricing tiers, gallery images, and schedules.
"""

import json
import sys
import urllib.request
import urllib.error
from pathlib import Path

API_BASE = "http://localhost:8080"
DEFAULT_PASSWORD = "demo1234"

# New category structure for this domain
ESCORT_CATEGORIES = {
    "slug": "companion-services",
    "icon": "heart",
    "translations": {"en": "Companion Services", "pl": "Usługi towarzyskie", "uk": "Ескорт-послуги", "de": "Begleitservices"},
    "imageUrl": "https://images.unsplash.com/photo-1529333166437-7750a6dd5a70?w=600&h=400&fit=crop",
    "children": [
        {"slug": "classic-meeting", "icon": "users", "translations": {"en": "Classic Meeting", "pl": "Spotkanie klasyczne", "uk": "Класична зустріч", "de": "Klassisches Treffen"}},
        {"slug": "massage-services", "icon": "spa", "translations": {"en": "Massage", "pl": "Masaż", "uk": "Масаж", "de": "Massage"}},
        {"slug": "gfe-experience", "icon": "heart", "translations": {"en": "GFE Experience", "pl": "Klimat GFE", "uk": "GFE досвід", "de": "GFE Erlebnis"}},
        {"slug": "duo-services", "icon": "users", "translations": {"en": "Duo / Couple", "pl": "Duet / Para", "uk": "Дует / Пара", "de": "Duo / Paar"}},
        {"slug": "overnight-meeting", "icon": "moon", "translations": {"en": "Overnight", "pl": "Spotkanie całonocne", "uk": "Нічна зустріч", "de": "Übernachtung"}},
        {"slug": "domination", "icon": "shield", "translations": {"en": "Domination", "pl": "Dominacja", "uk": "Домінація", "de": "Domination"}},
        {"slug": "outcall-companion", "icon": "map-pin", "translations": {"en": "Outcall / Travel", "pl": "Wyjazdy", "uk": "Виїзд", "de": "Outcall / Reise"}},
    ],
}

# Map source tags to subcategory slugs
TAG_TO_SUBCATEGORY = {
    "seks klasyczny": "classic-meeting",
    "seks oralny": "classic-meeting",
    "minetka": "classic-meeting",
    "francuz": "classic-meeting",
    "69": "classic-meeting",
    "od tyłu": "classic-meeting",
    "na jeźdźca": "classic-meeting",
    "masaż": "massage-services",
    "masaż klasyczny": "massage-services",
    "masaż relaksacyjny": "massage-services",
    "masaż body to body": "massage-services",
    "masaż gfe": "massage-services",
    "masaż par": "massage-services",
    "klimat gfe": "gfe-experience",
    "namiętne pocałunki": "gfe-experience",
    "pocałunki": "gfe-experience",
    "przytulanie": "gfe-experience",
    "duet z koleżanką": "duo-services",
    "2k+1m": "duo-services",
    "2m+1k": "duo-services",
    "spotkanie całonocne": "overnight-meeting",
    "dominacja": "domination",
    "femdom": "domination",
    "facesitting": "domination",
    "wspólne wyjścia": "outcall-companion",
}


def api_call(method, path, body=None, token=None, lang="en"):
    url = f"{API_BASE}{path}"
    data = json.dumps(body).encode() if body else None
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "Accept-Language": lang,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode()
            return resp.status, json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        body_text = e.read().decode() if e.fp else ""
        try:
            return e.code, json.loads(body_text)
        except json.JSONDecodeError:
            return e.code, {"detail": body_text}


def get_admin_token():
    """Register/login an admin user for creating categories."""
    email = "admin@hps-system.com"
    status, resp = api_call("POST", "/api/v1/auth/register", {
        "email": email, "password": DEFAULT_PASSWORD, "firstName": "Admin"
    })
    if status == 201:
        return resp["accessToken"]
    status, resp = api_call("POST", "/api/v1/auth/login", {
        "email": email, "password": DEFAULT_PASSWORD
    })
    if status == 200:
        return resp["accessToken"]
    print(f"ERROR: Cannot get admin token: {resp}")
    sys.exit(1)


def ensure_categories():
    """Fetch categories and check if our domain exists. Return slug->id map."""
    status, cats = api_call("GET", "/api/v1/categories")
    slug_map = {}
    for cat in cats:
        if cat.get("slug"):
            slug_map[cat["slug"]] = cat["id"]
        for child in cat.get("children", []):
            if child.get("slug"):
                slug_map[child["slug"]] = child["id"]

    if "companion-services" in slug_map:
        print(f"  Category 'Companion Services' already exists")
        return slug_map

    # Need to create via DB seed since there's no admin category API yet
    # For now, we'll use existing categories as fallback
    print("  WARNING: 'Companion Services' category not found.")
    print("  Using existing massage/body-treatments categories as fallback.")
    return slug_map


def find_city_id(city_name, country_code="PL"):
    """Look up a city by name, searching through all regions of a country."""
    if not city_name:
        return None

    # Polish city name mappings to English seed names
    CITY_MAP = {
        "Kraków": "Cracow",
        "Warszawa": "Warsaw",
        "Gdańsk": "Gdansk",
        "Łódź": "Lodz",
        "Wrocław": "Wroclaw",
        "Poznań": "Poznan",
        "Szczecin": "Szczecin",
        "Bydgoszcz": "Bydgoszcz",
        "Lublin": "Lublin",
        "Katowice": "Katowice",
    }

    # Get country code mapping
    country_map = {"Polska": "PL", "Poland": "PL"}
    iso = country_map.get(country_code, country_code)

    # Search names to try
    search_names = [city_name]
    if city_name in CITY_MAP:
        search_names.append(CITY_MAP[city_name])

    # Fetch regions for this country
    status, regions = api_call("GET", f"/api/v1/countries/{iso}/regions", lang="pl")
    if status != 200:
        status, regions = api_call("GET", f"/api/v1/countries/{iso}/regions", lang="en")
    if status != 200:
        return None

    for region in regions:
        status, cities = api_call("GET", f"/api/v1/regions/{region['id']}/cities", lang="pl")
        if status != 200:
            continue
        for city in cities:
            for name in search_names:
                if city["name"].lower() == name.lower():
                    return city["id"]

        # Also check English names
        status, cities_en = api_call("GET", f"/api/v1/regions/{region['id']}/cities", lang="en")
        if status != 200:
            continue
        for city in cities_en:
            for name in search_names:
                if city["name"].lower() == name.lower():
                    return city["id"]

    return None


# Cache city lookups
_city_cache = {}

def get_city_id(city_name, country="PL"):
    key = f"{city_name}:{country}"
    if key not in _city_cache:
        _city_cache[key] = find_city_id(city_name, country)
    return _city_cache[key]


# Known coordinates for Polish cities
CITY_COORDS = {
    "Kraków": (50.0647, 19.9450),
    "Warszawa": (52.2297, 21.0122),
    "Wrocław": (51.1079, 17.0385),
    "Gdańsk": (54.3520, 18.6466),
    "Poznań": (52.4064, 16.9252),
    "Łódź": (51.7592, 19.4560),
    "Katowice": (50.2649, 19.0238),
    "Lublin": (51.2465, 22.5684),
    "Bydgoszcz": (53.1235, 18.0084),
    "Szczecin": (53.4285, 14.5528),
    "Koszalin": (54.1943, 16.1714),
    "Olkusz": (50.2814, 19.5660),
    "Brzesko": (49.9689, 20.6092),
}


def make_email(name):
    safe = name.lower().replace(" ", ".").replace("ó", "o").replace("ą", "a")
    safe = safe.replace("ł", "l").replace("ś", "s").replace("ź", "z").replace("ż", "z")
    safe = safe.replace("ć", "c").replace("ń", "n").replace("ę", "e").replace("ö", "o")
    safe = "".join(c for c in safe if c.isalnum() or c == ".")
    safe = safe[:30]  # Limit length
    return f"{safe}@escort.example.com"


def seed_provider(provider, slug_map):
    name = provider["name"]
    location = provider.get("location", {})
    city = location.get("city", "")
    pricing = provider.get("pricing", {})
    descriptions = provider.get("descriptions", {})
    photos = provider.get("photos", [])
    tags = provider.get("services", [])
    schedule = provider.get("schedule", {})
    attributes = provider.get("attributes", {})

    email = make_email(name)
    print(f"\n  [{name}] ({city or '?'})")

    # 1. Register
    status, resp = api_call("POST", "/api/v1/auth/register", {
        "email": email, "password": DEFAULT_PASSWORD, "firstName": name,
    })
    if status == 201:
        token = resp["accessToken"]
    elif status == 400:
        status, resp = api_call("POST", "/api/v1/auth/login", {
            "email": email, "password": DEFAULT_PASSWORD
        })
        if status == 200:
            token = resp["accessToken"]
            print(f"    Already exists, logged in")
        else:
            print(f"    SKIP: Cannot login: {resp.get('detail', '')}")
            return
    else:
        print(f"    SKIP: Register failed: {resp.get('detail', resp)}")
        return

    # 2. Determine categories from tags
    matched_subcats = set()
    for tag in tags:
        tag_lower = tag.lower().strip()
        for keyword, subcat in TAG_TO_SUBCATEGORY.items():
            if keyword in tag_lower:
                matched_subcats.add(subcat)

    # Use companion-services parent if available, else fallback
    parent_slug = "companion-services"
    if parent_slug in slug_map:
        category_ids = [slug_map[parent_slug]]
    else:
        # Fallback to massage + body-treatments
        category_ids = []
        for fallback in ["massage", "body-treatments"]:
            if fallback in slug_map:
                category_ids.append(slug_map[fallback])
        if not category_ids and slug_map:
            category_ids = [list(slug_map.values())[0]]

    # 3. Build translations
    translations = []
    lang_map = {"PL": "pl", "EN": "en", "DE": "de", "UA": "uk", "RU": "en"}
    for lang_code, desc in descriptions.items():
        mapped = lang_map.get(lang_code, lang_code.lower())
        if desc and len(desc.strip()) > 10:
            translations.append({
                "lang": mapped,
                "businessName": name,
                "description": desc.strip()
            })

    # 4. Create provider profile with location
    city_id = get_city_id(city, location.get("country", "PL")) if city else None
    coords = CITY_COORDS.get(city, (None, None))

    profile_data = {
        "businessName": name,
        "description": descriptions.get("EN") or descriptions.get("PL", f"Professional services in {city}"),
        "isMobile": attributes.get("Wyjazdy", "").lower() == "tak",
        "categoryIds": category_ids,
        "translations": translations,
    }
    if city_id:
        profile_data["cityId"] = city_id
    if coords[0]:
        profile_data["latitude"] = coords[0]
        profile_data["longitude"] = coords[1]

    status, resp = api_call("POST", "/api/v1/providers/me", profile_data, token)

    if status == 201:
        city_info = f"in {city}" if city else "no city"
        print(f"    Profile created ({city_info}{', linked to DB' if city_id else ''})")
    elif "already exists" in str(resp.get("detail", "")):
        print(f"    Profile exists")
    else:
        print(f"    Profile error: {resp.get('detail', resp)}")
        return

    # Re-login to get PROVIDER role
    status, resp = api_call("POST", "/api/v1/auth/login", {
        "email": email, "password": DEFAULT_PASSWORD
    })
    if status == 200:
        token = resp["accessToken"]

    # 5. Create time-based services with incall/outcall variants
    does_outcall = attributes.get("Wyjazdy", "").lower() == "tak"
    base_cat_id = category_ids[0] if category_ids else None
    service_count = 0

    # Subcategory for classic meetings
    classic_cat = slug_map.get("classic-meeting", base_cat_id)

    time_tiers = [
        ("15 min", 15, "Quick Visit", "Szybka wizyta", "Kurzbesuch", "Швидкий візит"),
        ("0,5 godz", 30, "Half Hour", "Pół godziny", "Halbe Stunde", "Півгодини"),
        ("1 godz", 60, "One Hour", "Godzina", "Eine Stunde", "Година"),
        ("Noc", 480, "Overnight", "Całonocne", "Übernachtung", "На ніч"),
    ]

    for price_key, duration, en, pl, de, uk in time_tiers:
        price = pricing.get(price_key)
        if not price or not classic_cat:
            continue

        # Incall service
        svc = {
            "categoryId": classic_cat,
            "pricingType": "FIXED",
            "priceAmount": price,
            "priceCurrency": "PLN",
            "durationMinutes": duration,
            "translations": [
                {"lang": "en", "title": f"{en} — Incall", "description": "At provider's location"},
                {"lang": "pl", "title": f"{pl} — u mnie", "description": "W lokalu"},
                {"lang": "de", "title": f"{de} — Incall", "description": "Beim Anbieter"},
                {"lang": "uk", "title": f"{uk} — у мене", "description": "За адресою"},
            ]
        }
        status, _ = api_call("POST", "/api/v1/providers/me/services", svc, token)
        if status == 201:
            service_count += 1

        # Outcall variant (higher price)
        if does_outcall:
            outcall_price = int(price * 1.3) if duration < 480 else int(price * 1.1)
            svc_out = {
                "categoryId": classic_cat,
                "pricingType": "FIXED",
                "priceAmount": outcall_price,
                "priceCurrency": "PLN",
                "durationMinutes": duration,
                "translations": [
                    {"lang": "en", "title": f"{en} — Outcall", "description": "At your location"},
                    {"lang": "pl", "title": f"{pl} — u Ciebie", "description": "Dojazd do klienta"},
                    {"lang": "de", "title": f"{de} — Outcall", "description": "Bei Ihnen"},
                    {"lang": "uk", "title": f"{uk} — виїзд", "description": "За вашою адресою"},
                ]
            }
            status, _ = api_call("POST", "/api/v1/providers/me/services", svc_out, token)
            if status == 201:
                service_count += 1

    print(f"    {service_count} services created (incall{' + outcall' if does_outcall else ''})")

    # 5b. Store all provider attributes (physical + extras)
    provider_attrs = {}

    # Physical attributes from parsed data
    attr_key_map = {
        "Wzrost": "height",
        "Waga": "weight",
        "Wiek": "age",
        "Biust": "bust",
        "Oczy": "eyes",
        "Włosy": "hair",
        "Języki": "languages",
        "Orientacja": "orientation",
        "Etniczność": "ethnicity",
        "Narodowość": "nationality",
        "Znak zodiaku": "zodiac",
    }
    for pl_key, en_key in attr_key_map.items():
        val = attributes.get(pl_key)
        if val:
            provider_attrs[en_key] = val

    # Extras / offered services as a list
    if tags:
        provider_attrs["offered_services"] = tags

    if provider_attrs:
        api_call("PUT", "/api/v1/providers/me/attributes", provider_attrs, token)
        phys_count = len([k for k in provider_attrs if k != "offered_services"])
        extras_count = len(tags)
        print(f"    Attributes: {phys_count} physical + {extras_count} extras")

    # 6. Add gallery images via URL
    remote_photos = [p["remote_url"] for p in photos if p.get("remote_url")]
    gallery_count = 0
    for photo_url in remote_photos[:10]:
        status, resp = api_call("POST", "/api/v1/providers/me/gallery/url", {
            "url": photo_url,
        }, token)
        if status == 201:
            gallery_count += 1
    if gallery_count:
        print(f"    {gallery_count} gallery images added")

    # 7. Set schedule
    slots = []
    if schedule:
        for day_str, hours in schedule.items():
            try:
                day_num = int(day_str)
                if hours and isinstance(hours, list) and len(hours) == 2:
                    start, end = hours
                    sh, sm = start.split(":") if ":" in start else (start, "00")
                    eh, em = end.split(":") if ":" in end else (end, "00")
                    sm_aligned = (int(sm) // 30) * 30
                    em_aligned = ((int(em) + 29) // 30) * 30
                    if em_aligned >= 60:
                        eh = str(int(eh) + 1)
                        em_aligned = 0
                    start_str = f"{int(sh):02d}:{sm_aligned:02d}"
                    end_str = f"{int(eh):02d}:{em_aligned:02d}"
                    if start_str < end_str:
                        slots.append({"dayOfWeek": day_num, "startTime": start_str, "endTime": end_str})
            except (ValueError, IndexError):
                continue

    if not slots:
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
        print(f"    Schedule: {len(slots)} slots")


def main():
    input_file = Path(__file__).parent / "parsed-providers.json"
    if not input_file.exists():
        print("Run parse-pages.py first")
        sys.exit(1)

    with open(input_file) as f:
        providers = json.load(f)

    print("Fetching categories...")
    slug_map = ensure_categories()
    print(f"  {len(slug_map)} category slugs available")

    print(f"\nSeeding {len(providers)} providers...")
    for provider in providers:
        try:
            seed_provider(provider, slug_map)
        except Exception as e:
            print(f"  ERROR: {e}")
            import traceback
            traceback.print_exc()

    print(f"\nDone!")
    print(f"Login: <name>@escort.example.com / {DEFAULT_PASSWORD}")


if __name__ == "__main__":
    main()
