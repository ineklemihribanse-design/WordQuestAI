#!/usr/bin/env python3
"""Inventory actual approved v1.0.82 icon routes; do not rewrite existing art.

Passes only referential static coverage, NOT subjective image/visual quality.
"""
from pathlib import Path
import re
import sys

ROOT=Path(__file__).resolve().parents[2]
FILE=ROOT/"tools/v182/Wq182Icons.kt"
ATLAS=ROOT/"tools/v182/wq182_approved_33_icons_atlas.webp"
PREMIUM=ROOT/"tools/v175/WordQuest175Icons.kt"
REPORT=ROOT/"tools/v183/ICON_INVENTORY_183.md"

def audit():
    text=FILE.read_text("utf-8")
    names_block=text.split("private val names = listOf(",1)[1].split(")",1)[0]
    names=re.findall(r'"(wq_[a-z0-9_]+)"',names_block)
    replace_block=text.split("private val replacements = mapOf(",1)[1].split(")",1)[0]
    pairs=re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"',replace_block)
    assert len(names)==33 and len(set(names))==33, "33 premium icons are not unique"
    assert len({p[0] for p in pairs})==len(pairs), "Duplicate route keys"
    assert all(x in names for _,x in pairs), "Mapped icon missing from atlas"
    assert set(["arena_duel","arena_team","arena_quiz"]) <= {a for a,_ in pairs}
    blob=ATLAS.read_bytes()
    assert len(blob)>150000 and blob[:4]==b"RIFF" and blob[8:12]==b"WEBP", "Atlas invalid"
    assert PREMIUM.is_file(), "Previous premium icon route lost"
    protected=("trophy","puzzle","cards","yds","grammar","listen","plan","synonym")
    assert all(x not in dict(pairs) for x in protected), "Existing approved icons overwritten"
    rows=["# WordQuest AI 1.0.83 — İkon envanteri","",
        f"- v1.0.82 paketindeki benzersiz çizim: **{len(names)}**",
        f"- Açık yönlendirme: **{len(pairs)}**",
        "- Korunan eski premium ikon anahtarları: "+", ".join(protected),
        "- Bu denetim, çizimin kalitesini veya cihazda kırpılmadığını doğrulamaz.",
        "","## Onaylı 33 çizim",""]
    rows.extend(f"- `{n}`" for n in names)
    rows += ["","## Yerine kullanılan eşlemeler","",
             "| Ekran anahtarı | Kullanılan görsel |",
             "|---|---|"]
    rows.extend(f"| `{a}` | `{b}` |" for a,b in pairs)
    REPORT.write_text("\n".join(rows)+"\n","utf-8")
    print(f"PASS: {len(names)} unique approved icons; {len(pairs)} valid replacements; older art keys preserved")

if __name__=="__main__":
    audit()
