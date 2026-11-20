#!/usr/bin/env python3
"""Bundle attributed English-Turkish CEFR content as a non-destructive v175 addendum."""
from pathlib import Path
import json
import shutil
import re

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tools/v175/content"
ASSETS = ROOT / "source/app/src/main/assets"
CATALOG = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1/Catalog.kt"
LEVELS = ("A1", "A2", "B1", "B2", "C1")
LETTERS = re.compile(r"^[A-Z]+(?:[ '-][A-Z]+){0,5}$")

def main():
    assert (ASSETS / "vocabulary-extra.tsv").is_file(), "Existing vocabulary must not be overwritten"
    rows = []
    by_level = {}
    seen = set()
    for level in LEVELS:
        path = SOURCE / ("wordquest_en_tr_" + level + ".tsv")
        current = 0
        for number, line in enumerate(path.read_text("utf-8").splitlines(), 1):
            cols = line.split("\t")
            assert len(cols) == 5, (str(path), number, "five columns required")
            tier, topic, kind, word, meaning = cols
            assert tier == level, (path, number, tier)
            assert topic == "Genel İngilizce", (path, number)
            assert kind.strip(), (path, number)
            assert word == word.strip().upper(), (path, number)
            assert LETTERS.fullmatch(word), (path, number, word)
            assert 2 <= len(re.sub("[^A-Z]", "", word)) <= 22, (path, number)
            assert 2 <= len(meaning) <= 125 and any(c.isalpha() for c in meaning), (path, number)
            key = level, re.sub("[^A-Z]", "", word)
            assert key not in seen, ("duplicate", key)
            seen.add(key)
            rows.append(line)
            current += 1
        assert current >= 400, ("Unexpectedly small source content", level, current)
        by_level[level] = current
    assert len(rows) > 6000, "Word library was not populated"
    path = ASSETS / "vocabulary-addendum-v175.tsv"
    assert not path.exists(), "Do not apply the overlay twice"
    path.write_text("\n".join(rows) + "\n", encoding="utf-8")
    license_dir = ASSETS / "licenses"
    license_dir.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SOURCE / "ATTRIBUTION.md",
                    license_dir / "WORDQUEST_CEFR_175_ATTRIBUTION.md")
    catalog = CATALOG.read_text("utf-8")
    old = '''    internal val vocabulary by lazy {
        VocabularyBank(games, context.assets.open("vocabulary-extra.tsv").bufferedReader().use { it.readLines() })
    }'''
    new = '''    internal val vocabulary by lazy {
        val legacy = context.assets.open("vocabulary-extra.tsv")
            .bufferedReader(Charsets.UTF_8).use { it.readLines() }
        val additional = context.assets.open("vocabulary-addendum-v175.tsv")
            .bufferedReader(Charsets.UTF_8).use { it.readLines() }
        VocabularyBank(games, legacy + additional)
    }'''
    assert catalog.count(old) == 1, "Catalog must match tested v1.0.74 snapshot"
    CATALOG.write_text(catalog.replace(old, new, 1), "utf-8")
    report = {
        "source": "EnglishPractice rev 4277f12c2845dc065b28f9b330b0b736538e2ad7",
        "license": "CC BY-SA 4.0 derived wordlists",
        "added_count": len(rows),
        "added_by_level": by_level,
        "cefr_assurance": "source-reviewed; requires WordQuest sampling; not official CEFR certification",
        "C2_source": "not available",
        "target_per_level": 2000,
    }
    (ASSETS / "vocabulary-addendum-v175-report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print("Content bundled:", by_level, "total:", len(rows),
          "legacy untouched, source provenance included.")

if __name__ == "__main__":
    main()
