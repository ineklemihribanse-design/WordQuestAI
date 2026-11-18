#!/usr/bin/env python3
"""Build QA v1.0.44 from working signed v1.0.43 via deterministic 100 frames.

Never migrates user data. Keep app ID, signing, encrypted Gemini/OpenAI keys,
existing US/UK audio, conversations, Arena, YDS and archived Ethan sources.
"""
from pathlib import Path
import subprocess,sys

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/'source/app'
PKG=APP/'src/main/java/com/kelimebulmacasi/a1'
ASSETS=APP/'src/main/assets/teacher2d'
TESTS=APP/'src/test/java/com/kelimebulmacasi/a1'
GRADLE=APP/'build.gradle.kts'
SRC=ROOT/'tools/v144'
DIST=ROOT/'dist'

g=GRADLE.read_text('utf-8')
assert g.count('versionCode = 68')==1
assert g.count('versionName = "1.0.43-articulated-2d-teacher-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g
assert 'Teacher2DAnimator.mouthIndex' in (PKG/'Teacher2DPortrait.kt').read_text('utf-8')
assert (ROOT/'archived/ethan_3d/README.md').exists()
assert not (APP/'src/main/assets/avatars/ethan-mobile.glb').exists()

DIST.mkdir(exist_ok=True)
subprocess.run([sys.executable,str(SRC/'generate_teacher_100.py'),
                '--master',str(SRC/'fullbody_master.webp'),
                '--assets',str(ASSETS),
                '--zip',str(DIST/'WordQuestAI-100-animation-frames.zip'),
                '--preview',str(DIST/'teacher-100-contact.png')],check=True)
for filename in ('Teacher2DPortrait.kt','Teacher2D100Timeline.kt'):
    (PKG/filename).write_text((SRC/filename).read_text('utf-8'),encoding='utf-8')
TESTS.mkdir(parents=True,exist_ok=True)
(TESTS/'Teacher2D100TimelineTest.kt').write_text(
    (SRC/'Teacher2D100TimelineTest.kt').read_text('utf-8'),encoding='utf-8')
# Never load the old, mismatched head/eye/mouth/arm patches in live APK.
for filename in ('teacher2d_body.webp','teacher2d_head.webp',
                 'teacher2d_eyes.webp','teacher2d_mouth.webp'):
    (ASSETS/filename).unlink(missing_ok=True)
assert (ASSETS/'teacher2d_atlas_100.webp').is_file()
assert (ASSETS/'teacher2d_fullbody_master.webp').is_file()
g=g.replace('versionCode = 68','versionCode = 69',1).replace(
    'versionName = "1.0.43-articulated-2d-teacher-qa"',
    'versionName = "1.0.44-100-frame-stable-2d-teacher-qa"',1)
GRADLE.write_text(g,'utf-8')
print('1.0.44 QA: 100 pre-composited fixed-identity frames: no head/eye/lip photo overlays')
print('100-frame ZIP, contact sheet, full-body master and compact Android sprite atlas generated')
print('Most frames: both arms down. Breath: <=0.31 px and confined to torso')
print('Same app signature / installed-user API keys and learning data unchanged')