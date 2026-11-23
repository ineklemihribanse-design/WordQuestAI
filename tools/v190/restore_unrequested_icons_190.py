#!/usr/bin/env python3
"""v190 icon scope correction, applied AFTER v189 icon installation.

The approved user-provided icon pack is reserved for explicit destinations:
Ana, Yeni Oyun, Bulmacalarım, Hazır Paketler/klasör, Fiiller.
Restore the previous YDS Akademi / YDS Hazırlık artwork and leave unrelated
studying, progress, assistant, arena and daily icons exactly as before.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"

def one(text, before, after, label):
    n = text.count(before)
    if n != 1:
        raise AssertionError(f"{label}: expected exactly one match, found {n}")
    return text.replace(before, after, 1)

def apply():
    pending = {}

    p = SRC / "Wq189UploadedIcons.kt"
    s = p.read_text(encoding="utf-8")
    begin = s.index("    private val routes = mapOf(")
    end = s.index("\n    fun asset(key: String): String? = routes[key]", begin)
    prior = s[begin:end]
    for key in ("home", "new_game", "solo_puzzle", "packages", "verbs",
                "exam", "review", "cards", "analytics", "assistant", "daily_plan"):
        assert f'"{key}" to ' in prior, "Unexpected uploaded icon catalog: " + key
    scoped = """    // Explicitly requested replacements only: preserve all other old icons.
    private val routes = mapOf(
        "home" to "home",
        "new_game" to "puzzle_abc_neon",
        "solo_puzzle" to "puzzle_builder",
        "packages" to "ready_folders",
        "folder" to "ready_folders",
        "categories" to "ready_folders",
        "verbs" to "verb_forms"
    )"""
    s = s[:begin] + scoped + s[end:]
    pending[p] = s

    p = SRC / "Wq175PremiumHome.kt"
    s = p.read_text(encoding="utf-8")
    begin = s.index("                val userApproved = when(tile.title) {")
    end = s.index("                val illustrated=when(tile.title) {", begin)
    prior = s[begin:end]
    for key in ('"YDS Akademi" -> "exam"', '"YDS Hazırlık" -> "review"',
                '"Hazır Paketler" -> "packages"', '"Bulmacalarım" -> "solo_puzzle"'):
        assert key in prior, "Expected old v189 route: " + key
    scoped = """                // Preserve established icons for every non-requested tile.
                val userApproved = when(tile.title) {
                    "Hazır Paketler" -> "packages"
                    "Bulmacalarım" -> "solo_puzzle"
                    "Yeni Oyun" -> "new_game"
                    "Fiiller" -> "verbs"
                    else -> null
                }
"""
    s = s[:begin] + scoped + s[end:]
    assert "Wq189UploadedArtwork(userApproved,Modifier.size(imageSize))" in s
    pending[p] = s

    p = SRC / "WordQuest175MyPuzzles.kt"
    s = p.read_text(encoding="utf-8")
    s = one(s,
        'Wq189UploadedArtwork("learning",\n                    Modifier.align(Alignment.BottomEnd).size(47.dp))',
        'Wq175GlossIcon("notebook",\n                    Modifier.align(Alignment.BottomEnd).size(47.dp))',
        "restore the unrequested secondary learning icon")
    assert 'Wq189UploadedArtwork("solo_puzzle"' in s
    assert 'Wq189UploadedArtwork("new_game"' in s
    pending[p] = s

    # Explicit QA: original YDS assets are still available and the global
    # semantic "exam"/"yds" route does not override them.
    catalog = pending[SRC / "Wq189UploadedIcons.kt"]
    routes = catalog[catalog.index("private val routes"):catalog.index("fun asset(")]
    for key in ('"yds"', '"exam"', '"review"', '"grammar"', '"daily_plan"',
                '"plan"', '"cards"', '"analytics"', '"puzzle"', '"assistant"'):
        assert key not in routes, "Unrequested replacement leaked: " + key
    home = pending[SRC / "Wq175PremiumHome.kt"]
    selection = home[home.index("val userApproved = when"):home.index("val illustrated=when")]
    assert "YDS Akademi" not in selection and "YDS Hazırlık" not in selection
    art = (SRC / "WordQuest175ThemeArt.kt").read_text("utf-8")
    assert '"yds_akademi" -> "exam"' in art, "Original academy icon route missing"
    nav = (SRC / "Wq175HomeNavigation.kt").read_text("utf-8")
    assert 'Wq189UploadedArtwork("home"' in nav
    assert 'Wq189UploadedArtwork("new_game"' in nav
    assert 'Wq175GlossIcon("plan"' in nav, "Original daily navigation must remain"
    for p, text in pending.items():
        p.write_text(text, encoding="utf-8")
    print("PASS v190: YDS Akademi and YDS Hazırlık use their former artwork")
    print("PASS v190: only Ana, Yeni Oyun, Bulmacalarım, Hazır Paketler and Fiiller use supplied icons")
    print("PASS v190: other old card, daily, assistant, review and progress icons preserved")

if __name__ == "__main__":
    apply()
