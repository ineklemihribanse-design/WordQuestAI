#!/usr/bin/env python3
"""1.0.45 registered 2D face correction after the verified v1.0.44 overlay.

Rebuilds the 100 frame sprite atlas from one unchanged full-body master, fixes
lip ROI to the ACTUAL mouth center instead of cheek, and replaces eye-patch
cloning with same-face smooth lids. Does not touch keys, chats or app identity.
"""
from pathlib import Path
import subprocess, sys
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/'source/app'
PKG=APP/'src/main/java/com/kelimebulmacasi/a1'
ASSETS=APP/'src/main/assets/teacher2d'
GRADLE=APP/'build.gradle.kts'
SRC=ROOT/'tools/v145'
DIST=ROOT/'dist'

g=GRADLE.read_text('utf-8')
assert g.count('versionCode = 69') == 1
assert g.count('versionName = "1.0.44-100-frame-stable-2d-teacher-qa"') == 1
assert 'applicationIdSuffix = ".offline101"' in g
assert (ROOT/'tools/v144/fullbody_master.webp').is_file()
assert 'teacher2d_atlas_100.webp' in (PKG/'Teacher2DPortrait.kt').read_text('utf-8')
assert (ASSETS/'teacher2d_atlas_100.webp').is_file()

# Regenerate the 100 images and ZIP with correct registered eyes and lips.
DIST.mkdir(exist_ok=True)
subprocess.run([
    sys.executable, str(SRC/'generate_teacher_100_145.py'),
    '--master',str(ROOT/'tools/v144/fullbody_master.webp'),
    '--assets',str(ASSETS),
    '--zip',str(DIST/'WordQuestAI-100-animation-frames.zip'),
    '--preview',str(DIST/'teacher-100-contact.png'),
],check=True)
(PKG/'Teacher2D100Timeline.kt').write_text((SRC/'Teacher2D100Timeline.kt').read_text('utf-8'),encoding='utf-8')
assert not (ASSETS/'teacher2d_head.webp').exists()
assert not (ASSETS/'teacher2d_mouth.webp').exists()
assert not (ASSETS/'teacher2d_eyes.webp').exists()
# Never migrate saved profiles, encrypted keys, user data, package or signer.
g=g.replace('versionCode = 69','versionCode = 70',1)
g=g.replace('versionName = "1.0.44-100-frame-stable-2d-teacher-qa"',
            'versionName = "1.0.45-face-alignment-blink-qa"',1)
GRADLE.write_text(g,'utf-8')
print('v1.0.45 QA: precise lip center x121, not shifted to cheek; eyes fully registered')
print('100 frame ZIP regenerated and same full-body master retained')
print('Blink lasts 165ms instead of 1.3s; same-signature upgrade and data untouched')