#!/usr/bin/env python3
"""v1.0.43: replace ineffective photo slideshow with a REAL LAYERED 2D rig.

Preserves profiles, Gemini/OpenAI keys, conversations and the existing signed
upgrade channel. Does not reintroduce 3D Ethan or native Filament. This
applies only after the already-tested 1.0.42 reconstruction overlay.
"""
from pathlib import Path
import hashlib

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "source/app"
PKG = APP / "src/main/java/com/kelimebulmacasi/a1"
ASSETS = APP / "src/main/assets/teacher2d"
TESTS = APP / "src/test/java/com/kelimebulmacasi/a1"
GRADLE = APP / "build.gradle.kts"

g = GRADLE.read_text("utf-8")
assert g.count("versionCode = 67") == 1
assert g.count('versionName = "1.0.42-realistic-2d-live-teacher-qa"') == 1
assert 'applicationIdSuffix = ".offline101"' in g
assert not (PKG / "EthanLive3DStage.kt").exists()
assert not (PKG / "EthanIsolatedVoiceTeacherActivity.kt").exists()
assert not (APP / "src/main/assets/avatars/ethan-mobile.glb").exists()

old = (PKG / "Teacher2DPortrait.kt").read_text("utf-8")
assert 'teacher2d/teacher2d_atlas.webp' in old
assert 'val image = atlas' in old
assert (ASSETS / "teacher2d_atlas.webp").is_file()
assert 'TeacherStage2D(' in (PKG / "TeacherStage2D.kt").read_text("utf-8")

for name in ("Teacher2DPortrait.kt", "Teacher2DAnimator.kt"):
    src = ROOT / "tools/v143" / name
    assert src.is_file(), name
    (PKG / name).write_text(src.read_text("utf-8"), encoding="utf-8")

TESTS.mkdir(parents=True, exist_ok=True)
(TESTS / "Teacher2DAnimatorTest.kt").write_text(
    (ROOT / "tools/v143/Teacher2DAnimatorTest.kt").read_text("utf-8"),
    encoding="utf-8",
)

for name in ("body", "head", "mouth", "eyes"):
    file = f"teacher2d_{name}.webp"
    source = ROOT / "tools/v143" / file
    assert source.is_file() and source.stat().st_size > 30_000, file
    data = source.read_bytes()
    assert data[:4] == b"RIFF" and data[8:12] == b"WEBP", file
    (ASSETS / file).write_bytes(data)

# The old 21 full-body photo frames are NOT used as a fake animation fallback.
(ASSETS / "teacher2d_atlas.webp").unlink()

g = g.replace("versionCode = 67", "versionCode = 68", 1)
g = g.replace('versionName = "1.0.42-realistic-2d-live-teacher-qa"',
              'versionName = "1.0.43-articulated-2d-teacher-qa"', 1)
GRADLE.write_text(g, encoding="utf-8")

print("v1.0.43 QA: 4 separately drawn 2D layers (head, body, lips, eyes)")
print("Real generated 7 mouth expressions and 3 eye states replace photo slides")
print("NO TTS onRangeStart? Mouth animates ONLY during real onPlaying")
print("Gemini/OpenAI, encrypted user preferences, US/UK voice, chat intact")
print("Requires actual Android visual comparison; not phoneme-perfect lip sync")
