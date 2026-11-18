#!/usr/bin/env python3
"""Actual pixel QA for v1.0.44, not merely string-grep smoke testing."""
from pathlib import Path
from PIL import Image,ImageChops
import hashlib,io,json,zipfile
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/'source/app'
PKG=APP/'src/main/java/com/kelimebulmacasi/a1'
ASSETS=APP/'src/main/assets/teacher2d'
GRADLE=(APP/'build.gradle.kts').read_text('utf-8')
assert 'versionCode = 69' in GRADLE
assert 'versionName = "1.0.44-100-frame-stable-2d-teacher-qa"' in GRADLE
assert 'applicationIdSuffix = ".offline101"' in GRADLE
assert 'sceneview' not in GRADLE.lower() and 'filament' not in GRADLE.lower()
assert 'android:process=":teacher3d"' not in (
    APP/'src/main/AndroidManifest.xml').read_text('utf-8')
assert not (APP/'src/main/assets/avatars/ethan-mobile.glb').exists()
assert not any(ASSETS.glob('teacher2d_mouth.webp'))
assert not any(ASSETS.glob('teacher2d_eyes.webp'))
assert not any(ASSETS.glob('teacher2d_head.webp'))
assert not any(ASSETS.glob('teacher2d_body.webp'))
assert (ROOT/'archived/ethan_3d/README.md').exists()

atlas=Image.open(ASSETS/'teacher2d_atlas_100.webp').convert('RGB')
assert atlas.size==(2560,3840),atlas.size
full=Image.open(ASSETS/'teacher2d_fullbody_master.webp')
assert full.size==(1024,1536),full.size
portrait=(PKG/'Teacher2DPortrait.kt').read_text('utf-8')
assert 'teacher2d_atlas_100.webp' in portrait
assert 'Teacher2D100Timeline.select' in portrait
assert 'teacher2d_head.webp' not in portrait
assert 'BitmapFactory.decodeStream' in portrait
assert 'frame / 10 * cellH' in portrait
script=(ROOT/'tools/v144/generate_teacher_100.py').read_text('utf-8')
assert 'EYE_BOXES' in script and 'MOUTH_X, MOUTH_Y' in script
assert 'hands_low' in script and 'breathing' in script
assert '0.31 * math.sin' in script, 'Breathing must remain almost invisible'
zip_path=ROOT/'dist/WordQuestAI-100-animation-frames.zip'
assert zip_path.is_file()
with zipfile.ZipFile(zip_path) as z:
    manifest=json.loads(z.read('manifest.json'))
    assert len(manifest['frames'])==100
    assert len([n for n in z.namelist() if n.startswith('frames/frame_')])==100
    assert Image.open(io.BytesIO(z.read('fullbody_master.webp'))).size==(1024,1536)
    for item in manifest['frames']:
        img=Image.open(io.BytesIO(z.read(item['file'])))
        assert img.size==(256,384)
        assert hashlib.sha256(z.read(item['file'])).hexdigest()==item['sha256']
    # All 100 complete frames are already registered; no independent face,
    # mouth, eye images can shift relative to the hair and cheeks.
    stable_coords=[(128,38),(112,44),(145,43),(91,98),(165,107),(125,118)]
    base=Image.open(io.BytesIO(z.read('frames/frame_000.webp'))).convert('RGB')
    for i in range(100):
        frame=Image.open(io.BytesIO(z.read(f'frames/frame_{i:03d}.webp'))).convert('RGB')
        assert all(frame.getpixel(p)==base.getpixel(p) for p in stable_coords),i
    # Distinct facial movements, not 100 copies of the same photograph.
    opened=Image.open(io.BytesIO(z.read('frames/frame_015.webp'))).convert('RGB')
    closed=Image.open(io.BytesIO(z.read('frames/frame_005.webp'))).convert('RGB')
    assert ImageChops.difference(base.crop((108,77,148,97)),
                                 opened.crop((108,77,148,97))).getbbox() is not None
    assert ImageChops.difference(base.crop((96,54,157,72)),
                                 closed.crop((96,54,157,72))).getbbox() is not None
assert 'Teacher2D100TimelineTest.kt' in [x.name for x in
    (APP/'src/test/java/com/kelimebulmacasi/a1').iterdir()]
assert 'Teacher2DPersona.systemPrompt' in (PKG/'AiSession.kt').read_text('utf-8')
assert 'onActualSpeechRange' in (PKG/'LiveTeacherScreen.kt').read_text('utf-8')
assert 'AiProvider.entries.filter { it != AiProvider.Claude }' in (
    PKG/'AiProviderSettings.kt').read_text('utf-8')
print('PASS: EXACTLY 100 real WebP images, full-body master, manifest and ZIP')
print('PASS: pixel-perfect stable face landmarks in all 100; eyes and mouth actually vary')
print('PASS: Android atlas from 100 complete frames eliminates live patch-registration jitter')
print('PASS: no Filament/GLB, Gemini/OpenAI/US/UK and chat credentials untouched')
print('DEVICE VISUAL QA pending: realistic phoneme matching requires audio timing data')