#!/usr/bin/env python3
"""Preserve installed 2.5D prototype after v1.0.46 vocabulary overlay.

The user already installed this separately signed beta. Keep that renderer while
upgrading in-place to v1.0.47 with both vocabulary modes and 2.5D. The visual
rig remains experimental; never claim this stage is visually certified.
Keeps package, signing chain, encrypted API keys, user data and archive.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
tests=app/"src/test/java/com/kelimebulmacasi/a1"
gradle=app/"build.gradle.kts"
g=gradle.read_text("utf-8")
assert g.count('versionCode = 71') == 1
assert g.count('versionName = "1.0.46-vocabulary-learning-modes"') == 1
assert 'applicationIdSuffix = ".offline101"' in g
assert (app/"src/main/assets/teacher2d/teacher2d_fullbody_master.webp").is_file()
for name in ("Teacher25DMath.kt","Teacher25DPortrait.kt"):
    (pkg/name).write_text((root/"tools/v147"/name).read_text("utf-8"),encoding="utf-8")
(tests/"Teacher25DMathTest.kt").write_text(
    (root/"tools/v147/Teacher25DMathTest.kt").read_text("utf-8"),encoding="utf-8")
(pkg/"Teacher2DPortrait.kt").write_text("""package com.kelimebulmacasi.a1
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Legacy Compose call-site stays stable; renders mesh-rigged 2.5D instead of a 100-frame atlas. */
@Composable internal fun Teacher2DPortrait(
    motion: AvatarMode,
    mouthCue: AvatarMouthFrame,
    modifier: Modifier = Modifier,
) = Teacher25DPortrait(motion = motion, mouthCue = mouthCue, modifier = modifier)
""",encoding="utf-8")
# v1.0.47 compatibility update for already installed v1.0.46 experimental APK.
g=g.replace("versionCode = 71","versionCode = 72",1)
g=g.replace('versionName = "1.0.46-vocabulary-learning-modes"',
            'versionName = "1.0.47-vocabulary-2p5d-compat"',1)
gradle.write_text(g,"utf-8")
print("2.5D prototype: one source portrait, real-time Android drawBitmapMesh, no frames")
print("2.5D remains experimental and requires visual device QA; vocabulary remains enabled")
