#!/usr/bin/env python3
"""Preserve the latest 1.0.54 Word Memory light styling alongside Arena 1.0.55.

The assembled legacy v148 neon sources can begin with either dark palette
(061830 / 123B6B) or the later intermediate palette (173445 / 31576A).
Accept both, normalize once to the already-approved 1.0.54 light palette,
and do NOT touch app identity, versionCode, records or practice logic.
"""
from pathlib import Path
import re

root=Path(__file__).resolve().parents[2]
source=root/"source"
pkg=source/"app/src/main/java/com/kelimebulmacasi/a1"
home=pkg/"VocabularyNeonHome.kt"
practice=pkg/"VocabularyNeonPractice.kt"
gradle=source/"app/build.gradle.kts"

def main():
    for path in (home,practice,gradle):
        if not path.is_file(): raise RuntimeError(f"Missing source: {path}")
    s=home.read_text(encoding="utf-8")
    assert 'internal fun VocabularyNeonHome(' in s
    assert 'ProfileRobotPortrait(robot' in s
    assert 'NeonVocabularyPractice(session' in s
    palette={
        "MemoryNavy":"173D65",
        "MemoryBlue":"2C83CB",
        "MemoryCyan":"65BDF2",
        "MemoryLilac":"B9A7EF",
        "MemoryGold":"FFD78B",
    }
    for name,hex_value in palette.items():
        regex=rf'(internal val {name} = Color\\(0xFF)[0-9A-Fa-f]{{6}}(\\))'
        new,n=re.subn(regex,lambda m:m.group(1)+hex_value+m.group(2),s)
        if n!=1: raise RuntimeError(f"Expected one color declaration for {name}, got {n}")
        s=new
    s=s.replace('Text("Bugünkü Tekrarın",','Text("Bugünkü Tekrar",')
    # Never blanket-replace all translucent backgrounds: only the actual
    # collection tile that was approved as a near-white card.
    for previous in (
        'Color.White.copy(alpha = .15f))\\n                            .border(1.dp, MemoryCyan.copy(alpha = .24f)',
        'Color.White.copy(alpha = .085f))\\n                            .border(1.dp, MemoryCyan.copy(alpha = .24f)',
    ):
        if previous in s:
            s=s.replace(previous,
                'Color.White.copy(alpha = .94f))\\n                            .border(1.dp, Color(0xFFD9E7F5)',1)
            break
    if 'internal val MemoryNavy = Color(0xFF173D65)' not in s:
        raise RuntimeError("1.0.54 light Word Memory palette not retained")
    g=gradle.read_text(encoding="utf-8")
    assert 'versionCode = 73' in g
    assert 'applicationIdSuffix = ".fresh"' in g
    home.write_text(s,encoding="utf-8")
    print("Preserved 1.0.54 light Word Memory styling and existing profile data.")
    print("No versionCode, signature, package name or word record changes.")

if __name__=="__main__":
    main()
