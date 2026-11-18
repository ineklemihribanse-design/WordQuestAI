#!/usr/bin/env python3
"""Apply v1.0.32 QA morph-isolation diagnostic AFTER v1.0.31 staging.

The difference between real full GLB and real body-only GLB is controlled:
same body, face at rest, rig, clothes and textures; only live ARKit morph
GPU buffers/face animation channels differ. Native crash reason still unknown.

No user data, Gemini keys, TTS, package ID, or release keys are touched.
This script must never automatically sign/publish the QA artifact.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
GRADLE=APP/"build.gradle.kts"
stage=PKG/"TeacherStage3D.kt"
probe=PKG/"Teacher3DProbeActivity.kt"
g=GRADLE.read_text("utf-8")
s=stage.read_text("utf-8")
p=probe.read_text("utf-8")
assert "versionCode = 56" in g
assert 'versionName = "1.0.31-human-avatar-stability-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g
assert "TeacherFallback2D(" in s and "rememberEngine(" not in s
assert "getHistoricalProcessExitReasons" in p and "REASON_CRASH_NATIVE" in p

for file in ("TeacherStage3D.kt","Teacher3DProbeActivity.kt"):
    target=PKG/file
    data=(ROOT/"tools/v132b"/file).read_text("utf-8")
    assert "Teacher3DHealth" in data
    target.write_text(data,encoding="utf-8")

g=g.replace("versionCode = 56","versionCode = 57",1)
g=g.replace('versionName = "1.0.31-human-avatar-stability-qa"',
            'versionName = "1.0.32-female-native-morph-diagnostics-qa"',1)
GRADLE.write_text(g,encoding="utf-8")
print("QA 1.0.32 code57: real female GLB full vs no-morph GPU diagnostic; no release")
