#!/usr/bin/env python3
"""v190 targeted correction: preserve the original YDS Academy/Preparation icons.

The user requested the uploaded artwork only on selected destinations, NOT
the existing YDS illustrations. Apply after the v189 icon installer and
before compiling the finished Android app. Changes only those two routes.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"


def exactly_once(source: str, old: str, new: str, label: str) -> str:
    found = source.count(old)
    if found != 1:
        raise AssertionError(f"{label}: expected one match, found {found}")
    return source.replace(old, new, 1)


def apply() -> None:
    premium = SRC / "Wq175PremiumHome.kt"
    icons = SRC / "Wq189UploadedIcons.kt"
    theme = SRC / "WordQuest175ThemeArt.kt"

    home = premium.read_text(encoding="utf-8")
    catalog = icons.read_text(encoding="utf-8")
    original_theme = theme.read_text(encoding="utf-8")

    # Remove the two unrequested title-level replacements introduced by v189.
    # This sends YDS Akademi and YDS Hazırlık back to their previously approved
    # pre-v189 art routes, not to an invented substitute.
    home = exactly_once(home,
        '                    "YDS Akademi" -> "exam"\n',
        '',
        'restore old YDS Akademi illustration')
    home = exactly_once(home,
        '                    "YDS Hazırlık" -> "review"\n',
        '',
        'restore old YDS Hazırlık illustration')

    # The second override was in the general-purpose glossary mapper.
    # Removing these intercepts prevents any other pre-approved exam / review
    # icons from being swapped for unrelated 3D imagery.
    catalog = exactly_once(catalog,
        '        "review" to "reading_review_classic",\n',
        '',
        'never change old review illustration globally')
    catalog = exactly_once(catalog,
        '        "exam" to "reading_review_neon",\n',
        '',
        'never change old exam illustration globally')

    # Keep legacy YDS icon and its route. Do not change its resources.
    for marker in (
        '"yds_akademi" -> "exam"',
        '"yds_akademi"->R.drawable.wq175_yds_akademi_icon',
    ):
        assert marker in original_theme, marker
    assert '"YDS Akademi" -> "exam"' not in home
    assert '"YDS Hazırlık" -> "review"' not in home
    for requested in (
        '"Hazır Paketler" -> "packages"',
        '"Bulmacalarım" -> "solo_puzzle"',
        '"Fiiller" -> "verbs"',
    ):
        assert requested in home, requested
    for route in (
        '"home" to "home"',
        '"new_game" to "puzzle_abc_neon"',
        '"solo_puzzle" to "puzzle_builder"',
        '"packages" to "ready_folders"',
        '"verbs" to "verb_forms"',
    ):
        assert route in catalog, route

    premium.write_text(home, encoding="utf-8")
    icons.write_text(catalog, encoding="utf-8")
    print("PASS: YDS Akademi and YDS Hazırlık use previously approved icons")
    print("PASS: user-requested home, new-game, solo, folder and verb icons unchanged")
    print("PASS: no other card, mascot, layout, action or Firebase setting altered")


if __name__ == "__main__":
    apply()
