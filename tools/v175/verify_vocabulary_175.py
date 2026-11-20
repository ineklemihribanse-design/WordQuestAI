#!/usr/bin/env python3
"""v1.0.75 corpus validation: true tier coverage, integrity, and safe-app loading."""
from pathlib import Path
from collections import defaultdict
import json
import re

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "source/app/src/main/assets"
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
LEVELS = ("A1", "A2", "B1", "B2", "C1", "C2")
ID = lambda s: re.sub(r"[^A-Z]", "", s.strip().upper())

def collect_existing():
    by_level = defaultdict(set)
    for filename in ("puzzles.json", "expanded-puzzles.json"):
        for game in json.loads((ASSETS / filename).read_text("utf-8")):
            if game["level"] in LEVELS and game["mode"] in ("topic", "kind"):
                for entry in game["words"]:
                    by_level[game["level"]].add(ID(entry[0]))
    for line in (ASSETS / "vocabulary-extra.tsv").read_text("utf-8").splitlines():
        if not line or line.startswith("#"): continue
        fields = line.split("\t")
        assert len(fields) == 5, "Legacy schema changed unexpectedly"
        if fields[0] in LEVELS:
            by_level[fields[0]].add(ID(fields[3]))
    return by_level

def main():
    legacy = collect_existing()
    added = defaultdict(set)
    invalid = []
    for n, row in enumerate((ASSETS / "vocabulary-addendum-v175.tsv").read_text("utf-8").splitlines(),1):
        fields = row.split("\t")
        if len(fields) != 5:
            invalid.append((n, "fields"))
            continue
        level, topic, kind, answer, meaning = fields
        word = ID(answer)
        if level not in LEVELS[:5] or topic != "Genel İngilizce" or \
            not kind or len(word) not in range(2,23) or not meaning.strip():
            invalid.append((n, "metadata"))
        if word in added[level] or word in legacy[level]:
            invalid.append((n, "duplicate"))
        added[level].add(word)
    assert not invalid, "Unsafe or duplicate vocabulary rows: " + str(invalid[:12])
    assert sum(map(len, added.values())) >= 6500
    catalog = (SRC / "Catalog.kt").read_text("utf-8")
    assert 'context.assets.open("vocabulary-extra.tsv")' in catalog
    assert 'context.assets.open("vocabulary-addendum-v175.tsv")' in catalog
    assert 'VocabularyBank(games, legacy + additional)' in catalog
    assert 'val byId = games.associateBy(Game::id)' in catalog
    attribution = ASSETS / "licenses/WORDQUEST_CEFR_175_ATTRIBUTION.md"
    assert attribution.is_file()
    credit = attribution.read_text("utf-8")
    for fragment in ("EnglishPractice", "CEFR-J", "Octanove", "CC BY-SA 4.0",
                     "4277f12c2845dc065b28f9b330b0b736538e2ad7"):
        assert fragment in credit, fragment
    report = json.loads((ASSETS/"vocabulary-addendum-v175-report.json").read_text("utf-8"))
    assert report["added_count"] == sum(map(len, added.values()))
    assert report["added_by_level"] == {tier:len(added[tier]) for tier in LEVELS[:5]}
    combined = {tier: len(legacy[tier] | added[tier]) for tier in LEVELS}
    shortfall = {tier: max(0, 2000 - count) for tier,count in combined.items()}
    print("Vocabulary levels (existing + added; pre-human-sampling):")
    for level in LEVELS:
        print(f"  {level}: existing={len(legacy[level])}, added={len(added[level])}, "
              f"total={combined[level]}, remaining={shortfall[level]}")
    assert all(combined[tier] >= 2000 for tier in ("B1", "B2"))
    assert combined["C2"] < 2000, "C2 is not imported: do not claim complete content"
    print("PASS: unique, attributed and existing-word-safe source content; no falsely claimed full 12K.")

if __name__ == "__main__":
    main()
