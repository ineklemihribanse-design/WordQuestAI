#!/usr/bin/env python3
"""Preserve v1.0.54 approved Word Memory UI alongside Arena 1.0.55.
Fail closed if the current source no longer matches the inspected v148 baseline.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
source = root / "source"
pkg = source / "app/src/main/java/com/kelimebulmacasi/a1"
home = pkg / "VocabularyNeonHome.kt"
practice = pkg / "VocabularyNeonPractice.kt"
gradle = source / "app/build.gradle.kts"

def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one anchor, got {count}: {old[:70]}")
    return text.replace(old, new, 1)

def main():
    for path in (home, practice, gradle):
        if not path.is_file():
            raise RuntimeError(f"Missing assembled Android source: {path}")
    h = home.read_text(encoding="utf-8")
    assert "internal fun VocabularyNeonHome(" in h
    assert "ProfileRobotPortrait(robot" in h
    assert "NeonVocabularyPractice(session" in h
    h = replace_once(h, 'internal val MemoryNavy = Color(0xFF173445)',
        'internal val MemoryNavy = Color(0xFF173D65)', "navy")
    h = replace_once(h, 'internal val MemoryBlue = Color(0xFF31576A)',
        'internal val MemoryBlue = Color(0xFF2C83CB)', "blue")
    h = replace_once(h, 'internal val MemoryCyan = Color(0xFF83E4D9)',
        'internal val MemoryCyan = Color(0xFF65BDF2)', "cyan")
    h = replace_once(h, 'internal val MemoryLilac = Color(0xFFA88DEB)',
        'internal val MemoryLilac = Color(0xFFB9A7EF)', "lilac")
    h = replace_once(h, 'internal val MemoryGold = Color(0xFFFFDDA5)',
        'internal val MemoryGold = Color(0xFFFFD78B)', "gold")
    h = replace_once(h, 'Text("Bugünkü Tekrarın",', 'Text("Bugünkü Tekrar",', "daily review")
    h = replace_once(h, 'Text("Kelime\\nHafızam",', 'Text("Kelime\\nHafızam",', "heading") if False else h
    h = replace_once(h, 'Color.White.copy(alpha = .15f))\n                            .border(1.dp, MemoryCyan.copy(alpha = .24f)',
        'Color.White.copy(alpha = .94f))\n                            .border(1.dp, Color(0xFFD9E7F5)', "collection card background")
    home.write_text(h, encoding="utf-8")
    g = gradle.read_text(encoding="utf-8")
    assert 'versionCode = 73' in g
    assert 'versionName = "1.0.48-neon-word-memory"' in g
    assert 'applicationIdSuffix = ".fresh"' in g
    # This compat patch preserves current 1.0.54 Memory UI without modifying
    # Arena's independent 1.0.55 Android version. No records are deleted.
    print("Word Memory 1.0.54 styling retained alongside Arena avatar fixes.")
    print("Original .fresh package identifier and signing remain unchanged.")

if __name__ == "__main__":
    main()
