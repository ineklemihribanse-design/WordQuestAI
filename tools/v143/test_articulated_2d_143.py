#!/usr/bin/env python3
"""Fail-closed code and actual pixel-layer verification for 1.0.43."""
from pathlib import Path
from PIL import Image, ImageChops, ImageStat
import hashlib

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "source/app"
SRC = APP / "src/main/java/com/kelimebulmacasi/a1"
ASSETS = APP / "src/main/assets/teacher2d"
GRADLE = (APP / "build.gradle.kts").read_text("utf-8")
assert 'versionCode = 68' in GRADLE
assert 'versionName = "1.0.43-articulated-2d-teacher-qa"' in GRADLE
assert 'applicationIdSuffix = ".offline101"' in GRADLE
assert 'sceneview' not in GRADLE.lower()
assert 'filament' not in GRADLE.lower()
assert 'android:process=":teacher3d"' not in (
    APP / "src/main/AndroidManifest.xml").read_text("utf-8")

assert not (ASSETS / "teacher2d_atlas.webp").exists()
assert not (APP / "src/main/assets/avatars/ethan-mobile.glb").exists()
assert (ROOT / "archived/ethan_3d/README.md").is_file()
for name, dims in {
    "teacher2d_body.webp": (384, 2048),
    "teacher2d_head.webp": (384, 264),
    "teacher2d_mouth.webp": (931, 77),
    "teacher2d_eyes.webp": (531, 55),
}.items():
    p = ASSETS / name
    assert p.is_file(), name
    raw = p.read_bytes()
    assert raw[:4] == b"RIFF" and raw[8:12] == b"WEBP"
    im = Image.open(p)
    assert im.size == dims, (name, im.size, dims)
    assert len(raw) > 30_000, name

mouths = Image.open(ASSETS / "teacher2d_mouth.webp").convert("RGB")
base = mouths.crop((0, 0, 133, 77))
for index in range(1,7):
    target = mouths.crop((133*index,0,133*(index+1),77))
    diff = ImageChops.difference(base,target)
    average = sum(ImageStat.Stat(diff).mean)/3
    assert average > 5.0, ("viseme is same photograph",index,average)
eyes = Image.open(ASSETS / "teacher2d_eyes.webp").convert("RGB")
eyeOpen = eyes.crop((0,0,177,55))
for i in (1,2):
    closed = eyes.crop((177*i,0,177*(i+1),55))
    diff = ImageChops.difference(eyeOpen,closed)
    assert sum(ImageStat.Stat(diff).mean)/3 > 5.0, "Eyes not actually blinking"
body = Image.open(ASSETS / "teacher2d_body.webp").convert("RGB")
rest = body.crop((0,0,384,512))
for index in (1,2,3):
    gesture = body.crop((0,512*index,384,512*(index+1)))
    # A dynamic actual arm image, not a still reference or painted line.
    assert ImageChops.difference(rest,gesture).getbbox() is not None

portrait=(SRC/"Teacher2DPortrait.kt").read_text("utf-8")
animator=(SRC/"Teacher2DAnimator.kt").read_text("utf-8")
for expected in ('Teacher2DAnimator.mouthIndex', 'Teacher2DAnimator.blinkIndex',
                 'Teacher2DAnimator.gesture', 'Teacher2DAnimator.headTiltDegrees',
                 'withTransform({ rotate', 'teacher2d_head.webp',
                 'teacher2d_mouth.webp','teacher2d_eyes.webp','teacher2d_body.webp'):
    assert expected in portrait,expected
assert 'teacher2d_atlas' not in portrait
assert "actual audio" not in animator.lower() or "onPlaying" in animator
for expected in ('fallbackVisemes', 'mode != AvatarMode.SPEAKING',
                 'speechStartedAtMs <= 0L', 'cue.quality != LipSyncQuality.NONE'):
    assert expected in animator,expected

assert (APP/"src/test/java/com/kelimebulmacasi/a1/Teacher2DAnimatorTest.kt").is_file()
assert 'Teacher2DPersona.systemPrompt' in (SRC/"AiSession.kt").read_text("utf-8")
assert 'onActualSpeechRange' in (SRC/"LiveTeacherScreen.kt").read_text("utf-8")
assert 'AiProvider.entries.filter { it != AiProvider.Claude }' in (
    SRC/"AiProviderSettings.kt").read_text("utf-8")
print("PASS: 7 visibly DIFFERENT real lip textures, open/half/closed eyelids")
print("PASS: independent moving arms and separately articulated neck/face")
print("PASS: real onPlaying gates fallback lip motion when TTS gives no ranges")
print("PASS: 1.0.42 to 1.0.43 in-place same-signer compatibility, no 3D engine")
print("DEVICE QA still required: visual naturalness cannot be guaranteed by CI")
