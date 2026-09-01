#!/usr/bin/env python3
"""
Build a compact offline medicine catalog (JSON asset) from the Indian-Medicine-Dataset CSV.
Output: app/src/main/assets/medicines.json  — loaded at app startup for real-medicine matching.

Compact keys keep the file small: i=id, b=brand, g=generic, n=ingredientIds, s=strength,
u=unit, f=form, m=manufacturer, p=price.
"""
import csv, json, re, sys

STRENGTH = re.compile(r"([\d.]+)\s*(mg|mcg|ml|g|iu)", re.I)

def cid(s): return re.sub(r"[^A-Z0-9]+", "_", s.upper()).strip("_")

def parse_comp(comp):
    if not comp: return None
    base = re.split(r"[(]", comp)[0].strip()
    if not base or len(base) < 2: return None
    ing = cid(base)
    m = STRENGTH.search(comp)
    return ing, (float(m.group(1)) if m else None), (m.group(2).upper() if m else None)

def main():
    src = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else "../app/src/main/assets/medicines.json"
    cap = int(sys.argv[3]) if len(sys.argv) > 3 else 60000
    seen, meds = set(), []
    with open(src, newline="", encoding="utf-8", errors="ignore") as f:
        for row in csv.DictReader(f):
            if len(meds) >= cap: break
            name = (row.get("name") or "").strip()
            if not name: continue
            if (row.get("Is_discontinued") or "").upper() == "TRUE": continue
            t = (row.get("type") or "").lower()
            if t not in ("", "allopathy"): continue
            comps = [parse_comp(row.get("short_composition1","")),
                     parse_comp(row.get("short_composition2",""))]
            comps = [c for c in comps if c]
            if not comps: continue
            mid = cid(name)[:48]
            if not mid or mid in seen: continue
            seen.add(mid)
            ings = [c[0] for c in comps]
            strength = next((c[1] for c in comps if c[1] is not None), None)
            unit = next((c[2] for c in comps if c[2] is not None), None)
            price = None
            pr = (row.get("price(₹)") or row.get("price") or "").replace("₹","").strip()
            try: price = float(pr)
            except: pass
            meds.append({
                "i": mid, "b": name.split()[0], "g": " + ".join(ings),
                "n": ings, "s": strength, "u": unit,
                "f": "CAPSULE" if "cap" in (row.get("pack_size_label","") or "").lower() else "TABLET",
                "m": (row.get("manufacturer_name") or "").strip(),
                "p": price,
            })
    with open(out, "w", encoding="utf-8") as w:
        json.dump(meds, w, ensure_ascii=False, separators=(",", ":"))
    print(f"wrote {len(meds)} medicines -> {out}")

if __name__ == "__main__":
    main()
