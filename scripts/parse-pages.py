#!/usr/bin/env python3
"""
Parse saved HTML pages from escort.club to extract provider data.
Outputs parsed-providers.json with structured provider information.
"""

import json
import os
import re
import sys
from pathlib import Path
from bs4 import BeautifulSoup

PAGES_DIR = Path(__file__).parent.parent / "pages"
OUTPUT_FILE = Path(__file__).parent / "parsed-providers.json"

# Polish day names to ISO day-of-week (1=Monday, 7=Sunday)
DAY_MAP = {
    "poniedziałek": 1,
    "wtorek": 2,
    "środa": 3,
    "sroda": 3,
    "czwartek": 4,
    "piątek": 5,
    "piatek": 5,
    "sobota": 6,
    "niedziela": 7,
}


def parse_schedule_hours(text: str):
    """Parse Polish schedule text into start/end times.
    Returns (start, end) or None if unparseable.
    'cały czas' means 24h availability.
    """
    text = text.strip().lower()
    if not text or text == "-":
        return None
    if "cały czas" in text or "caly czas" in text:
        return ("00:00", "23:59")
    # Try to parse "HH:MM - HH:MM" or "HH - HH" patterns
    m = re.search(r"(\d{1,2}[:.]\d{2})\s*[-–]\s*(\d{1,2}[:.]\d{2})", text)
    if m:
        return (m.group(1).replace(".", ":"), m.group(2).replace(".", ":"))
    m = re.search(r"(\d{1,2})\s*[-–]\s*(\d{1,2})", text)
    if m:
        return (f"{int(m.group(1)):02d}:00", f"{int(m.group(2)):02d}:00")
    return None


def parse_price(text: str):
    """Extract price in PLN from text like '600 PLN' or '400 zł'."""
    text = text.strip()
    m = re.search(r"(\d[\d\s]*)\s*(?:PLN|zł|złotych|pln)", text, re.IGNORECASE)
    if m:
        return int(m.group(1).replace(" ", ""))
    # Just a number
    m = re.search(r"(\d+)", text)
    if m:
        return int(m.group(1))
    return None


def extract_phone_from_whatsapp(soup: BeautifulSoup) -> str | None:
    """Extract phone number from WhatsApp link."""
    wa_link = soup.find("a", href=re.compile(r"wa\.me/"))
    if wa_link:
        m = re.search(r"wa\.me/(\d+)", wa_link["href"])
        if m:
            raw = m.group(1)
            # Format as +XX XXX-XXX-XXX
            if raw.startswith("48"):
                return f"+{raw[:2]} {raw[2:5]}-{raw[5:8]}-{raw[8:]}"
            return f"+{raw}"
    return None


def extract_phone_from_data(soup: BeautifulSoup) -> str | None:
    """Try to get phone from the masked phone element."""
    phone_elem = soup.find("a", attrs={"data-show-phone": ""})
    if phone_elem:
        text = phone_elem.get_text(strip=True)
        if text and "..." not in text:
            return text
    return None


def parse_html_file(filepath: str) -> dict | None:
    """Parse a single HTML file and return extracted provider data."""
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
    except Exception as e:
        print(f"  [ERROR] Cannot read {filepath}: {e}", file=sys.stderr)
        return None

    soup = BeautifulSoup(content, "html.parser")
    provider = {}

    # --- Name ---
    h1 = soup.find("h1")
    if h1:
        provider["name"] = h1.get_text(strip=True)
    else:
        # Fallback: from title tag
        title = soup.find("title")
        if title:
            m = re.match(r"^(.+?)\s+\d+\s+lat", title.get_text(strip=True))
            if m:
                provider["name"] = m.group(1).strip()
    if not provider.get("name"):
        provider["name"] = Path(filepath).stem.split(" - ")[0].strip()

    # --- JSON-LD structured data ---
    ld_script = soup.find("script", type="application/ld+json")
    ld_data = {}
    if ld_script:
        try:
            ld_data = json.loads(ld_script.string)
        except Exception:
            pass

    # --- Location ---
    location = {}
    if ld_data.get("location", {}).get("address"):
        addr = ld_data["location"]["address"]
        location["city"] = addr.get("addressLocality", "")
        location["country"] = addr.get("addressCountry", "PL")
    # Also try from content-location span
    loc_span = soup.find("div", class_="content-location")
    if loc_span:
        links = loc_span.find_all("a")
        parts = [a.get_text(strip=True) for a in links if a.get_text(strip=True)]
        if parts:
            location["country"] = parts[0] if len(parts) >= 1 else ""
            if len(parts) >= 2:
                location["region"] = parts[1]
            if len(parts) >= 3:
                location["city"] = parts[2]
            elif len(parts) == 2:
                # Sometimes region IS the city
                location["city"] = parts[-1]
    provider["location"] = location

    # --- Attributes (age, height, weight, etc.) ---
    attributes = {}
    content_hours_div = soup.find("div", class_="content-hours")
    if content_hours_div:
        stat_elems = content_hours_div.find_all("div", class_="stat-elem")
        for elem in stat_elems:
            label_div = elem.find("div", class_="sub-label")
            desc_div = elem.find("div", class_="sub-desc")
            if label_div and desc_div:
                key = label_div.get_text(strip=True).rstrip(":")
                val = desc_div.get_text(strip=True)
                if val:
                    attributes[key] = val

    # Extract age from attributes or title
    age = None
    age_text = attributes.get("Wiek", "")
    m = re.search(r"(\d+)", age_text)
    if m:
        age = int(m.group(1))
    else:
        # Try from title
        title = soup.find("title")
        if title:
            m = re.search(r"(\d+)\s+lat", title.get_text())
            if m:
                age = int(m.group(1))
    provider["age"] = age
    provider["attributes"] = attributes

    # --- Description (O mnie) ---
    descriptions = {}
    tab_content = soup.find("div", id="isoTabsContent")
    if tab_content:
        panes = tab_content.find_all("div", class_="tab-pane")
        for pane in panes:
            lang_id = pane.get("id", "").strip()
            text = pane.get_text(strip=True)
            if text and lang_id:
                descriptions[lang_id] = text
    else:
        # Fallback: try content-desc
        desc_div = soup.find("div", class_="content-desc")
        if desc_div:
            text = desc_div.get_text(strip=True)
            if text:
                # Remove "O mnie" label
                text = re.sub(r"^O mnie\s*", "", text)
                if text:
                    descriptions["PL"] = text
    provider["descriptions"] = descriptions

    # --- Pricing ---
    pricing = {}
    prices_div = soup.find("div", class_="contant-prices")
    if prices_div:
        stat_elems = prices_div.find_all("div", class_="stat-elem")
        for elem in stat_elems:
            label_div = elem.find("div", class_="sub-label")
            desc_div = elem.find("div", class_="sub-desc")
            if label_div and desc_div:
                duration = label_div.get_text(strip=True).rstrip(":")
                price_text = desc_div.get_text(strip=True)
                price = parse_price(price_text)
                if price:
                    pricing[duration] = price
    # Also get base price from JSON-LD
    if ld_data.get("offers", {}).get("price"):
        try:
            provider["base_price_pln"] = int(ld_data["offers"]["price"])
        except (ValueError, TypeError):
            pass
    provider["pricing"] = pricing

    # --- Services / Preferences ---
    services = []
    tags_box = soup.find("div", class_="sub-info-box -tags")
    if tags_box:
        tag_links = tags_box.find_all("a", class_="tag")
        for tag in tag_links:
            services.append(tag.get_text(strip=True))
    provider["services"] = services

    # --- Schedule ---
    schedule = {}
    schedule_box = soup.find("div", class_="sub-info-box -info")
    if schedule_box:
        elems = schedule_box.find_all("div", class_="sub-info-elem")
        for elem in elems:
            label_span = elem.find("span", class_="sub-label")
            desc_span = elem.find("span", class_="desc")
            if label_span and desc_span:
                day_name = label_span.get_text(strip=True).rstrip(":").lower()
                hours_text = desc_span.get_text(strip=True)
                day_num = DAY_MAP.get(day_name)
                if day_num:
                    parsed = parse_schedule_hours(hours_text)
                    if parsed:
                        schedule[day_num] = {"start": parsed[0], "end": parsed[1]}
    provider["schedule"] = schedule

    # --- Photos ---
    photos = []
    # Main gallery images
    gallery = soup.find("div", class_="galleryContainer")
    if gallery:
        slides = gallery.find_all("li", class_="lslide")
        for slide in slides:
            img = slide.find("img")
            if img:
                src = img.get("src", "")
                # Get the data-thumb for the remote URL
                thumb = slide.get("data-thumb", "")
                photos.append({
                    "local_path": src,
                    "remote_url": thumb if thumb else src,
                })
    provider["photos"] = photos

    # --- Contact ---
    contact = {}
    phone = extract_phone_from_whatsapp(soup) or extract_phone_from_data(soup)
    if phone:
        contact["phone"] = phone
    # WhatsApp link
    wa_link = soup.find("a", href=re.compile(r"wa\.me/"))
    if wa_link:
        contact["whatsapp"] = wa_link["href"]
    provider["contact"] = contact

    # --- Source file ---
    provider["source_file"] = os.path.basename(filepath)

    return provider


def main():
    if not PAGES_DIR.exists():
        print(f"Pages directory not found: {PAGES_DIR}", file=sys.stderr)
        sys.exit(1)

    html_files = sorted(PAGES_DIR.glob("*.html"))
    if not html_files:
        print(f"No HTML files found in {PAGES_DIR}", file=sys.stderr)
        sys.exit(1)

    print(f"Found {len(html_files)} HTML files to parse")
    providers = []

    for filepath in html_files:
        print(f"  Parsing: {filepath.name}")
        try:
            data = parse_html_file(str(filepath))
            if data:
                providers.append(data)
                print(f"    -> {data.get('name', '?')}, age={data.get('age')}, "
                      f"city={data.get('location', {}).get('city', '?')}, "
                      f"services={len(data.get('services', []))}, "
                      f"photos={len(data.get('photos', []))}")
            else:
                print(f"    -> SKIPPED (parse returned None)")
        except Exception as e:
            print(f"    -> ERROR: {e}", file=sys.stderr)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(providers, f, indent=2, ensure_ascii=False)

    print(f"\nWrote {len(providers)} providers to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
