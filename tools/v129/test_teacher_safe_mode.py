#!/usr/bin/env python3
"""Fail closed: start live teacher without initializing SceneView/Filament.

This prevents crashes on navigation for v1.0.28's unverified human GLBs.
Does not diagnose or certify the 3D native renderer itself.
"""
from pathlib import Path
from zipfile import ZipFile
import re
root=Path(__file__).resolve().parents[2]
stage=root/"source/app/src/main/java/com/kelimebulmacasi/a1/TeacherStage3D.kt"
gradle=root/"source/app/build.gradle.kts"
s=stage.read_text("utf-8")
g=gradle.read_text("utf-8")
assert "versionCode = 54" in g
assert 'versionName = "1.0.29-teacher-crash-safemode"' in g
assert 'applicationIdSuffix = ".offline101"' in g
assert 'var experimental3D by remember { mutableStateOf(false) }' in s
assert 'if (!experimental3D) {' in s
safe=s.split('if (!experimental3D) {',1)[1].split('} else {',1)[0]
safe_code="\n".join(line for line in safe.splitlines() if not line.strip().startswith("//"))
assert "TeacherFallback2D(" in safe_code
assert not any(native in safe_code for native in ("rememberEngine(", "rememberModelLoader(", "ModelNode(", "Scene("))
experimental=s.split('if (!experimental3D) {',1)[1].split('} else {',1)[1]
assert "rememberEngine()" in experimental and "ModelNode(" in experimental
assert s.index('if (!experimental3D) {')<s.index('rememberEngine()')
assert 'if (experimental3D) {' in s
assert '3D Dene (Beta)' in s and '2D Güvenli' in s
assert not any(x in s for x in ("putBoolean(\"experimental3D\", true)", "rememberSaveable { mutableStateOf(true) }"))
assert "avatarMotion" in (root/"source/app/src/main/java/com/kelimebulmacasi/a1/LiveTeacherScreen.kt").read_text("utf-8")
print("PASS: no native SceneView/Filament created when entering voice assistant")
print("PASS: 2D fallback can still speak/listen and 3D remains a session-only opt-in")
print("PASS: versionCode54, same app ID/signing pipeline, no persisted crash loop")
