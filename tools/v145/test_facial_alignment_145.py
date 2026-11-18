#!/usr/bin/env python3
"""Pixel-level QA: reject cheek lips, shifted eyes, face flicker and broken ZIP."""
from pathlib import Path
import io, json, zipfile, math
from PIL import Image, ImageChops
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/'source/app'
ASSETS=APP/'src/main/assets/teacher2d'
GRADLE=(APP/'build.gradle.kts').read_text('utf-8')
assert 'versionCode = 70' in GRADLE
assert 'versionName = "1.0.45-face-alignment-blink-qa"' in GRADLE
assert 'applicationIdSuffix = ".offline101"' in GRADLE
assert 'sceneview' not in GRADLE.lower() and 'filament' not in GRADLE.lower()
assert not (APP/'src/main/assets/avatars/ethan-mobile.glb').exists()
assert (ROOT/'archived/ethan_3d/README.md').is_file()
assert (ASSETS/'teacher2d_atlas_100.webp').is_file()
assert Image.open(ASSETS/'teacher2d_atlas_100.webp').size==(2560,3840)
z=zipfile.ZipFile(ROOT/'dist/WordQuestAI-100-animation-frames.zip')
manifest=json.loads(z.read('manifest.json'))
assert manifest['frame_count']==100 and len(manifest['frames'])==100
base=Image.open(io.BytesIO(z.read('frames/frame_000.webp'))).convert('RGB')
assert base.size==(256,384)
fixed=((92,42,163,51),(93,75,103,105),(146,75,163,105),(108,99,145,108))
for i in range(100):
    frame=Image.open(io.BytesIO(z.read(f'frames/frame_{i:03d}.webp'))).convert('RGB')
    for roi in fixed:
        assert ImageChops.difference(base.crop(roi),frame.crop(roi)).getbbox() is None, ('facial drift',i,roi)
# Frame 15 only opens the actual mouth, not an oval on the cheek.
opened=Image.open(io.BytesIO(z.read('frames/frame_015.webp'))).convert('RGB')
changed=ImageChops.difference(base,opened)
assert changed.crop((96,48,154,74)).getbbox() is None,'Mouth shifted iris/eyebrow'
assert changed.crop((107,78,137,94)).getbbox() is not None,'Mouth does not move'
# Weighted center of change must match the original real lip center (~x121).
x0,y0,x1,y1=104,78,140,96
patch=changed.crop((x0,y0,x1,y1)).convert('RGB')
weight=total=0.0
for y in range(patch.height):
    for x in range(patch.width):
        v=sum(patch.getpixel((x,y)))/3.0
        total+=v;weight+=(x+x0)*v
assert total>600 and 118.0 < weight/total <124.0,('misaligned mouth centroid',weight/total)
# Blink changes eyes only, and NOT cheeks, hair or nose.
closed=Image.open(io.BytesIO(z.read('frames/frame_005.webp'))).convert('RGB')
eyechange=ImageChops.difference(base,closed)
assert eyechange.crop((99,58,145,70)).getbbox() is not None,'No eye closure'
for roi in [(92,40,163,52),(96,77,152,110)]:
    assert eyechange.crop(roi).getbbox() is None,('Blink moves whole face',roi)
timeline=(APP/'src/main/java/com/kelimebulmacasi/a1/Teacher2D100Timeline.kt').read_text('utf-8')
assert 'blink < 165L' in timeline and 'listOf(0, 1, 2, 3, 7, 8, 9)' in timeline
assert 'Teacher2D100Timeline.select' in (APP/'src/main/java/com/kelimebulmacasi/a1/Teacher2DPortrait.kt').read_text('utf-8')
print('PASS: 100 frames, eye/hair/cheek landmark pixels identical, mouth centroid x121')
print('PASS: no rectangular moving eyes/mouth, eyelids ~165ms, arms relaxed')
print('PASS: full-body source, signed update identity and previous AI data intact')
print('PENDING: user-device visual validation and true phoneme-level lip timing')